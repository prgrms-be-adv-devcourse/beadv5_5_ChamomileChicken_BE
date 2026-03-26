package jabaclass.order.order.presentation.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jabaclass.order.common.dto.ApiResponseDto;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
    @ApiResponse(
        responseCode = "400",
        description = "요청 오류",
        content = @Content(
            schema = @Schema(implementation = ApiResponseDto.class),
            examples = @ExampleObject(
                value = """
                    {
                      "status": "BAD_REQUEST",
                      "message": "요청 오류"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content = @Content(
            schema = @Schema(implementation = ApiResponseDto.class),
            examples = @ExampleObject(
                value = """
                    {
                      "status": "UNAUTHORIZED",
                      "message": "인증 실패"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "403",
        description = "인가 실패",
        content = @Content(
            schema = @Schema(implementation = ApiResponseDto.class),
            examples = @ExampleObject(
                value = """
                    {
                      "status": "FORBIDDEN",
                      "message": "인가 실패"
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "리소스를 찾을 수 없음",
        content = @Content(
            schema = @Schema(implementation = ApiResponseDto.class),
            examples = @ExampleObject(
                value = """
                    {
                      "status": "NOT_FOUND",
                      "message": "주문을 찾을 수 없습니다."
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(
            schema = @Schema(implementation = ApiResponseDto.class),
            examples = @ExampleObject(
                value = """
                    {
                      "status": "INTERNAL_SERVER_ERROR",
                      "message": "서버 오류"
                    }
                    """
            )
        )
    )
})
public @interface CommonErrorResponses {
}
