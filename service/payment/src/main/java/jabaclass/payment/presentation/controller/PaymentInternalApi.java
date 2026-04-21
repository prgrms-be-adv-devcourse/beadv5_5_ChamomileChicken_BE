package jabaclass.payment.presentation.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.payment.presentation.dto.response.PaymentSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.RefundSettlementSliceResponseDto;
import jabaclass.payment.presentation.dto.response.InternalRefundDetailResponseDto;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Payment Internal", description = "결제 내부 연동 API")
public interface PaymentInternalApi {

	@Operation(
		summary = "주문별 환불 정보 조회",
		description = "내부 서비스가 주문 ID 기준으로 환불 비율과 환불 처리 시각을 조회합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "환불 정보 조회 성공",
		content = @Content(schema = @Schema(implementation = InternalRefundDetailResponseDto.class))
	)
	InternalRefundDetailResponseDto getRefundInfoByOrder(@PathVariable UUID orderId);

	@Operation(
		summary = "결제 정산 원천 조회",
		description = """
			정산 모듈이 결제 완료 원천 데이터를 cursor 기반으로 조회합니다.
			- 결제 완료 시각 기준으로 정산 대상이 됩니다.
			- cursorUpdatedAt, cursorId로 증분 조회합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "결제 정산 원천 조회 성공",
		content = @Content(schema = @Schema(implementation = PaymentSettlementSliceResponseDto.class))
	)
	PaymentSettlementSliceResponseDto getPaymentSettlementTargets(
		@Parameter(description = "조회 시작 시각", example = "2026-03-01T00:00:00")
		@RequestParam LocalDateTime from,
		@Parameter(description = "조회 종료 시각", example = "2026-03-31T23:59:59")
		@RequestParam LocalDateTime to,
		@Parameter(description = "다음 페이지 조회용 cursor 시간", example = "2026-03-10T12:30:00")
		@RequestParam(required = false) LocalDateTime cursorUpdatedAt,
		@Parameter(description = "다음 페이지 조회용 cursor ID")
		@RequestParam(required = false) UUID cursorId,
		@Parameter(description = "페이지 크기", example = "1000")
		@RequestParam(defaultValue = "1000") int size
	);

	@Operation(
		summary = "환불 정산 원천 조회",
		description = """
			정산 모듈이 환불 완료 원천 데이터를 cursor 기반으로 조회합니다.
			- 환불 완료 시각 기준으로 정산 차감 대상이 됩니다.
			- cursorUpdatedAt, cursorId로 증분 조회합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "환불 정산 원천 조회 성공",
		content = @Content(schema = @Schema(implementation = RefundSettlementSliceResponseDto.class))
	)
	RefundSettlementSliceResponseDto getRefundSettlementTargets(
		@Parameter(description = "조회 시작 시각", example = "2026-03-01T00:00:00")
		@RequestParam LocalDateTime from,
		@Parameter(description = "조회 종료 시각", example = "2026-03-31T23:59:59")
		@RequestParam LocalDateTime to,
		@Parameter(description = "다음 페이지 조회용 cursor 시간", example = "2026-03-10T12:30:00")
		@RequestParam(required = false) LocalDateTime cursorUpdatedAt,
		@Parameter(description = "다음 페이지 조회용 cursor ID")
		@RequestParam(required = false) UUID cursorId,
		@Parameter(description = "페이지 크기", example = "1000")
		@RequestParam(defaultValue = "1000") int size
	);
}
