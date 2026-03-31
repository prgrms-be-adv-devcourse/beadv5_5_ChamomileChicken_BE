package jabaclass.settlement.application.dto;

public record SettlementTransferResult(
	boolean success,
	String message
) {
	public static SettlementTransferResult ok() {
		return new SettlementTransferResult(true, null);
	}

	public static SettlementTransferResult fail(String message) {
		return new SettlementTransferResult(false, message);
	}
}