package jabaclass.order.order.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.order.common.error.BusinessException;
import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.domain.repository.OrderRepository;
import jabaclass.order.order.presentation.dto.request.CancelOrderRequestDto;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void 주문을_생성한다() {
        // given
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            2,
            new BigDecimal("10000")
        );
        given(orderRepository.save(any(Order.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        OrderResponseDto actual = orderService.create(requestDto);

        // then
        assertThat(actual.productScheduleId()).isEqualTo(requestDto.productScheduleId());
        assertThat(actual.userId()).isEqualTo(requestDto.userId());
        assertThat(actual.quantity()).isEqualTo(requestDto.quantity());
        assertThat(actual.price()).isEqualByComparingTo(requestDto.price());
        assertThat(actual.status()).isEqualTo(OrderStatus.PENDING);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    void 수량이_0이면_주문_생성시_예외가_발생한다() {
        // given
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            0,
            new BigDecimal("10000")
        );

        // when & then
        assertThatThrownBy(() -> orderService.create(requestDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("수량은 1 이상이어야 합니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    void 주문_가격이_음수이면_주문_생성시_예외가_발생한다() {
        // given
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new BigDecimal("-1")
        );

        // when & then
        assertThatThrownBy(() -> orderService.create(requestDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("주문 가격은 0 이상이어야 합니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    void 주문을_조회한다() {
        // given
        Order order = Order.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new BigDecimal("5000")
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        OrderResponseDto actual = orderService.getById(order.getId());

        // then
        assertThat(actual.id()).isEqualTo(order.getId());
        assertThat(actual.productScheduleId()).isEqualTo(order.getProductScheduleId());
        assertThat(actual.userId()).isEqualTo(order.getUserId());
        assertThat(actual.quantity()).isEqualTo(order.getQuantity());
        assertThat(actual.price()).isEqualByComparingTo(order.getPrice());
        assertThat(actual.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void 없는_주문을_조회하면_예외가_발생한다() {
        // given
        UUID orderId = UUID.randomUUID();
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getById(orderId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("주문을 찾을 수 없습니다.");
    }

    @Test
    void 주문_목록을_조회한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order firstOrder = Order.create(
            UUID.randomUUID(),
            userId,
            1,
            new BigDecimal("5000")
        );
        Order secondOrder = Order.create(
            UUID.randomUUID(),
            userId,
            2,
            new BigDecimal("10000")
        );
        given(orderRepository.findAllByUserId(userId)).willReturn(List.of(firstOrder, secondOrder));

        // when
        List<OrderResponseDto> actual = orderService.getOrders(userId, null);

        // then
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).userId()).isEqualTo(userId);
        assertThat(actual.get(1).userId()).isEqualTo(userId);
    }

    @Test
    void 상태로_필터링하여_주문_목록을_조회한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            userId,
            1,
            new BigDecimal("7000")
        );
        given(orderRepository.findAllByUserIdAndStatus(userId, OrderStatus.PENDING))
            .willReturn(List.of(order));

        // when
        List<OrderResponseDto> actual = orderService.getOrders(userId, OrderStatus.PENDING);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void 주문을_취소한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            userId,
            1,
            new BigDecimal("15000")
        );
        CancelOrderRequestDto requestDto = new CancelOrderRequestDto(userId);
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        OrderResponseDto actual = orderService.cancel(order.getId(), requestDto);

        // then
        assertThat(actual.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 다른_사용자의_주문을_취소하면_예외가_발생한다() {
        // given
        Order order = Order.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new BigDecimal("15000")
        );
        CancelOrderRequestDto requestDto = new CancelOrderRequestDto(UUID.randomUUID());
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancel(order.getId(), requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("본인의 주문만 조회 및 취소할 수 있습니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    void 이미_취소된_주문을_취소하면_예외가_발생한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            userId,
            1,
            new BigDecimal("15000")
        );
        order.cancel();
        CancelOrderRequestDto requestDto = new CancelOrderRequestDto(userId);
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancel(order.getId(), requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("현재 주문 상태에서는 취소할 수 없습니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }
}
