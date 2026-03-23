package jabaclass.order.order.application.usecase;


import java.util.List;
import java.util.UUID;

import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.presentation.dto.request.CancelOrderRequestDto;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;

public interface OrderUseCase {

    OrderResponseDto create(CreateOrderRequestDto requestDto);

    OrderResponseDto getById(UUID orderId);

    List<OrderResponseDto> getOrders(UUID userId, OrderStatus status);

    OrderResponseDto cancel(UUID orderId, CancelOrderRequestDto requestDto);
}
