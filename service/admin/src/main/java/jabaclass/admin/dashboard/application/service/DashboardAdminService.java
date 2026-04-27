package jabaclass.admin.dashboard.application.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.dashboard.application.usecase.DashboardAdminUseCase;
import jabaclass.admin.dashboard.presentation.dto.response.DashboardResponseDto;
import jabaclass.admin.order.domain.model.OrderStatus;
import jabaclass.admin.order.domain.repository.OrderAdminRepository;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import jabaclass.admin.settlement.domain.repository.SettlementAdminRepository;
import jabaclass.admin.user.domain.repository.UserAdminRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAdminService implements DashboardAdminUseCase {

	private final OrderAdminRepository orderAdminRepository;
	private final UserAdminRepository userAdminRepository;
	private final ProductAdminRepository productAdminRepository;
	private final SettlementAdminRepository settlementAdminRepository;

	@Override
	public DashboardResponseDto getDashboard(int year) {
		DashboardResponseDto.DomainOverview overview = buildOverview();
		List<DashboardResponseDto.MonthlyOrderStat> monthlyOrderStats = buildMonthlyOrderStats(year);
		List<DashboardResponseDto.MonthlyNewUserStat> monthlyNewUserStats = buildMonthlyNewUserStats(year);

		return new DashboardResponseDto(overview, monthlyOrderStats, monthlyNewUserStats);
	}

	private DashboardResponseDto.DomainOverview buildOverview() {
		long totalUsers = userAdminRepository.countAll();
		long activeProducts = productAdminRepository.countActiveProducts();
		long pendingOrders = orderAdminRepository.countByStatus(OrderStatus.PENDING);
		String currentMonth = YearMonth.now().toString();
		BigDecimal currentMonthSettlement = settlementAdminRepository.sumSettlementAmountByMonth(currentMonth);

		return new DashboardResponseDto.DomainOverview(totalUsers, activeProducts, pendingOrders, currentMonthSettlement);
	}

	private List<DashboardResponseDto.MonthlyOrderStat> buildMonthlyOrderStats(int year) {
		List<Object[]> rows = orderAdminRepository.findMonthlyOrderStats(year);
		Map<Integer, Object[]> dataByMonth = new HashMap<>();
		for (Object[] row : rows) {
			dataByMonth.put(((Number) row[0]).intValue(), row);
		}

		List<DashboardResponseDto.MonthlyOrderStat> result = new ArrayList<>();
		for (int month = 1; month <= 12; month++) {
			String monthLabel = String.format("%d-%02d", year, month);
			if (dataByMonth.containsKey(month)) {
				Object[] row = dataByMonth.get(month);
				long orderCount = ((Number) row[1]).longValue();
				BigDecimal totalRevenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
				result.add(new DashboardResponseDto.MonthlyOrderStat(monthLabel, orderCount, totalRevenue));
			} else {
				result.add(new DashboardResponseDto.MonthlyOrderStat(monthLabel, 0L, BigDecimal.ZERO));
			}
		}
		return result;
	}

	private List<DashboardResponseDto.MonthlyNewUserStat> buildMonthlyNewUserStats(int year) {
		List<Object[]> rows = userAdminRepository.findMonthlyNewUserStats(year);
		Map<Integer, Long> dataByMonth = new HashMap<>();
		for (Object[] row : rows) {
			dataByMonth.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
		}

		List<DashboardResponseDto.MonthlyNewUserStat> result = new ArrayList<>();
		for (int month = 1; month <= 12; month++) {
			String monthLabel = String.format("%d-%02d", year, month);
			result.add(new DashboardResponseDto.MonthlyNewUserStat(monthLabel, dataByMonth.getOrDefault(month, 0L)));
		}
		return result;
	}
}