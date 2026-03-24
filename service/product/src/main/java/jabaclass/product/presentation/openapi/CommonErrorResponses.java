package jabaclass.product.presentation.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jabaclass.product.common.exception.ApiResponseDto;

// 이 어노테이션을 어디에 붙일 수 있을까 -> 컨트롤러 메서드에만 붙이기 가능
@Target(ElementType.METHOD)
// 실행 중에도 살아있게 하는 설정
@Retention(RetentionPolicy.RUNTIME)
// 에러들
@ApiResponses({
	@ApiResponse(
		responseCode = "400",
		description = "요청 오류",
		content = @Content(
			schema = @Schema(implementation = ApiResponseDto.class),
			examples = @ExampleObject(
				value = """
					{
					  "status": "400 BAD_REQUEST",
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
					  "status": "401 UNAUTHORIZED",
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
					  "status": "403 FORBIDDEN",
					  "message": "인가 실패"
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
					  "status": "500 INTERNAL_SERVER_ERROR",
					  "message": "서버 오류"
					}
					"""
			)
		)
	)
})
public @interface CommonErrorResponses {
}
