package jabaclass.user.deposit.presentation.dto.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.user.deposit.presentation.dto.request.DepositChargeRequestDto;
import jabaclass.user.deposit.presentation.dto.response.DepositChargeResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositDetailResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositHistoryResponseDto;
import jabaclass.user.deposit.presentation.dto.response.DepositMeResponseDto;

@Tag(name = "Deposit", description = "예치금 API")
public interface DepositApi {

	@Operation(
		summary = "예치금 이력 조회",
		description = "로그인한 사용자의 예치금 이력을 조회합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	ResponseEntity<DepositHistoryResponseDto> findAllDepositHistories(
		@AuthenticationPrincipal UUID userId
	);

	@Operation(
		summary = "예치금 조회",
		description = "로그인한 사용자의 현재 예치금 잔액을 조회합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	ResponseEntity<DepositMeResponseDto> findMyDeposit(
		@AuthenticationPrincipal UUID userId
	);

	@Operation(
		summary = "예치금 상세 조회",
		description = "예치금 이력 ID로 상세 정보를 조회합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	ResponseEntity<DepositDetailResponseDto> findDepositHistory(
		@PathVariable UUID depositHistoryId
	);

	@Operation(
		summary = "예치금 충전",
		description = "결제 수단과 충전 금액으로 예치금을 충전합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	ResponseEntity<DepositChargeResponseDto> chargeDeposit(
		@AuthenticationPrincipal UUID userId,
		@RequestBody DepositChargeRequestDto request
	);
}