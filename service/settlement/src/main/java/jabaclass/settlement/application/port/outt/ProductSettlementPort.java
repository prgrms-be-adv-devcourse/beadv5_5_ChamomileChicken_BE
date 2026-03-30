package jabaclass.settlement.application.port.outt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jabaclass.settlement.application.dto.ProductSettlementDetail;

public interface ProductSettlementPort {
	List<ProductSettlementDetail> fetchProducts(Set<UUID> productIds);
}