package jabaclass.admin.dashboard.application.usecase;

import jabaclass.admin.dashboard.presentation.dto.response.DashboardResponseDto;

public interface DashboardAdminUseCase {

	DashboardResponseDto getDashboard(int year);
}