package jabaclass.order.order.presentation.openapi;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.order.order.presentation.dto.request.UpdateOrderPaymentStatusRequestDto;
import jabaclass.order.order.presentation.dto.response.ValidatePaymentAmountResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Order Internal", description = "주문 내부 연동 API")
public interface OrderInternalOpenApi {

    @Operation(summary = "결제 금액 검증", description = "Payment 모듈이 주문 총 금액과 결제 요청 금액의 일치 여부를 검증합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "결제 금액 검증 성공",
        content = @Content(schema = @Schema(implementation = ValidatePaymentAmountResponseDto.class))
    )
    @CommonErrorResponses
    ResponseEntity<ValidatePaymentAmountResponseDto> validatePaymentAmount(
        @PathVariable UUID orderId,
        @RequestParam BigDecimal amount
    );

    @Operation(summary = "결제 상태 반영", description = "Payment 모듈이 결제 결과를 Order 모듈에 반영합니다.")
    @ApiResponse(responseCode = "204", description = "결제 상태 반영 성공")
    @CommonErrorResponses
    ResponseEntity<Void> updatePaymentStatus(
        @PathVariable UUID orderId,
        @RequestBody UpdateOrderPaymentStatusRequestDto requestDto
    );
}
