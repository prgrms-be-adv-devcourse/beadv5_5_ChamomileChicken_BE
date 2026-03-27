package jabaclass.product.domain.model.status;

import java.util.Arrays;

public enum OrderStatus {
	PENDING("주문/결제 대기"),
	PAID("결제 완료"),
	CANCELLED("주문 취소"),
	REFUNDED("환불 완료"),
	FAILED("실패");

	private final String statusName;

	OrderStatus(String statusName) {
		this.statusName = statusName;
	}

	public String getStatusName() {
		return this.statusName;
	}

	public static OrderStatus fromDescription(String statusName) {
		return Arrays.stream(values())
			.filter(status -> status.statusName.equals(statusName))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태값"));
	}
}
