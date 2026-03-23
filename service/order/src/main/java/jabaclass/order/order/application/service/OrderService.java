package jabaclass.order.order.application.service;

import java.util.List;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.order.application.exception.OrderErrorCode;
import jabaclass.order.order.application.usecase.OrderUseCase;
import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.domain.repository.OrderRepository;
import jabaclass.order.order.presentation.dto.request.CancelOrderRequestDto;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;
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
    public OrderResponseDto cancel(UUID orderId, CancelOrderRequestDto requestDto) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));

        validateOrderOwner(order, requestDto.userId());
        validateCancelable(order);
        order.cancel();

        return OrderResponseDto.from(orderRepository.save(order));
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
}
