package jabaclass.order.application.port.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.infrastructure.kafka.payment.dto.PaymentCompletedEvent;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.request.OrderBulkReadRequestDto;
import jabaclass.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.presentation.dto.response.OrderSettlementItemResponseDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;

public interface OrderUseCase {

    CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto);

    OrderResponseDto getById(UUID userId, UUID orderId);

    List<OrderResponseDto> getOrders(UUID userId, OrderStatus status);

    boolean validatePaymentAmount(UUID orderId, BigDecimal amount);

    void completePayment(PaymentCompletedEvent event);

    void failPayment(UUID eventId, UUID orderId, BigDecimal depositAmount);

    void expireOrder(UUID eventId, UUID orderId, BigDecimal depositAmount);

    void refund(UUID userId, UUID orderId);

    List<OrderSettlementItemResponseDto> getOrdersByIds(OrderBulkReadRequestDto requestDto);
}
