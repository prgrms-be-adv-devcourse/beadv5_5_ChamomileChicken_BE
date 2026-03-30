package jabaclass.user.user.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jabaclass.user.user.application.usercase.UserUseCase;
import jabaclass.user.user.presentation.dto.request.SellerBulkReadRequestDto;
import jabaclass.user.user.presentation.dto.request.UserBulkReadRequestDto;
import jabaclass.user.user.presentation.dto.response.SellerSettlementAccountResponseDto;
import jabaclass.user.user.presentation.dto.response.SellerSettlementDetailResponseDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserInternalController implements UserInternalApi {

	private final UserUseCase userUseCase;

	@Override
	@PostMapping("/bulk")
	public ResponseEntity<List<UserResponseDto>> getUsersByIds(
		@Valid @RequestBody UserBulkReadRequestDto request
	) {
		return ResponseEntity.ok(userUseCase.getUsersByIds(request.userIds()));
	}

	@Override
	@PostMapping("/sellers/bulk")
	public ResponseEntity<List<SellerSettlementDetailResponseDto>> getSellersByIds(
		@Valid @RequestBody SellerBulkReadRequestDto request
	) {
		return ResponseEntity.ok(userUseCase.getSellerDetailsByIds(request.sellerIds()));
	}

	@Override
	@PostMapping("/sellers/settlement-accounts/bulk")
	public ResponseEntity<List<SellerSettlementAccountResponseDto>> getSellerSettlementAccountsByIds(
		@Valid @RequestBody SellerBulkReadRequestDto request
	) {
		return ResponseEntity.ok(userUseCase.getSellerSettlementAccountsByIds(request.sellerIds()));
	}
}
