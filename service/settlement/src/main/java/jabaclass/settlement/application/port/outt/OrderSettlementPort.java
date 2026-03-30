package jabaclass.settlement.application.port.outt;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jabaclass.settlement.application.dto.OrderSettlementDetail;

public interface OrderSettlementPort {
	List<OrderSettlementDetail> fetchOrders(Set<UUID> orderIds);
}