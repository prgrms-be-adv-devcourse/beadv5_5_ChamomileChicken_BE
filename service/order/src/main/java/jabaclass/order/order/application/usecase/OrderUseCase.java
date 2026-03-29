package jabaclass.order.order.application.usecase;


import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;

public interface OrderUseCase {

    CreateOrderResponseDto create(UUID userId, CreateOrderRequestDto requestDto);

    OrderResponseDto getById(UUID orderId);

    List<OrderResponseDto> getOrders(UUID userId, OrderStatus status);

    OrderResponseDto cancel(UUID orderId, UUID userId);

    boolean validatePaymentAmount(UUID orderId, BigDecimal amount);

    void updatePaymentStatus(UUID orderId, UpdateOrderPaymentStatusRequestDto requestDto);
}
