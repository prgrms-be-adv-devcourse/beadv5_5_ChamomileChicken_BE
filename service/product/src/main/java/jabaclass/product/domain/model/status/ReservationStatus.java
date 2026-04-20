package jabaclass.product.domain.model.status;

import java.util.Arrays;

public enum ReservationStatus {
	RESERVED("예약 보류"),
	CONFIRMED("예약 확정"),
	RELEASED("예약 해제"),
	REFUNDED("환불 완료");

	private final String statusName;

	ReservationStatus(String statusName) {
		this.statusName = statusName;
	}

	public String getStatusName() {
		return this.statusName;
	}

	public static ReservationStatus fromDescription(String statusName) {
		return Arrays.stream(values())
			.filter(status -> status.statusName.equals(statusName))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태값입니다."));
	}
}
