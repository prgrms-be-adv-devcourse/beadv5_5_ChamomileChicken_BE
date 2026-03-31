package jabaclass.settlement.domain.model;

public enum SettlementStatus {
	READY,          // 정산 계산 완료, 송금 대기
	HOLD,           // 송금 보류 (계좌 미등록, 음수 정산금 등)
	TRANSFERRING,   // 송금 진행 중
	SENT,           // 송금 완료
	FAILED          // 송금 실패
}