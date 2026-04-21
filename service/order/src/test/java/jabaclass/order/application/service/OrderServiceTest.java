package jabaclass.order.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.application.port.external.DepositPort;
import jabaclass.order.application.port.external.PaymentPort;
import jabaclass.order.application.port.external.ProductPort;
import jabaclass.order.application.service.handler.OrderExpireHandler;
import jabaclass.order.application.service.handler.OrderPaymentResultHandler;
import jabaclass.order.application.service.handler.OrderRefundHandler;
import jabaclass.order.common.error.BusinessException;
import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.model.PaymentResultStatus;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.infrastructure.client.payment.dto.InternalRefundResponseDto;
import jabaclass.order.infrastructure.client.product.dto.ProductReservationResponseDto;
import jabaclass.order.infrastructure.outbox.EventType;
import jabaclass.order.infrastructure.outbox.OutboxRepository;
import jabaclass.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.presentation.dto.response.OrderResponseDto;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class OrderServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DepositPort depositPort;

    @Mock
    private ProductPort productPort;

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private PaymentPort paymentPort;

    @Mock
    private OrderPaymentResultHandler orderPaymentResultHandler;

    @Mock
    private OrderExpireHandler orderExpireHandler;

    @Mock
    private OrderRefundHandler orderRefundHandler;

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
            new BigDecimal("10000"),
            new BigDecimal("3000")
        );
        given(depositPort.validateAndUse(userId, requestDto.depositAmount())).willReturn(true);
        given(productPort.reserve(requestDto.productScheduleId(), userId, requestDto.quantity(), requestDto.productPrice()))
            .willReturn(new ProductReservationResponseDto(new BigDecimal("10000"), 2, "OK", productUserId, SELLER_ID));
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
        then(productPort).should().reserve(requestDto.productScheduleId(), userId, requestDto.quantity(), requestDto.productPrice());
        then(depositPort).should().validateAndUse(userId, requestDto.depositAmount());
        then(orderRepository).should().save(argThat(order ->
            productUserId.equals(order.getProductUserId()) && SELLER_ID.equals(order.getSellerId())
        ));
    }

    @Test
    void 수량이_0이면_주문_생성시_예외가_발생한다() {
        // given
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), 0, new BigDecimal("10000"), new BigDecimal("1000")
        );

        // when & then
        assertThatThrownBy(() -> orderService.create(UUID.randomUUID(), requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("수량은 1 이상이어야 합니다.");
        then(orderRepository).should(never()).save(any(Order.class));
        then(productPort).shouldHaveNoInteractions();
    }

    @Test
    void 예치금이_부족하면_주문_생성시_예외가_발생한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID productUserId = UUID.randomUUID();
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("10000"), new BigDecimal("10000")
        );
        given(productPort.reserve(requestDto.productScheduleId(), userId, requestDto.quantity(), requestDto.productPrice()))
            .willReturn(new ProductReservationResponseDto(new BigDecimal("10000"), 1, "OK", productUserId, SELLER_ID));
        given(depositPort.validateAndUse(userId, requestDto.depositAmount())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> orderService.create(userId, requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("사용 가능한 예치금이 부족합니다.");
        then(orderRepository).should(never()).save(any(Order.class));
        then(outboxRepository).should().save(argThat(e ->
            e.getEventType() == EventType.ORDER_RESERVATION_RELEASED
        ));
    }

    @Test
    void 예약_가능한_상품_일정이_아니면_주문_생성시_예외가_발생한다() {
        // given
        UUID userId = UUID.randomUUID();
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto(
            UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("10000"), new BigDecimal("1000")
        );
        given(productPort.reserve(requestDto.productScheduleId(), userId, requestDto.quantity(), requestDto.productPrice()))
            .willReturn(new ProductReservationResponseDto(new BigDecimal("10000"), 0, "OUT_OF_STOCK", null, null));

        // when & then
        assertThatThrownBy(() -> orderService.create(userId, requestDto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("예약 가능한 상품 일정이 아닙니다.");
        then(orderRepository).should(never()).save(any(Order.class));
        then(depositPort).shouldHaveNoInteractions();
    }

    @Test
    void 주문을_조회한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order order = savedOrder(UUID.randomUUID(), userId, UUID.randomUUID(), 1, new BigDecimal("5000"));
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        OrderResponseDto actual = orderService.getById(userId, order.getId());

        // then
        assertThat(actual.id()).isEqualTo(order.getId());
        assertThat(actual.productScheduleId()).isEqualTo(order.getProductScheduleId());
        assertThat(actual.sellerId()).isEqualTo(order.getSellerId());
        assertThat(actual.buyerId()).isEqualTo(order.getUserId());
        assertThat(actual.quantity()).isEqualTo(order.getQuantity());
        assertThat(actual.totalAmount()).isEqualByComparingTo(order.getPrice());
        assertThat(actual.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void 없는_주문을_조회하면_예외가_발생한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getById(userId, orderId))
            .isInstanceOf(BusinessException.class)
            .hasMessage("주문을 찾을 수 없습니다.");
    }

    @Test
    void 주문_목록을_조회한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order firstOrder = savedOrder(UUID.randomUUID(), userId, UUID.randomUUID(), 1, new BigDecimal("5000"));
        Order secondOrder = savedOrder(UUID.randomUUID(), userId, UUID.randomUUID(), 2, new BigDecimal("10000"));
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
        Order order = savedOrder(UUID.randomUUID(), userId, UUID.randomUUID(), 1, new BigDecimal("7000"));
        given(orderRepository.findAllByUserIdAndStatus(userId, OrderStatus.PENDING)).willReturn(List.of(order));

        // when
        List<OrderResponseDto> actual = orderService.getOrders(userId, OrderStatus.PENDING);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void 결제_금액이_일치하면_true를_반환한다() {
        // given
        Order order = savedOrder(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("15000"));
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));

        // when
        boolean actual = orderService.validatePaymentAmount(order.getId(), new BigDecimal("15000"));

        // then
        assertThat(actual).isTrue();
    }

    @Test
    void 환불을_반영한다() {
        // given
        UUID userId = UUID.randomUUID();
        Order order = savedOrder(UUID.randomUUID(), userId, UUID.randomUUID(), 2, new BigDecimal("15000"));
        order.pay();
        InternalRefundResponseDto refundResponse = new InternalRefundResponseDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("15000"),
            new BigDecimal("15000"),
            LocalDateTime.of(2026, 4, 20, 18, 0)
        );
        given(orderRepository.findById(order.getId())).willReturn(Optional.of(order));
        given(productPort.getScheduleStartDate(order.getProductScheduleId()))
            .willReturn(LocalDate.now().plusDays(8));
        given(paymentPort.refund(eq(order.getId()), any(BigDecimal.class)))
            .willReturn(refundResponse);

        // when
        orderService.refund(userId, order.getId());

        // then — 환불 핸들러에 위임됨
        then(orderRefundHandler).should().onSuccess(order.getId(), refundResponse);
    }

    private Order savedOrder(UUID productScheduleId, UUID userId, UUID productUserId, int quantity, BigDecimal price) {
        Order order = Order.create(productScheduleId, SELLER_ID, userId, productUserId, quantity, price);
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        return order;
    }
}
