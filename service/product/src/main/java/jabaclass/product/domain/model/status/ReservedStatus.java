package jabaclass.product.domain.model.status;

public enum ReservedStatus {
	// 인원 마감
	FULL("인원 마감"),
	// 예약 가능
	AVAILABLE("예약 가능"),
	// 대기
	PENDING("대기"),
	// 날짜 마감
	CLOSED("날짜마감");

	private final String statusName;

	ReservedStatus(String statusName) {
		this.statusName = statusName;
	}

	public String getStatusName() {
		return this.statusName;
	}

}
