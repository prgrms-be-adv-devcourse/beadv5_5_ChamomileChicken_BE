package jabaclass.order.application.service;

import jabaclass.order.application.usecase.OrderUseCase;
import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponseDto create(CreateOrderRequestDto requestDto) {
        Order order = Order.create(
            requestDto.productScheduleId(),
            requestDto.userId(),
            requestDto.quantity(),
            requestDto.price()
        );
        Order savedOrder = orderRepository.save(order);

        return OrderResponseDto.from(savedOrder);
    }

    @Override
    public OrderResponseDto getById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        return OrderResponseDto.from(order);
    }
}
