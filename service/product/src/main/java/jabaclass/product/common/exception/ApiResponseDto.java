package jabaclass.product.common.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponseDto<T> {
	private HttpStatus status;
	private String message;
	private final T data;

	private ApiResponseDto(HttpStatus status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public static <T> ApiResponseDto<T> success(HttpStatus status, String message, T data) {
		return new ApiResponseDto<>(status, message, data);
	}

	public static ApiResponseDto<Void> fail(HttpStatus status, String message) {
		return new ApiResponseDto<>(status, message, null);
	}

}
