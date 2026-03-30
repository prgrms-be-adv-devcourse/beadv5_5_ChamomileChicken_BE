package jabaclass.settlement.application.port.outt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jabaclass.settlement.application.dto.SellerSettlementAccount;
import jabaclass.settlement.application.dto.SellerSettlementDetail;

public interface SellerSettlementPort {
	List<SellerSettlementDetail> fetchSellers(Set<UUID> sellerIds);

	List<SellerSettlementAccount> fetchSellerSettlementAccounts(Set<UUID> sellerIds);
}
