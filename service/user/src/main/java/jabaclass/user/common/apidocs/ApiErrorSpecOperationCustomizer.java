package jabaclass.user.common.apidocs;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
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

@Component
public class ApiErrorSpecOperationCustomizer implements OperationCustomizer {

	private static final String APPLICATION_JSON = "application/json";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		ApiErrorSpecs specs = findApiErrorSpecs(handlerMethod);
		if (specs == null) {
			return operation;
		}

		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		for (ApiErrorSpec spec : specs.value()) {
			ErrorCode errorCode = getErrorCode(spec);

			String statusCode = String.valueOf(errorCode.getStatus().value());
			ApiResponse apiResponse = responses.computeIfAbsent(statusCode, key -> new ApiResponse());

			Content content = apiResponse.getContent();
			if (content == null) {
				content = new Content();
			}

			MediaType mediaType = content.getOrDefault(APPLICATION_JSON, new MediaType());

			Example example = new Example();
			example.setValue(ApiResponseDto.fail(errorCode.getStatus(), errorCode.getMessage()));

			if (StringUtils.hasText(spec.summary())) {
				example.setSummary(spec.summary());
			}
			if (StringUtils.hasText(spec.description())) {
				example.setDescription(spec.description());
			}

			String exampleName = StringUtils.hasText(spec.name())
				? spec.name()
				: spec.constant();

			mediaType.addExamples(exampleName, example);

			content.addMediaType(APPLICATION_JSON, mediaType);
			apiResponse.setContent(content);

			if (!StringUtils.hasText(apiResponse.getDescription())) {
				apiResponse.setDescription(errorCode.getMessage());
			}
		}

		return operation;
	}

	private ApiErrorSpecs findApiErrorSpecs(HandlerMethod handlerMethod) {
		ApiErrorSpecs methodAnnotation = handlerMethod.getMethodAnnotation(ApiErrorSpecs.class);
		if (methodAnnotation != null) {
			return methodAnnotation;
		}
		return handlerMethod.getBeanType().getAnnotation(ApiErrorSpecs.class);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private ErrorCode getErrorCode(ApiErrorSpec spec) {
		Class enumClass = spec.value();
		Enum constant = Enum.valueOf(enumClass, spec.constant());
		return (ErrorCode) constant;
	}
}