package jabaclass.settlement.application.dto;

public record SettlementTransferStatusResult(
	SettlementTransferCheckStatus status,
	String message
) {

	public static SettlementTransferStatusResult sent() {
		return new SettlementTransferStatusResult(SettlementTransferCheckStatus.SENT, null);
	}

	public static SettlementTransferStatusResult failed(String message) {
		return new SettlementTransferStatusResult(SettlementTransferCheckStatus.FAILED, message);
	}

	public static SettlementTransferStatusResult notFound() {
		return new SettlementTransferStatusResult(SettlementTransferCheckStatus.NOT_FOUND, null);
	}
}
