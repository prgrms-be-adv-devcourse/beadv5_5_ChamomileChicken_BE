package jabaclass.user.deposit.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.user.common.apidocs.ApiErrorSpec;
import jabaclass.user.common.apidocs.ApiErrorSpecs;
import jabaclass.user.deposit.domain.exception.DepositErrorCode;
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
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = DepositErrorCode.class,
			constant = "NOT_FOUND_USER",
			summary = "존재하지 않는 회원입니다"
		)
	})
	ResponseEntity<DepositMeResponseDto> findMyDeposit(
		@AuthenticationPrincipal UUID userId
	);

	@Operation(
		summary = "예치금 상세 조회",
		description = "예치금 이력 ID로 상세 정보를 조회합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = DepositErrorCode.class,
			constant = "NOT_FOUND_DEPOSIT_HISTORY",
			summary = "존재하지 않는 예치금 이력입니다"
		)
	})
	ResponseEntity<DepositDetailResponseDto> findDepositHistory(
		@PathVariable UUID depositHistoryId
	);

	@Operation(
		summary = "예치금 충전",
		description = "결제 수단과 충전 금액으로 예치금을 충전합니다."
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiErrorSpecs({
		@ApiErrorSpec(
			value = DepositErrorCode.class,
			constant = "NOT_FOUND_USER",
			summary = "존재하지 않는 회원입니다"
		),
		@ApiErrorSpec(
			value = DepositErrorCode.class,
			constant = "PAYMENT_FAILED",
			summary = "결제에 실패했습니다"
		),
		@ApiErrorSpec(
			value = DepositErrorCode.class,
			constant = "PAYMENT_SERVICE_UNAVAILABLE",
			summary = "결제 서비스에 연결할 수 없습니다"
		)
	})
	ResponseEntity<DepositChargeResponseDto> chargeDeposit(
		@AuthenticationPrincipal UUID userId,
		@RequestBody DepositChargeRequestDto request
	);
}