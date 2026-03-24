package jabaclass.product.event;

import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jabaclass.product.infrastructure.event.dto.ProductEventResposeDto;

@Component
public class ProductTestEventListener {

	private UUID lastProductId;

	@EventListener
	public void handle(ProductEventResposeDto event) {
		lastProductId = event.productId();
		// 테스트용: 단순 기록
		lastProductId = event.productId();
		System.out.println("테스트용 이벤트 리스너 호출: " + lastProductId);
	}

	public UUID getLastProductId() {
		return lastProductId;
	}

}
