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

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.dto.SellerSettlementAccount;
import jabaclass.settlement.application.dto.SettlementTransferCommand;
import jabaclass.settlement.application.dto.SettlementTransferResult;
import jabaclass.settlement.application.port.external.SellerSettlementPort;
import jabaclass.settlement.application.port.external.SettlementTransferPort;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementTransfer;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SettlementTransferService {

	private final SettlementRepository settlementRepository;
	private final SettlementTransferStatePersistenceService settlementTransferStatePersistenceService;
	private final SellerSettlementPort sellerSettlementPort;
	private final SettlementTransferPort settlementTransferPort;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public int transferMonthly(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
		}

		List<Settlement> settlements =
			settlementRepository.findBySettlementMonthAndStatus(settlementMonth, SettlementStatus.READY);

		return transferSettlements(settlements);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public int transferSettlements(List<Settlement> settlements) {
		Map<UUID, SellerSettlementAccount> accountMap = sellerSettlementPort.fetchSellerSettlementAccounts(
			settlements.stream()
				.map(Settlement::getSellerId)
				.collect(Collectors.toSet())
		).stream().collect(Collectors.toMap(SellerSettlementAccount::sellerId, Function.identity()));

		int successCount = 0;

		for (Settlement settlement : settlements) {
			SettlementTransfer transferHistory = null;
			try {
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

				settlement.markTransferring();
				transferHistory = SettlementTransfer.requested(
					settlement.getId(),
					account.bankCode(),
					account.accountNumber(),
					settlement.getSettlementAmount()
				);
				saveTransferResult(settlement, transferHistory);

				SettlementTransferCommand command = new SettlementTransferCommand(
					settlement.getId(),
					settlement.getSellerId(),
					account.bankCode(),
					account.accountNumber(),
					account.accountHolder(),
					settlement.getSettlementAmount()
				);

				SettlementTransferResult result = settlementTransferPort.transfer(command);

				if (result.success()) {
					settlement.markSent(LocalDateTime.now());
					transferHistory.markSent();
					successCount++;
				} else {
					settlement.markFailed(result.message());
					transferHistory.markFailed(result.message());
				}
				saveTransferResult(settlement, transferHistory);
			} catch (Exception e) {
				settlement.markFailed(e.getMessage());
				if (transferHistory == null) {
					transferHistory = SettlementTransfer.requested(
						settlement.getId(),
						null,
						null,
						settlement.getSettlementAmount()
					);
				}
				transferHistory.markFailed(e.getMessage());
				saveTransferResult(settlement, transferHistory);
				log.error("[SETTLEMENT_TRANSFER] settlementId={} 송금 실패", settlement.getId(), e);
			}
		}

		return successCount;
	}

	private void saveTransferResult(Settlement settlement, SettlementTransfer transferHistory) {
		settlementTransferStatePersistenceService.saveTransferState(settlement, transferHistory);
	}
}
