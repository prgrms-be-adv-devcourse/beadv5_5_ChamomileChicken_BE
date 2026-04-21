package jabaclass.order.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.application.port.external.DepositPort;
import jabaclass.order.application.port.external.PaymentPort;
import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.application.service.handler.OrderExpireHandler;
import jabaclass.order.application.service.handler.OrderPaymentResultHandler;
import jabaclass.order.application.service.handler.OrderRefundHandler;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.model.RefundPolicy;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.infrastructure.client.payment.dto.InternalRefundResponseDto;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentCompletedEvent;
import jabaclass.order.infrastructure.kafka.product.dto.OrderReservationReleasedEvent;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxEvent;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.request.OrderBulkReadRequestDto;
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
	private final PaymentPort paymentClient;
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;
	private final OrderPaymentResultHandler orderPaymentResultHandler;
	private final OrderExpireHandler orderExpireHandler;
	private final OrderRefundHandler orderRefundHandler;

	// 주문 생성 (외부 호출 포함 — tx 없음)
	@Override
	public CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto) {
		validateCreateRequest(requestDto);

		//재고 차감
		ProductReservationResponseDto reservation = reserveProduct(userId, requestDto);
		BigDecimal totalAmount = calculateTotalAmount(reservation.price(), requestDto.quantity());

		try {
			//예치금 차감
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
			reservation.sellerId(),
			userId,
			reservation.productUserId(),
			requestDto.quantity(),
			totalAmount
		);
		return CreateOrderResponseDto.of(orderRepository.save(order), requestDto.productId(), requestDto.depositAmount());
	}

	// [오케스트레이터] @Transactional 없음 — 외부 HTTP 호출(Product, Payment) 포함
	// Payment 서비스 호출이 동기이므로 PG 환불 실패 시 예외가 여기로 전파 → Order PAID 유지 (보상 불필요)
	@Override
	public void refund(UUID userId, UUID orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

		if (!order.isOwnedBy(userId)) {
			throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
		}
		// PAID 상태 가드 — Eventual Consistency로 인해 결제 직후 이 시점에 아직 PENDING일 수 있음
		if (order.getStatus() != OrderStatus.PAID) {
			throw new BusinessException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
		}

		// Product 서비스에서 스케줄 시작일 조회 → 환불 비율 계산
		LocalDate startDate = productClient.getScheduleStartDate(order.getProductScheduleId());
		long days = ChronoUnit.DAYS.between(LocalDate.now(), startDate);
		BigDecimal refundRate = RefundPolicy.rateOf(days);
		if (refundRate.signum() == 0) {
			throw new BusinessException(OrderErrorCode.ORDER_REFUND_POLICY_EXPIRED);
		}

		// Payment 서비스로 환불 요청 (PG 호출 포함) — 성공 시 예치금 환불 금액 반환
		// orderId를 PG 멱등키로 사용하므로 타임아웃 후 재시도해도 이중 환불 없음
		InternalRefundResponseDto refundResponse = paymentClient.refund(orderId, refundRate);

		// Payment 커밋 완료 후 → Order REFUNDED + Outbox 저장 (독립 트랜잭션)
		orderRefundHandler.onSuccess(orderId, refundResponse);
	}

	// 조회
	@Override
	@Transactional(readOnly = true)
	public OrderResponseDto getById(UUID userId, UUID orderId) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
		if (!order.isOwnedBy(userId)) {
			throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
		}
		return OrderResponseDto.from(order);
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
		return orderRepository.findById(orderId)
			.orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND))
			.isPaymentAmountValid(amount);
	}

	@Override
	public void completePayment(PaymentCompletedEvent event) {
		orderPaymentResultHandler.onSuccess(event);
	}

	@Override
	public void failPayment(UUID eventId, UUID orderId, BigDecimal depositAmount) {
		orderPaymentResultHandler.onFailed(eventId, orderId, depositAmount);
	}

	@Override
	public void expireOrder(UUID eventId, UUID orderId, BigDecimal depositAmount) {
		orderExpireHandler.expire(eventId, orderId, depositAmount);
	}

	private ProductReservationResponseDto reserveProduct(UUID userId, CreateOrderRequestDto requestDto) {
		ProductReservationResponseDto response = productClient.reserve(
			requestDto.productScheduleId(),
			userId,
			requestDto.quantity(),
			requestDto.productPrice()
		);
		if (response == null || !response.isOk() || response.productUserId() == null || response.sellerId() == null) {
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
