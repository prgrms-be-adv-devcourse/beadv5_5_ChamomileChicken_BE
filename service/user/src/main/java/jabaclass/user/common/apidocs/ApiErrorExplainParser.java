package jabaclass.user.common.apidocs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jabaclass.user.common.dto.ApiResponseDto;
import jabaclass.user.common.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;

public final class ApiErrorExplainParser {

	private static final String APPLICATION_JSON = "application/json";

	private ApiErrorExplainParser() {
	}

	public static void parse(Operation operation, HandlerMethod handlerMethod) {
		ApiErrorSpecs specs = findApiErrorSpecs(handlerMethod);
		if (specs == null) {
			return;
		}

		generateExceptionResponseDocs(operation, specs.value());
	}

	private static ApiErrorSpecs findApiErrorSpecs(HandlerMethod handlerMethod) {
		ApiErrorSpecs methodAnnotation = handlerMethod.getMethodAnnotation(ApiErrorSpecs.class);
		if (methodAnnotation != null) {
			return methodAnnotation;
		}
		return handlerMethod.getBeanType().getAnnotation(ApiErrorSpecs.class);
	}

	private static void generateExceptionResponseDocs(
		Operation operation,
		ApiErrorSpec[] errors
	) {
		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		Map<Integer, List<ExampleHolder>> holders = Arrays.stream(errors)
			.map(ExampleHolder::from)
			.collect(Collectors.groupingBy(ExampleHolder::httpStatus));

		addExamplesToResponses(responses, holders);
	}

	private static void addExamplesToResponses(
		ApiResponses responses,
		Map<Integer, List<ExampleHolder>> holders
	) {
		holders.forEach((httpStatus, exampleHolders) -> {
			String statusCode = String.valueOf(httpStatus);
			ApiResponse response = responses.get(statusCode);
			if (response == null) {
				response = new ApiResponse();
			}

			Content content = response.getContent();
			if (content == null) {
				content = new Content();
			}

			MediaType mediaType = content.get(APPLICATION_JSON);
			if (mediaType == null) {
				mediaType = new MediaType();
			}

			for (ExampleHolder holder : exampleHolders) {
				mediaType.addExamples(holder.name(), holder.example());
			}

			content.addMediaType(APPLICATION_JSON, mediaType);
			response.setContent(content);

			if (response.getDescription() == null || response.getDescription().isBlank()) {
				response.setDescription(exampleHolders.get(0).defaultMessage());
			}

			responses.addApiResponse(statusCode, response);
		});
	}

	@Builder(access = AccessLevel.PRIVATE)
	private record ExampleHolder(
		int httpStatus,
		String name,
		String defaultMessage,
		Example example
	) {
		static ExampleHolder from(ApiErrorSpec annotation) {
			ErrorCode errorCode = getErrorCode(annotation);

			String exampleName = StringUtils.hasText(annotation.name())
				? annotation.name()
				: annotation.constant();

			return ExampleHolder.builder()
				.httpStatus(errorCode.getStatus().value())
				.name(exampleName)
				.defaultMessage(errorCode.getMessage())
				.example(createExample(errorCode, annotation.summary(), annotation.description()))
				.build();
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private static ErrorCode getErrorCode(ApiErrorSpec annotation) {
			Class enumClass = annotation.value();
			Enum constant = Enum.valueOf(enumClass, annotation.constant());
			return (ErrorCode) constant;
		}

		private static Example createExample(ErrorCode errorCode, String summary, String description) {
			ApiResponseDto<Void> response = ApiResponseDto.fail(
				errorCode.getStatus(),
				errorCode.getMessage()
			);

			Example example = new Example();
			example.setValue(response);

			if (StringUtils.hasText(summary)) {
				example.setSummary(summary);
			}
			if (StringUtils.hasText(description)) {
				example.setDescription(description);
			}

			return example;
		}
	}
}