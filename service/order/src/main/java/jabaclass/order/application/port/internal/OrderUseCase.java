package jabaclass.order.application.port.internal;


import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.request.OrderBulkReadRequestDto;
import jabaclass.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.presentation.dto.response.OrderSettlementItemResponseDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;

public interface OrderUseCase {

    CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto);

    OrderResponseDto getById(UUID orderId);

    List<OrderResponseDto> getOrders(UUID userId, OrderStatus status);

    boolean validatePaymentAmount(UUID orderId, BigDecimal amount);

    void updatePaymentStatus(UUID orderId, UpdateOrderPaymentStatusRequestDto requestDto);

    void expireOrder(UUID orderId);

    void refund(UUID orderId);

    List<OrderSettlementItemResponseDto> getOrdersByIds(OrderBulkReadRequestDto requestDto);
}
