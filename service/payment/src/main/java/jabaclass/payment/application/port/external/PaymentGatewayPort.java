package jabaclass.payment.application.port.external;

public interface PaymentGatewayPort {
	void confirm(String paymentKey, String orderId, int amount);

	void refund(String paymentKey, int cancelAmount);
}
