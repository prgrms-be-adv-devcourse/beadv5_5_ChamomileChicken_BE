package jabaclass.order.order.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.order.common.error.BusinessException;
import jabaclass.order.order.application.client.DepositClient;
import jabaclass.order.order.application.client.ProductClient;
import jabaclass.order.order.domain.model.Order;
import jabaclass.order.order.domain.model.PaymentResultStatus;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.domain.repository.OrderRepository;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.order.infrastructure.client.product.dto.ProductReservationStatus;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DepositClient depositClient;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void 주문을_생성한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID productUserId = UUID.randomUUID();
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            2,
            new BigDecimal("3000")
        );
        given(depositClient.isValid(userId, requestDto.depositAmount())).willReturn(true);
        given(productClient.reserve(requestDto.productScheduleId(), userId, requestDto.quantity()))
            .willReturn(new ProductReservationResponseDto(new BigDecimal("10000"), requestDto.quantity(), productUserId, true));
        given(orderRepository.save(any(Order.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateOrderResponseDto actual = orderService.create(userId, requestDto);

        // then
        assertThat(actual.productId()).isEqualTo(requestDto.productId());
        assertThat(actual.productScheduleId()).isEqualTo(requestDto.productScheduleId());
        assertThat(actual.buyerId()).isEqualTo(userId);
        assertThat(actual.quantity()).isEqualTo(requestDto.quantity());
        assertThat(actual.totalAmount()).isEqualByComparingTo("20000");
        assertThat(actual.depositAmount()).isEqualByComparingTo(requestDto.depositAmount());
        assertThat(actual.paymentAmount()).isEqualByComparingTo("17000");
        assertThat(actual.status()).isEqualTo(OrderStatus.PENDING);
        then(productClient).should().reserve(requestDto.productScheduleId(), userId, requestDto.quantity());
        then(orderRepository).should().save(argThat(order -> productUserId.equals(order.getProductUserId())));
    }

    @Test
    void 수량이_0이면_주문_생성시_예외가_발생한다() {
        // given
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            0,
            new BigDecimal("1000")
        );

        // when & then
        assertThatThrownBy(() -> orderService.create(UUID.randomUUID(), requestDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("수량은 1 이상이어야 합니다.");
        then(orderRepository).should(never()).save(any(Order.class));
        then(productClient).shouldHaveNoInteractions();
    }

    @Test
    void 예치금이_부족하면_주문_생성시_예외가_발생한다() {
        // given
        UUID userId = UUID.randomUUID();
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new BigDecimal("10000")
        );
        given(depositClient.isValid(userId, requestDto.depositAmount())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> orderService.create(userId, requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("사용 가능한 예치금이 부족합니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    void 예약_가능한_상품_일정이_아니면_주문_생성시_예외가_발생한다() {
        // given
        UUID userId = UUID.randomUUID();
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new BigDecimal("1000")
        );
        given(depositClient.isValid(userId, requestDto.depositAmount())).willReturn(true);
        given(productClient.reserve(requestDto.productScheduleId(), userId, requestDto.quantity()))
            .willReturn(new ProductReservationResponseDto(new BigDecimal("10000"), requestDto.quantity(), null, false));

        // when & then
        assertThatThrownBy(() -> orderService.create(userId, requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("예약 가능한 상품 일정이 아닙니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    void 주문을_조회한다() {
        // given
        Order order = Order.create(
            UUID.randomUUID(),
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
        assertThat(actual.buyerId()).isEqualTo(order.getUserId());
        assertThat(actual.quantity()).isEqualTo(order.getQuantity());
        assertThat(actual.totalAmount()).isEqualByComparingTo(order.getPrice());
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
            UUID.randomUUID(),
            1,
            new BigDecimal("5000")
        );
        Order secondOrder = Order.create(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            2,
            new BigDecimal("10000")
        );
        given(orderRepository.findAllByUserId(userId)).willReturn(List.of(firstOrder, secondOrder));

        // when
        List<OrderResponseDto> actual = orderService.getOrders(userId, null);

        // then
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).buyerId()).isEqualTo(userId);
        assertThat(actual.get(1).buyerId()).isEqualTo(userId);
    }

    @Test
    void 상태로_필터링하여_주문_목록을_조회한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
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
            UUID.randomUUID(),
            1,
            new BigDecimal("15000")
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        OrderResponseDto actual = orderService.cancel(order.getId(), userId);

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
            UUID.randomUUID(),
            1,
            new BigDecimal("15000")
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancel(order.getId(), UUID.randomUUID()))
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
            UUID.randomUUID(),
            1,
            new BigDecimal("15000")
        );
        order.cancel();
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancel(order.getId(), userId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("현재 주문 상태에서는 취소할 수 없습니다.");
        then(orderRepository).should(never()).save(any(Order.class));
    }

    @Test
    void 결제_금액이_일치하면_true를_반환한다() {
        // given
        Order order = Order.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            1,
            new BigDecimal("15000")
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        boolean actual = orderService.validatePaymentAmount(order.getId(), new BigDecimal("15000"));

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 결제_성공을_반영한다() {
        // given
        UUID productUserId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            productUserId,
            1,
            new BigDecimal("15000")
        );
        UpdateOrderPaymentStatusRequestDto requestDto = new UpdateOrderPaymentStatusRequestDto(
            UUID.randomUUID(),
            PaymentResultStatus.SUCCESS,
            new BigDecimal("5000")
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        orderService.updatePaymentStatus(order.getId(), requestDto);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        then(depositClient).should().use(order.getUserId(), new BigDecimal("5000"));
        then(productClient).should().updateReservation(
            order.getProductScheduleId(),
            order.getUserId(),
            ProductReservationStatus.SUCCESS,
            productUserId,
            order.getQuantity()
        );
    }

    @Test
    void 결제_실패를_반영한다() {
        // given
        UUID productUserId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            productUserId,
            2,
            new BigDecimal("15000")
        );
        UpdateOrderPaymentStatusRequestDto requestDto = new UpdateOrderPaymentStatusRequestDto(
            UUID.randomUUID(),
            PaymentResultStatus.FAILED,
            BigDecimal.ZERO
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        orderService.updatePaymentStatus(order.getId(), requestDto);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        then(productClient).should().updateReservation(
            order.getProductScheduleId(),
            order.getUserId(),
            ProductReservationStatus.FAILED,
            productUserId,
            order.getQuantity()
        );
    }

    @Test
    void 결제_취소를_반영한다() {
        // given
        UUID productUserId = UUID.randomUUID();
        Order order = Order.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            productUserId,
            2,
            new BigDecimal("15000")
        );
        UpdateOrderPaymentStatusRequestDto requestDto = new UpdateOrderPaymentStatusRequestDto(
            UUID.randomUUID(),
            PaymentResultStatus.CANCELLED,
            BigDecimal.ZERO
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        orderService.updatePaymentStatus(order.getId(), requestDto);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        then(productClient).should().updateReservation(
            order.getProductScheduleId(),
            order.getUserId(),
            ProductReservationStatus.CANCEL,
            productUserId,
            order.getQuantity()
        );
    }
}
