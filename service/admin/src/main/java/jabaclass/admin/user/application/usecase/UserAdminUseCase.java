package jabaclass.admin.user.application.usecase;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.user.presentation.dto.response.UserAdminResponseDto;

public interface UserAdminUseCase {
	Page<UserAdminResponseDto> getUsers(Pageable pageable);

	UserAdminResponseDto getUser(UUID userId);

	void approveSeller(UUID userId);
}
