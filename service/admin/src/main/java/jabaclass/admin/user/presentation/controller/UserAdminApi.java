package jabaclass.admin.user.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.admin.common.dto.ApiResponseDto;
import jabaclass.admin.user.presentation.dto.response.UserAdminResponseDto;

@Tag(name = "Admin - User", description = "어드민 유저 관리 API")
public interface UserAdminApi {

	@Operation(summary = "전체 유저 목록 조회 (필터링/검색)")
	ResponseEntity<ApiResponseDto<Page<UserAdminResponseDto>>> getUsers(
		Pageable pageable,
		@Parameter(description = "역할 필터 (USER/SELLER/ADMIN)") @RequestParam(required = false) String role,
		@Parameter(description = "이름 검색 (부분 일치)") @RequestParam(required = false) String name,
		@Parameter(description = "이메일 검색 (부분 일치)") @RequestParam(required = false) String email
	);

	@Operation(summary = "특정 유저 상세 조회")
	ResponseEntity<ApiResponseDto<UserAdminResponseDto>> getUser(@PathVariable UUID userId);

	@Operation(summary = "셀러 승인")
	ResponseEntity<ApiResponseDto<Void>> approveSeller(@PathVariable UUID userId);
}
