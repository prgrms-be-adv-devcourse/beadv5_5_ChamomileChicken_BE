package jabaclass.order.order.presentation.openapi;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jabaclass.order.order.domain.model.OrderStatus;
import jabaclass.order.order.presentation.dto.request.CreateOrderRequestDto;
import jabaclass.order.order.presentation.dto.response.CreateOrderResponseDto;
import jabaclass.order.order.presentation.dto.response.OrderResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Order", description = "주문 API")
public interface OrderOpenApi {

    @Operation(summary = "주문 생성", description = "예치금과 상품 재고를 검증한 뒤 주문을 생성합니다.")
    @ApiResponse(
        responseCode = "201",
        description = "주문 생성 성공",
        content = @Content(schema = @Schema(implementation = CreateOrderResponseDto.class))
    )
    @CommonErrorResponses
    ResponseEntity<CreateOrderResponseDto> create(
        @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
        @RequestBody CreateOrderRequestDto requestDto
    );

    @Operation(summary = "주문 단건 조회", description = "주문 ID로 주문 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "주문 조회 성공",
        content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
    )
    @CommonErrorResponses
    ResponseEntity<OrderResponseDto> getById(@PathVariable UUID orderId);

    @Operation(summary = "내 주문 목록 조회", description = "로그인한 사용자의 주문 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "주문 목록 조회 성공",
        content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
    )
    @CommonErrorResponses
    ResponseEntity<List<OrderResponseDto>> getOrders(
        @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) OrderStatus status
    );

    @Operation(summary = "주문 취소", description = "로그인한 사용자의 주문을 취소합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "주문 취소 성공",
        content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
    )
    @CommonErrorResponses
    ResponseEntity<OrderResponseDto> cancel(
        @PathVariable UUID orderId,
        @Parameter(hidden = true) @AuthenticationPrincipal UUID userId
    );
}
