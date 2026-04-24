package jabaclass.settlement.infrastructure.kafka;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementPaymentCompletedEvent;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementRefundCompletedEvent;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetEventHandler {

	private static final LocalTime MONTHLY_SETTLEMENT_GRACE_DEADLINE_TIME = LocalTime.of(1, 0);

	private final SettlementTargetJpaRepository settlementTargetJpaRepository;

	public void handlePaymentCompleted(SettlementPaymentCompletedEvent event) {
		LocalDateTime receivedAt = LocalDateTime.now();
		saveIgnoringDuplicateSourceEvent(SettlementTarget.forPayment(
			event.eventId(),
			resolveSettlementMonth(event.occurredAt(), receivedAt),
			event.sellerId(),
			event.orderId(),
			event.paymentId(),
			event.productId(),
			event.settlementBaseAmount(),
			event.occurredAt()
		));
	}

	public void handleRefundCompleted(SettlementRefundCompletedEvent event) {
		LocalDateTime receivedAt = LocalDateTime.now();
		saveIgnoringDuplicateSourceEvent(SettlementTarget.forRefund(
			event.eventId(),
			resolveSettlementMonth(event.occurredAt(), receivedAt),
			event.sellerId(),
			event.orderId(),
			event.paymentId(),
			event.refundId(),
			event.productId(),
			event.settlementBaseAmount(),
			event.occurredAt()
		));
	}

	String resolveSettlementMonth(LocalDateTime occurredAt, LocalDateTime receivedAt) {
		YearMonth receivedMonth = YearMonth.from(receivedAt);
		if (occurredAt.isAfter(receivedAt)) {
			return receivedMonth.toString();
		}

		YearMonth occurredMonth = YearMonth.from(occurredAt);
		LocalDateTime graceDeadline = occurredMonth.plusMonths(1)
			.atDay(1)
			.atTime(MONTHLY_SETTLEMENT_GRACE_DEADLINE_TIME);

		if (receivedAt.isBefore(graceDeadline)) {
			return occurredMonth.toString();
		}

		return receivedMonth.toString();
	}

	private void saveIgnoringDuplicateSourceEvent(SettlementTarget target) {
		try {
			settlementTargetJpaRepository.saveAndFlush(target);
		} catch (DataIntegrityViolationException e) {
			if (!isDuplicateSourceEvent(e)) {
				throw e;
			}
		}
	}

	private boolean isDuplicateSourceEvent(DataIntegrityViolationException e) {
		Throwable current = e;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.toLowerCase().contains("source_event_id")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
