package jabaclass.admin.user.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.user.application.usecase.UserAdminUseCase;
import jabaclass.admin.user.presentation.dto.response.UserAdminResponseDto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admins/users")
@RequiredArgsConstructor
public class UserAdminController implements UserAdminApi {

	private final UserAdminUseCase userAdminUseCase;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponseDto<Page<UserAdminResponseDto>>> getUsers(Pageable pageable) {
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "유저 목록 조회 성공", userAdminUseCase.getUsers(pageable))
		);
	}

	@Override
	@GetMapping("/{userId}")
	public ResponseEntity<ApiResponseDto<UserAdminResponseDto>> getUser(@PathVariable UUID userId) {
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "유저 상세 조회 성공", userAdminUseCase.getUser(userId))
		);
	}

	@Override
	@PatchMapping("/{userId}/approve-seller")
	public ResponseEntity<ApiResponseDto<Void>> approveSeller(@PathVariable UUID userId) {
		userAdminUseCase.approveSeller(userId);
		return ResponseEntity.ok(
			ApiResponseDto.success(HttpStatus.OK, "셀러 승인 완료", null)
		);
	}
}
