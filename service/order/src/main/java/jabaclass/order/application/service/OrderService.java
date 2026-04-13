package jabaclass.order.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jabaclass.order.common.error.BusinessException;
import jabaclass.order.application.port.external.DepositPort;
import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.application.exception.OrderErrorCode;
import jabaclass.order.application.port.internal.OrderUseCase;
import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.infrastructure.kafka.OrderEventPublisher;
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
@Transactional(readOnly = true)
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final DepositPort depositClient;
    private final ProductPort productClient;
    private final OrderEventPublisher eventPublisher;

    // -------------------------------------------------------------------------
    // 주문 생성
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto) {
        validateCreateRequest(requestDto);

        ProductReservationResponseDto reservation = reserveProduct(userId, requestDto);
        BigDecimal totalAmount = calculateTotalAmount(reservation.price(), requestDto.quantity());

        try {
            useDeposit(userId, requestDto.depositAmount(), totalAmount);
        } catch (RuntimeException e) {
            eventPublisher.publishReservationReleased(reservation.productUserId());
            throw e;
        }

        Order order = Order.create(
            requestDto.productScheduleId(),
            userId,
            reservation.productUserId(),
            requestDto.quantity(),
            totalAmount,
            requestDto.depositAmount()
        );

        return CreateOrderResponseDto.of(orderRepository.save(order), requestDto.productId());
    }

    // -------------------------------------------------------------------------
    // 조회
    // -------------------------------------------------------------------------

    @Override
    public OrderResponseDto getById(UUID orderId) {
        return OrderResponseDto.from(findOrderOrThrow(orderId));
    }

    @Override
    public List<OrderResponseDto> getOrders(UUID userId, OrderStatus status) {
        List<Order> orders = (status == null)
            ? orderRepository.findAllByUserId(userId)
            : orderRepository.findAllByUserIdAndStatus(userId, status);

        return orders.stream().map(OrderResponseDto::from).toList();
    }

    @Override
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

    // -------------------------------------------------------------------------
    // 주문 상태 변경
    // -------------------------------------------------------------------------

    @Override
    public boolean validatePaymentAmount(UUID orderId, BigDecimal amount) {
        return findOrderOrThrow(orderId).isPaymentAmountValid(amount);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(UUID orderId, UpdateOrderPaymentStatusRequestDto requestDto) {
        Order order = findOrderOrThrow(orderId);
        switch (requestDto.paymentStatus()) {
            case SUCCESS -> onPaymentSuccess(order);
            case FAILED -> onPaymentFailed(order);
        }
    }

    @Override
    @Transactional
    public void expireOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        if (order.getStatus() == OrderStatus.EXPIRED) {
            return;
        }
        order.expire();
        eventPublisher.publishReservationReleased(requireProductUserId(order));
        eventPublisher.publishOrderExpired(order);
    }

    @Override
    @Transactional
    public void refund(UUID orderId) {
        findOrderOrThrow(orderId).refund();
    }

    // -------------------------------------------------------------------------
    // 결제 결과 처리
    // -------------------------------------------------------------------------

    private void onPaymentSuccess(Order order) {
        order.pay();
        eventPublisher.publishReservationConfirmed(requireProductUserId(order));
    }

    private void onPaymentFailed(Order order) {
        order.failPayment();
        eventPublisher.publishReservationReleased(requireProductUserId(order));
        eventPublisher.publishDepositRefundRequested(order);
    }

    // -------------------------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------------------------

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

    private UUID requireProductUserId(Order order) {
        if (order.getProductUserId() == null) {
            throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_USER_ID_REQUIRED);
        }
        return order.getProductUserId();
    }
}
