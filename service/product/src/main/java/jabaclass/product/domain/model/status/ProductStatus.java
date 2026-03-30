package jabaclass.product.domain.model.status;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ProductStatus {
	// 공개
	ENABLE("공개"),
	// 비공개
	DISABLE("비공개");

	private final String statusName;

	ProductStatus(String statusName) {
		this.statusName = statusName;
	}

	public String getStatusName() {
		return this.statusName;
	}

	@JsonCreator
	public static ProductStatus from(String value) {
		return ProductStatus.valueOf(value.replace("\"", ""));
	}

}
