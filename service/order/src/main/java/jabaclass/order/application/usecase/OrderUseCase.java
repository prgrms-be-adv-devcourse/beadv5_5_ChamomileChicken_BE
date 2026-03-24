package jabaclass.order.application.usecase;


import java.util.UUID;

import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;

public interface OrderUseCase {

    OrderResponseDto create(CreateOrderRequestDto requestDto);

    OrderResponseDto getById(UUID orderId);
}
