package jabaclass.settlement.application.port.external;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jabaclass.settlement.application.dto.SellerSettlementAccount;

public interface SellerSettlementPort {
	List<SellerSettlementAccount> fetchSellerSettlementAccounts(Set<UUID> sellerIds);
}
