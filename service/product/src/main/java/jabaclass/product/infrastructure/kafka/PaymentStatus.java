package jabaclass.product.infrastructure.kafka;

public enum PaymentStatus {
	// 결제 성공
	SUCCESS,
	// 환불
	REFUNDED,
	// 취소,실패
	CANCELLED;
}
