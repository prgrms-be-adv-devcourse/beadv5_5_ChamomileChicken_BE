package jabaclass.user.user.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.user.user.presentation.dto.request.SellerBulkReadRequestDto;
import jabaclass.user.user.presentation.dto.request.UserBulkReadRequestDto;
import jabaclass.user.user.presentation.dto.response.SellerSettlementAccountResponseDto;
import jabaclass.user.user.presentation.dto.response.SellerSettlementDetailResponseDto;
import jabaclass.user.user.presentation.dto.response.UserResponseDto;
import jakarta.validation.Valid;

@Tag(name = "User Internal", description = "사용자 내부 연동 API")
public interface UserInternalApi {

	@Operation(
		summary = "사용자 벌크 조회",
		description = """
			사용자 ID 목록으로 사용자 정보를 조회합니다.
			- 요청한 사용자 ID 순서를 기준으로 응답합니다.
			- 존재하지 않는 사용자 ID는 응답에서 제외합니다.
			"""
	)
	ResponseEntity<List<UserResponseDto>> getUsersByIds(
		@Valid @RequestBody UserBulkReadRequestDto request
	);

	@Operation(
		summary = "판매자 벌크 조회",
		description = """
			판매자 ID 목록으로 정산에 필요한 판매자 정보를 조회합니다.
			- 판매자 여부
			- 정산 계좌 등록 여부
			- 정산 계좌 활성 여부
			"""
	)
	ResponseEntity<List<SellerSettlementDetailResponseDto>> getSellersByIds(
		@Valid @RequestBody SellerBulkReadRequestDto request
	);

	@Operation(
		summary = "판매자 정산 계좌 벌크 조회",
		description = """
			판매자 ID 목록으로 정산 송금에 필요한 계좌 정보를 조회합니다.
			- 계좌가 등록된 판매자만 응답합니다.
			"""
	)
	ResponseEntity<List<SellerSettlementAccountResponseDto>> getSellerSettlementAccountsByIds(
		@Valid @RequestBody SellerBulkReadRequestDto request
	);
}
