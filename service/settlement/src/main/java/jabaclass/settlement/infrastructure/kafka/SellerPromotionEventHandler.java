package jabaclass.settlement.infrastructure.kafka;

import org.springframework.stereotype.Component;

import jabaclass.settlement.application.service.promotion.NewSellerPromotionService;
import jabaclass.settlement.infrastructure.kafka.dto.SellerApprovedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SellerPromotionEventHandler {

	private final NewSellerPromotionService newSellerPromotionService;

	public void handleSellerApproved(SellerApprovedEvent event) {
		newSellerPromotionService.register(event.sellerId(), event.approvedAt());
	}
}
