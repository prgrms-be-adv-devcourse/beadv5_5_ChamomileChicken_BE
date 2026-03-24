package jabaclass.order.application.service;

import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

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
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("주문을 찾을 수 없습니다.");
    }
}
