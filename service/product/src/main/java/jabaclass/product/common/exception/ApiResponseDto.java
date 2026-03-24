package jabaclass.product.common.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponseDto<T> {

	@Schema(description = "응답 코드", example = "200 OK")
	private HttpStatus status;
	@Schema(description = "메세지", example = "작업이 완료 되었습니다.")
	private String message;
	// Response되는 파일이 달라질 수도 있음.
	private T data;

	private ApiResponseDto(HttpStatus status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public ApiResponseDto() {
	}

	public static <T> ApiResponseDto<T> success(HttpStatus status, String message, T data) {
		return new ApiResponseDto<>(status, message, data);
	}

	public static ApiResponseDto<Void> fail(HttpStatus status, String message) {
		return new ApiResponseDto<>(status, message, null);
	}

}
