package jabaclass.order.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.application.port.external.DepositPort;
import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.application.service.handler.OrderExpireHandler;
import jabaclass.order.application.service.handler.OrderPaymentResultHandler;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxEvent;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.request.OrderBulkReadRequestDto;
import jabaclass.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;
import jabaclass.order.presentation.dto.response.OrderSettlementItemResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

	private final OrderRepository orderRepository;
	private final DepositPort depositClient;
	private final ProductPort productClient;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final OrderPaymentResultHandler orderPaymentResultHandler;
	private final OrderExpireHandler orderExpireHandler;

	// 주문 생성 (외부 호출 포함 — tx 없음)
	@Override
	public CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto) {
		validateCreateRequest(requestDto);

		ProductReservationResponseDto reservation = reserveProduct(userId, requestDto);
		BigDecimal totalAmount = calculateTotalAmount(reservation.price(), requestDto.quantity());

		try {
			useDeposit(userId, requestDto.depositAmount(), totalAmount);
		} catch (RuntimeException e) {
			// 예치금 차감 실패 → 재고 예약 해제 (단순 outbox 저장, save() 자체 @Transactional로 충분)
			outboxRepository.save(OutboxEvent.create(
				"ORDER",
				reservation.productUserId().toString(),
				EventType.ORDER_RESERVATION_RELEASED,
				toJson(new OrderReservationReleasedEvent(UUID.randomUUID(), null, reservation.productUserId()))
			));
			throw e;
		}

		Order order = Order.create(
			requestDto.productScheduleId(),
			userId,
			reservation.productUserId(),
			requestDto.quantity(),
			totalAmount
		);
		return CreateOrderResponseDto.of(orderRepository.save(order), requestDto.productId(), requestDto.depositAmount());
	}

	// 조회
	@Override
	@Transactional(readOnly = true)
	public OrderResponseDto getById(UUID orderId) {
		return OrderResponseDto.from(findOrderOrThrow(orderId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderResponseDto> getOrders(UUID userId, OrderStatus status) {
		List<Order> orders = (status == null)
			? orderRepository.findAllByUserId(userId)
			: orderRepository.findAllByUserIdAndStatus(userId, status);
		return orders.stream().map(OrderResponseDto::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderSettlementItemResponseDto> getOrdersByIds(OrderBulkReadRequestDto requestDto) {
		if (requestDto == null || requestDto.orderIds() == null || requestDto.orderIds().isEmpty()) {
			return List.of();
		}
		List<UUID> distinctIds = requestDto.orderIds().stream().distinct().toList();
		Map<UUID, Order> orderMap = orderRepository.findAllByIds(distinctIds).stream()
			.collect(Collectors.toMap(Order::getId, Function.identity()));
		return distinctIds.stream()
			.map(orderMap::get)
			.filter(Objects::nonNull)
			.map(OrderSettlementItemResponseDto::from)
			.toList();
	}

	// 주문 상태 변경
	@Override
	@Transactional(readOnly = true)
	public boolean validatePaymentAmount(UUID orderId, BigDecimal amount) {
		return findOrderOrThrow(orderId).isPaymentAmountValid(amount);
	}

	@Override
	public void updatePaymentStatus(UUID eventId, UUID orderId, UpdateOrderPaymentStatusRequestDto requestDto) {
		switch (requestDto.paymentStatus()) {
			case SUCCESS -> orderPaymentResultHandler.onSuccess(eventId, orderId);
			case FAILED -> orderPaymentResultHandler.onFailed(eventId, orderId, requestDto.depositAmount());
		}
	}

	@Override
	public void expireOrder(UUID eventId, UUID orderId, BigDecimal depositAmount) {
		orderExpireHandler.expire(eventId, orderId, depositAmount);
	}

	@Override
	@Transactional
	public void refund(UUID orderId) {
		findOrderOrThrow(orderId).refund();
	}

	// 내부 헬퍼
	private Order findOrderOrThrow(UUID orderId) {
		return orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
	}

	private ProductReservationResponseDto reserveProduct(UUID userId, CreateOrderRequestDto requestDto) {
		ProductReservationResponseDto response = productClient.reserve(
			requestDto.productScheduleId(),
			userId,
			requestDto.quantity(),
			requestDto.productPrice()
		);
		if (response == null || !response.isOk() || response.productUserId() == null) {
			throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_NOT_AVAILABLE);
		}
		return response;
	}

	private void useDeposit(UUID userId, BigDecimal depositAmount, BigDecimal totalAmount) {
		if (depositAmount.compareTo(totalAmount) > 0) {
			throw new BusinessException(OrderErrorCode.ORDER_DEPOSIT_AMOUNT_EXCEEDS_PRICE);
		}
		if (depositAmount.signum() == 0) {
			return;
		}
		if (!depositClient.validateAndUse(userId, depositAmount)) {
			throw new BusinessException(OrderErrorCode.ORDER_DEPOSIT_NOT_AVAILABLE);
		}
	}

	private void validateCreateRequest(CreateOrderRequestDto requestDto) {
		if (requestDto.quantity() == null || requestDto.quantity() <= 0) {
			throw new BusinessException(OrderErrorCode.ORDER_QUANTITY_INVALID);
		}
		if (requestDto.depositAmount() == null || requestDto.depositAmount().signum() < 0) {
			throw new BusinessException(OrderErrorCode.ORDER_DEPOSIT_AMOUNT_INVALID);
		}
		if (requestDto.productPrice() == null || requestDto.productPrice().signum() < 0) {
			throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_PRICE_INVALID);
		}
	}

	private BigDecimal calculateTotalAmount(BigDecimal unitPrice, Integer quantity) {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	private String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 직렬화 실패", e);
		}
	}
}
