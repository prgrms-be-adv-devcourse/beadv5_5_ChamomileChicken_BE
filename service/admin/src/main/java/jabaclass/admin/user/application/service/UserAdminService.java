package jabaclass.admin.user.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.user.application.usecase.UserAdminUseCase;
import jabaclass.admin.user.domain.repository.UserAdminRepository;
import jabaclass.admin.user.presentation.dto.response.UserAdminResponseDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAdminService implements UserAdminUseCase {

	private final UserAdminRepository userAdminRepository;

	@Override
	@Transactional(readOnly = true, transactionManager = "userTransactionManager")
	public Page<UserAdminResponseDto> getUsers(Pageable pageable) {
		return userAdminRepository.findAll(pageable)
			.map(UserAdminResponseDto::from);
	}

	@Override
	@Transactional(readOnly = true, transactionManager = "userTransactionManager")
	public UserAdminResponseDto getUser(UUID userId) {
		return userAdminRepository.findById(userId)
			.map(UserAdminResponseDto::from)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND));
	}

	@Override
	@Transactional(transactionManager = "userTransactionManager")
	public void approveSeller(UUID userId) {
		userAdminRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND))
			.approveSeller();
	}
}
