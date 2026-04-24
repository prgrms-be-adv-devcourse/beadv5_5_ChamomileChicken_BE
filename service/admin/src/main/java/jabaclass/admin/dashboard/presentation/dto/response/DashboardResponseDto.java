package jabaclass.admin.dashboard.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponseDto(
	DomainOverview overview,
	List<MonthlyOrderStat> monthlyOrderStats,
	List<MonthlyNewUserStat> monthlyNewUserStats
) {

	public record DomainOverview(
		long totalUsers,
		long activeProducts,
		long pendingOrders,
		BigDecimal currentMonthSettlement
	) {}

	public record MonthlyOrderStat(
		String month,
		long orderCount,
		BigDecimal totalRevenue
	) {}

	public record MonthlyNewUserStat(
		String month,
		long newUserCount
	) {}
}