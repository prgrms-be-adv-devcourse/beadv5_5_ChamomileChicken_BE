package jabaclass.order.kafka;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.order.domain.model.Order;
import jabaclass.order.domain.model.OrderStatus;
import jabaclass.order.domain.repository.OrderRepository;
import jabaclass.order.infrastructure.client.deposit.DepositAdapter;
import jabaclass.order.infrastructure.client.payment.PaymentAdapter;
import jabaclass.order.infrastructure.client.product.ProductAdapter;
import jabaclass.order.infrastructure.idempotency.ProcessedEventRepository;
import jabaclass.order.infrastructure.outbox.OutboxRepository;

// payment.events 토픽을 order-service가 소비하는 흐름을 EmbeddedKafka로 검증
// - 실제 Kafka Consumer 스레드 + H2 DB + Outbox까지 포함한 통합 테스트
// - 외부 HTTP 어댑터(Product/Payment/Deposit)는 MockitoBean으로 격리
// - DirtiesContext: 테스트마다 Kafka offset/컨텍스트 초기화
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = {"payment.events", "payment.events.dlq"})
@DirtiesContext
@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
class PaymentEventsConsumerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    // processed_events 테이블 — 이벤트 중복 처리 방지용 멱등성 저장소
    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 외부 HTTP 어댑터 Mock — Kafka Consumer 흐름만 검증, HTTP 호출 차단
    @MockitoBean
    private ProductAdapter productAdapter;

    @MockitoBean
    private PaymentAdapter paymentAdapter;

    @MockitoBean
    private DepositAdapter depositAdapter;

    @Test
    void PAYMENT_COMPLETED_수신시_Order가_PAID로_변경되고_재고확정_이벤트가_저장된다() throws Exception {
        Order order = savedPendingOrder();
        UUID eventId = UUID.randomUUID();

        send("PAYMENT_COMPLETED", objectMapper.writeValueAsString(
            new TestEvent(
                eventId, UUID.randomUUID(), order.getId(), UUID.randomUUID(),
                new BigDecimal("10000"), null
            )
        ));

        // Kafka Consumer는 별도 스레드 — await으로 비동기 처리 완료 대기
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        });

        // Order 상태 변경 + Outbox 저장이 같은 트랜잭션으로 원자적으로 커밋됐는지 검증
        assertThat(outboxRepository.findAll())
            .anyMatch(e -> e.getEventType().name().equals("ORDER_COMPLETED"));
        assertThat(outboxRepository.findAll())
            .anyMatch(e -> e.getEventType().name().equals("ORDER_RESERVATION_CONFIRMED"));
        assertThat(outboxRepository.findAll())
            .anyMatch(e -> e.getPayload().contains(order.getUserId().toString()));
        assertThat(outboxRepository.findAll())
            .anyMatch(e -> e.getEventType().name().equals("SETTLEMENT_PAYMENT_COMPLETED"));
        // eventId가 processed_events에 저장됐는지 — 중복 수신 시 재처리 차단 확인
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void PAYMENT_FAILED_수신시_Order가_FAILED로_변경되고_보상_이벤트가_저장된다() throws Exception {
        Order order = savedPendingOrder();
        UUID eventId = UUID.randomUUID();

        send("PAYMENT_FAILED", objectMapper.writeValueAsString(
            new TestFailedEvent(eventId, UUID.randomUUID(), order.getId(), new BigDecimal("3000"))
        ));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.FAILED);
            // Saga 보상 트랜잭션 — 상태 변경과 보상 이벤트 저장이 함께 끝날 때까지 대기
            assertThat(outboxRepository.findAll())
                .anyMatch(e -> e.getEventType().name().equals("ORDER_RESERVATION_RELEASED"));
            assertThat(outboxRepository.findAll())
                .anyMatch(e -> e.getEventType().name().equals("ORDER_DEPOSIT_REFUND_REQUESTED"));
            assertThat(processedEventRepository.existsById(eventId)).isTrue();
        });
    }

    @Test
    void PAYMENT_EXPIRED_수신시_Order가_EXPIRED로_변경되고_보상_이벤트가_저장된다() throws Exception {
        Order order = savedPendingOrder();
        UUID eventId = UUID.randomUUID();

        send("PAYMENT_EXPIRED", objectMapper.writeValueAsString(
            new TestFailedEvent(eventId, UUID.randomUUID(), order.getId(), new BigDecimal("5000"))
        ));

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        });

        // 만료는 예치금 환불 없이 재고 해제만 보상
        assertThat(outboxRepository.findAll())
            .anyMatch(e -> e.getEventType().name().equals("ORDER_RESERVATION_RELEASED"));
        assertThat(processedEventRepository.existsById(eventId)).isTrue();
    }

    @Test
    void 동일_eventId로_중복_수신시_한_번만_처리된다() throws Exception {
        Order order = savedPendingOrder();
        UUID eventId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(
            new TestEvent(
                eventId, UUID.randomUUID(), order.getId(), UUID.randomUUID(),
                new BigDecimal("10000"), null
            )
        );

        // 첫 번째 전송 후 processedEvent 커밋 확인 뒤 두 번째 전송
        // → 두 번째 메시지 도착 시점에 existsById=true → 멱등성 가드가 즉시 return
        send("PAYMENT_COMPLETED", payload);
        await().atMost(5, SECONDS).until(() -> processedEventRepository.existsById(eventId));

        send("PAYMENT_COMPLETED", payload);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Order updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        });

        // 같은 order에 대한 ORDER_RESERVATION_CONFIRMED가 정확히 1개인지 확인
        long confirmedCount = outboxRepository.findAll().stream()
            .filter(e -> e.getEventType().name().equals("ORDER_RESERVATION_CONFIRMED")
                && e.getAggregateId().equals(order.getId().toString()))
            .count();
        assertThat(confirmedCount).isEqualTo(1);
    }

    private Order savedPendingOrder() {
        Order order = Order.create(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("10000")
        );
        return orderRepository.save(order);
    }
    // eventType 헤더를 포함한 Kafka 메시지 전송 — 실제 PaymentEventsConsumer가 헤더로 이벤트 종류를 구분
    private void send(String eventType, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>("payment.events", payload);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record);
    }

    // 테스트용 이벤트 페이로드 — 실제 Payment 서비스가 발행하는 이벤트 구조와 동일하게 맞춤
    record TestEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        UUID productId,
        BigDecimal totalAmount,
        java.time.LocalDateTime occurredAt
    ) {}
    record TestFailedEvent(UUID eventId, UUID paymentId, UUID orderId, BigDecimal depositAmount) {}
}
