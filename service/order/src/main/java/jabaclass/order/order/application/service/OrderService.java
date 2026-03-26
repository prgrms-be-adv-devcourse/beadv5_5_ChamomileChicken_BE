package jabaclass.order.order.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jabaclass.order.common.error.BusinessException;
import jabaclass.order.order.application.client.DepositClient;
import jabaclass.order.order.application.client.ProductClient;
import jabaclass.order.order.application.exception.OrderErrorCode;
import jabaclass.order.order.application.usecase.OrderUseCase;
import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.PaymentResultStatus;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.domain.repository.OrderRepository;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final DepositClient depositClient;
    private final ProductClient productClient;

    @Override
    @Transactional
    public CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto) {
        validateCreateRequest(requestDto);
        validateDeposit(userId, requestDto.depositAmount());
        ProductReservationResponseDto reservation = validateAndReserveProduct(requestDto);
        BigDecimal totalAmount = calculateTotalAmount(reservation.price(), requestDto.quantity());
        validateDepositAmount(requestDto.depositAmount(), totalAmount);

        Order order = Order.create(
            requestDto.productScheduleId(),
            userId,
            requestDto.quantity(),
            totalAmount
        );
        Order savedOrder = orderRepository.save(order);

        return CreateOrderResponseDto.of(savedOrder, requestDto.productId(), requestDto.depositAmount());
    }

    @Override
    public OrderResponseDto getById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        return OrderResponseDto.from(order);
    }

    @Override
    public List<OrderResponseDto> getOrders(UUID userId, OrderStatus status) {
        List<Order> orders = getOrdersByCondition(userId, status);

        return orders.stream()
            .map(OrderResponseDto::from)
            .toList();
    }

    @Override
    @Transactional
    public OrderResponseDto cancel(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, userId);
        validateCancelable(order);
        order.cancel();
        return OrderResponseDto.from(order);
    }

    @Override
    public boolean validatePaymentAmount(UUID orderId, BigDecimal amount) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        return order.isPaymentAmountValid(amount);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(UUID orderId, UpdateOrderPaymentStatusRequestDto requestDto) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        processPaymentResult(order, requestDto);
    }

    private List<Order> getOrdersByCondition(UUID userId, OrderStatus status) {
        if (status == null) {
            return orderRepository.findAllByUserId(userId);
        }

        return orderRepository.findAllByUserIdAndStatus(userId, status);
    }

    private void validateOrderOwner(Order order, UUID userId) {
        if (!order.isOwnedBy(userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    private void validateCancelable(Order order) {
        if (!order.canCancel()) {
            throw new BusinessException(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }
    }

    private void validateDeposit(UUID userId, BigDecimal depositAmount) {
        if (depositAmount.signum() == 0) {
            return;
        }

        if (!depositClient.isValid(userId, depositAmount)) {
            throw new BusinessException(OrderErrorCode.ORDER_DEPOSIT_NOT_AVAILABLE);
        }
    }

    private ProductReservationResponseDto validateAndReserveProduct(CreateOrderRequestDto requestDto) {
        ProductReservationResponseDto response = productClient.reserve(
            requestDto.productScheduleId(),
            requestDto.quantity()
        );

        if (response == null || !response.valid()) {
            throw new BusinessException(OrderErrorCode.ORDER_PRODUCT_NOT_AVAILABLE);
        }

        return response;
    }

    private void processPaymentResult(Order order, UpdateOrderPaymentStatusRequestDto requestDto) {
        if (requestDto.paymentStatus() == PaymentResultStatus.SUCCESS) {
            processPaymentSuccess(order, requestDto.depositAmount());
            return;
        }

        if (requestDto.paymentStatus() == PaymentResultStatus.FAILED) {
            processPaymentFailure(order);
            return;
        }

        processPaymentCancel(order);
    }

    private void processPaymentSuccess(Order order, BigDecimal depositAmount) {
        if (depositAmount.signum() > 0) {
            depositClient.use(order.getUserId(), depositAmount);
        }

        order.pay();
    }

    private void processPaymentFailure(Order order) {
        productClient.release(order.getProductScheduleId(), order.getQuantity());
        order.failPayment();
    }

    private void processPaymentCancel(Order order) {
        productClient.release(order.getProductScheduleId(), order.getQuantity());
        order.cancel();
    }

    private BigDecimal calculateTotalAmount(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private void validateCreateRequest(CreateOrderRequestDto requestDto) {
        if (requestDto.quantity() == null || requestDto.quantity() <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        if (requestDto.depositAmount() == null || requestDto.depositAmount().signum() < 0) {
            throw new IllegalArgumentException("예치금 사용 금액은 0 이상이어야 합니다.");
        }

    }

    private void validateDepositAmount(BigDecimal depositAmount, BigDecimal totalAmount) {
        if (depositAmount.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("예치금 사용 금액은 주문 가격보다 클 수 없습니다.");
        }
    }
}
