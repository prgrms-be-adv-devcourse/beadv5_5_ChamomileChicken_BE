package jabaclass.settlement.kafka;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.settlement.domain.model.promotion.PromotionType;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.infrastructure.persistence.SellerPromotionJpaRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementPromotionJpaRepository;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = {"settlement.events", "settlement.events.dlq"})
@DirtiesContext
@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
class SettlementEventsConsumerTest {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private SettlementTargetJpaRepository settlementTargetJpaRepository;

	@Autowired
	private SellerPromotionJpaRepository sellerPromotionJpaRepository;

	@Autowired
	private SettlementPromotionJpaRepository settlementPromotionJpaRepository;

	@BeforeEach
	void setUp() {
		if (settlementPromotionJpaRepository.count() > 0) {
			return;
		}

		settlementPromotionJpaRepository.save(new SettlementPromotion(
			"신규 가입 셀러 30일 우대 수수료",
			PromotionType.NEW_SELLER,
			new BigDecimal("0.0300"),
			30,
			true
		));
	}

	@Test
	void SETTLEMENT_PAYMENT_COMPLETED_수신시_settlementTarget에_저장된다() throws Exception {
		UUID paymentId = UUID.randomUUID();
		send("SETTLEMENT_PAYMENT_COMPLETED", objectMapper.writeValueAsString(new PaymentEvent(
			UUID.randomUUID(),
			UUID.randomUUID(),
			paymentId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("20000"),
			LocalDateTime.of(2026, 4, 20, 12, 0)
		)));

		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(findPaymentTarget(paymentId)).isPresent();
		});
	}

	@Test
	void SETTLEMENT_REFUND_COMPLETED_수신시_refund_target이_저장된다() throws Exception {
		UUID refundId = UUID.randomUUID();
		send("SETTLEMENT_REFUND_COMPLETED", objectMapper.writeValueAsString(new RefundEvent(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			refundId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("15000"),
			LocalDateTime.of(2026, 4, 21, 9, 30)
		)));

		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(findRefundTarget(refundId)).isPresent();
		});
	}

	@Test
	void 동일_eventId로_중복_수신시_정산타겟은_한_번만_적재된다() throws Exception {
		UUID eventId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		String payload = objectMapper.writeValueAsString(new PaymentEvent(
			eventId,
			UUID.randomUUID(),
			paymentId,
			UUID.randomUUID(),
			UUID.randomUUID(),
			new BigDecimal("21000"),
			LocalDateTime.of(2026, 4, 20, 18, 0)
		));

		send("SETTLEMENT_PAYMENT_COMPLETED", payload);
		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(
				settlementTargetJpaRepository.findAll().stream()
					.filter(target -> eventId.equals(target.getSourceEventId()))
					.count()
			).isEqualTo(1L);
		});

		send("SETTLEMENT_PAYMENT_COMPLETED", payload);

		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(
				settlementTargetJpaRepository.findAll().stream()
					.filter(target -> eventId.equals(target.getSourceEventId()))
					.count()
			).isEqualTo(1L);
		});
	}

	@Test
	void USER_SELLER_APPROVED_수신시_신규셀러_프로모션이_등록된다() throws Exception {
		UUID sellerId = UUID.randomUUID();
		send("USER_SELLER_APPROVED", objectMapper.writeValueAsString(new SellerApprovedPayload(
			UUID.randomUUID(),
			"SELLER_APPROVED",
			sellerId,
			LocalDateTime.of(2026, 4, 24, 10, 0)
		)));

		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(
				sellerPromotionJpaRepository.findAll().stream()
					.filter(promotion -> sellerId.equals(promotion.getSellerId()))
					.count()
			).isEqualTo(1L);
		});
	}

	@Test
	void 동일_신규셀러_이벤트를_중복_수신해도_프로모션은_한_번만_등록된다() throws Exception {
		UUID sellerId = UUID.randomUUID();
		String payload = objectMapper.writeValueAsString(new SellerApprovedPayload(
			UUID.randomUUID(),
			"SELLER_APPROVED",
			sellerId,
			LocalDateTime.of(2026, 4, 24, 10, 0)
		));

		send("USER_SELLER_APPROVED", payload);
		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(
				sellerPromotionJpaRepository.findAll().stream()
					.filter(promotion -> sellerId.equals(promotion.getSellerId()))
					.count()
			).isEqualTo(1L);
		});

		send("USER_SELLER_APPROVED", payload);
		await().atMost(5, SECONDS).untilAsserted(() -> {
			assertThat(
				sellerPromotionJpaRepository.findAll().stream()
					.filter(promotion -> sellerId.equals(promotion.getSellerId()))
					.count()
			).isEqualTo(1L);
		});
	}

	private void send(String eventType, String payload) {
		ProducerRecord<String, String> record = new ProducerRecord<>("settlement.events", payload);
		record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
		kafkaTemplate.send(record);
	}

	private java.util.Optional<SettlementTarget> findPaymentTarget(UUID paymentId) {
		return settlementTargetJpaRepository.findAll().stream()
			.filter(target -> paymentId.equals(target.getPaymentId()))
			.filter(target -> target.getTargetType() == SettlementTargetType.PAYMENT)
			.findFirst();
	}

	private java.util.Optional<SettlementTarget> findRefundTarget(UUID refundId) {
		return settlementTargetJpaRepository.findAll().stream()
			.filter(target -> refundId.equals(target.getRefundId()))
			.findFirst();
	}

	record PaymentEvent(
		UUID eventId,
		UUID orderId,
		UUID paymentId,
		UUID sellerId,
		UUID productId,
		BigDecimal settlementBaseAmount,
		LocalDateTime occurredAt
	) {}

	record RefundEvent(
		UUID eventId,
		UUID orderId,
		UUID paymentId,
		UUID refundId,
		UUID sellerId,
		UUID productId,
		BigDecimal settlementBaseAmount,
		LocalDateTime occurredAt
	) {}

	record SellerApprovedPayload(
		UUID eventId,
		String type,
		UUID sellerId,
		LocalDateTime approvedAt
	) {}
}
