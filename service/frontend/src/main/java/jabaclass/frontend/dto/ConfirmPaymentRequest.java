package jabaclass.frontend.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmPaymentRequest {
	private UUID orderId;
	private String paymentKey;
	private int amount;
}
