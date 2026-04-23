package jabaclass.settlement.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.SellerSettlementAccount;
import jabaclass.settlement.application.dto.SettlementTransferCheckStatus;
import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.dto.SettlementTransferStatusResult;
import jabaclass.settlement.application.port.external.SellerSettlementPort;
import jabaclass.settlement.application.port.external.SettlementTransferPort;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTransfer;
import jabaclass.settlement.domain.repository.SettlementTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SettlementTransferService {

	private final SettlementTransferRepository settlementTransferRepository;
	private final SettlementTransferStatePersistenceService settlementTransferStatePersistenceService;
	private final SellerSettlementPort sellerSettlementPort;
	private final SettlementTransferPort settlementTransferPort;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void transferSettlements(List<Settlement> settlements) {
		transferSettlementsInternal(settlements);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void reconcileTransferringSettlements(List<Settlement> settlements) {
		for (Settlement settlement : settlements) {
			reconcileTransferringSettlement(settlement);
		}
	}

	private void transferSettlementsInternal(List<Settlement> settlements) {
		Map<UUID, SellerSettlementAccount> accountMap = sellerSettlementPort.fetchSellerSettlementAccounts(
			settlements.stream()
				.map(Settlement::getSellerId)
				.collect(Collectors.toSet())
		).stream().collect(Collectors.toMap(SellerSettlementAccount::sellerId, Function.identity()));

		for (Settlement settlement : settlements) {
			if (!settlement.isTransferable()) {
				settlement.hold("정산 금액이 0 이하이므로 송금 보류");
				saveTransferResult(settlement, SettlementTransfer.hold(
					settlement.getId(),
					settlement.getSettlementAmount(),
					"정산 금액이 0 이하이므로 송금 보류"
				));
				continue;
			}

			SellerSettlementAccount account = accountMap.get(settlement.getSellerId());

			if (account == null) {
				settlement.hold("판매자 정산 계좌 정보가 없습니다.");
				saveTransferResult(settlement, SettlementTransfer.hold(
					settlement.getId(),
					settlement.getSettlementAmount(),
					"판매자 정산 계좌 정보가 없습니다."
				));
				continue;
			}

			if (!account.isTransferable()) {
				settlement.hold("판매자 정산 계좌가 비활성 상태입니다.");
				saveTransferResult(settlement, SettlementTransfer.hold(
					settlement.getId(),
					settlement.getSettlementAmount(),
					"판매자 정산 계좌가 비활성 상태입니다."
				));
				continue;
			}

			SettlementTransfer transferHistory = requestTransfer(settlement, account);
			SettlementTransferResult result = requestExternalTransfer(settlement, account);
			if (result == null) {
				continue;
			}

			applyTransferResult(settlement, transferHistory, result);
			persistFinalTransferResult(settlement, transferHistory);
		}
	}

	private SettlementTransfer requestTransfer(Settlement settlement, SellerSettlementAccount account) {
		settlement.markTransferring();
		SettlementTransfer transferHistory = SettlementTransfer.requested(
			settlement.getId(),
			account.bankCode(),
			account.accountNumber(),
			settlement.getSettlementAmount()
		);
		saveTransferResult(settlement, transferHistory);
		return transferHistory;
	}

	private SettlementTransferResult requestExternalTransfer(Settlement settlement, SellerSettlementAccount account) {
		try {
			return settlementTransferPort.transfer(new SettlementTransferCommand(
				settlement.getId(),
				settlement.getSellerId(),
				account.bankCode(),
				account.accountNumber(),
				account.accountHolder(),
				settlement.getSettlementAmount()
			));
		} catch (Exception e) {
			log.error(
				"[SETTLEMENT_TRANSFER] settlementId={} 외부 송금 호출 후 상태 미확정. reconcile step에서 재확인합니다.",
				settlement.getId(),
				e
			);
			return null;
		}
	}

	private void applyTransferResult(
		Settlement settlement,
		SettlementTransfer transferHistory,
		SettlementTransferResult result
	) {
		if (result.success()) {
			settlement.markSent(LocalDateTime.now());
			transferHistory.markSent();
			return;
		}

		settlement.markFailed(result.message());
		transferHistory.markFailed(result.message());
	}

	private void persistFinalTransferResult(Settlement settlement, SettlementTransfer transferHistory) {
		try {
			saveTransferResult(settlement, transferHistory);
		} catch (Exception e) {
			log.error(
				"[SETTLEMENT_TRANSFER] settlementId={} 외부 송금 결과 저장에 실패했습니다. "
					+ "현재 DB 상태는 TRANSFERRING/REQUESTED일 수 있으며 reconcile step에서 재확인합니다.",
				settlement.getId(),
				e
			);
			throw e;
		}
	}

	private void reconcileTransferringSettlement(Settlement settlement) {
		SettlementTransfer transferHistory = settlementTransferRepository.findLatestBySettlementId(settlement.getId())
			.orElse(null);

		if (transferHistory == null) {
			log.warn("[SETTLEMENT_TRANSFER] settlementId={} 송금 이력이 없어 상태 확인을 건너뜁니다.", settlement.getId());
			return;
		}

		SettlementTransferStatusResult statusResult = settlementTransferPort.getTransferStatus(settlement.getId());
		if (statusResult.status() == SettlementTransferCheckStatus.SENT) {
			settlement.markSent(LocalDateTime.now());
			transferHistory.markSent();
			saveTransferResult(settlement, transferHistory);
			return;
		}

		if (statusResult.status() == SettlementTransferCheckStatus.FAILED) {
			settlement.markFailed(statusResult.message());
			transferHistory.markFailed(statusResult.message());
			saveTransferResult(settlement, transferHistory);
			return;
		}

		log.info(
			"[SETTLEMENT_TRANSFER] settlementId={} 외부 송금 상태 미확정. transferStatus={}",
			settlement.getId(),
			statusResult.status()
		);
	}

	private void saveTransferResult(Settlement settlement, SettlementTransfer transferHistory) {
		settlementTransferStatePersistenceService.saveTransferState(settlement, transferHistory);
	}
}
