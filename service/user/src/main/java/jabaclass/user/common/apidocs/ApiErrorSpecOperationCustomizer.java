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
		if (operation == null || handlerMethod == null) {
			return operation;
		}

		ApiErrorSpecs specs = findApiErrorSpecs(handlerMethod);
		if (specs == null || specs.value() == null || specs.value().length == 0) {
			return operation;
		}

		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}

		for (ApiErrorSpec spec : specs.value()) {
			if (spec == null) {
				continue;
			}

			ErrorCode errorCode = resolveErrorCode(spec, handlerMethod);
			String statusCode = String.valueOf(errorCode.getStatus().value());

			ApiResponse apiResponse = responses.get(statusCode);
			if (apiResponse == null) {
				apiResponse = new ApiResponse();
				responses.addApiResponse(statusCode, apiResponse);
			}

			Content content = apiResponse.getContent();
			if (content == null) {
				content = new Content();
			}

			MediaType mediaType = content.get(APPLICATION_JSON);
			if (mediaType == null) {
				mediaType = new MediaType();
			}

			Example example = createExample(spec, errorCode);
			String exampleName = resolveExampleName(spec);

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

	private Example createExample(ApiErrorSpec spec, ErrorCode errorCode) {
		Example example = new Example();
		example.setValue(ApiResponseDto.fail(errorCode.getStatus(), errorCode.getMessage()));

		if (StringUtils.hasText(spec.summary())) {
			example.setSummary(spec.summary());
		}
		if (StringUtils.hasText(spec.description())) {
			example.setDescription(spec.description());
		}

		return example;
	}

	private String resolveExampleName(ApiErrorSpec spec) {
		if (StringUtils.hasText(spec.name())) {
			return spec.name();
		}
		return spec.constant();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ErrorCode resolveErrorCode(ApiErrorSpec spec, HandlerMethod handlerMethod) {
		Class<?> enumClass = spec.value();
		String constantName = spec.constant();

		if (enumClass == null) {
			throw invalidSpec(handlerMethod, "ApiErrorSpec.value() must not be null");
		}

		if (!enumClass.isEnum()) {
			throw invalidSpec(
				handlerMethod,
				"ApiErrorSpec.value() must be an enum type. actual=" + enumClass.getName()
			);
		}

		if (!StringUtils.hasText(constantName)) {
			throw invalidSpec(handlerMethod, "ApiErrorSpec.constant() must not be blank");
		}

		if (!ErrorCode.class.isAssignableFrom(enumClass)) {
			throw invalidSpec(
				handlerMethod,
				"ApiErrorSpec.value() must implement ErrorCode. actual=" + enumClass.getName()
			);
		}

		try {
			Enum constant = Enum.valueOf((Class<? extends Enum>) enumClass, constantName);
			return (ErrorCode) constant;
		} catch (IllegalArgumentException e) {
			throw invalidSpec(
				handlerMethod,
				"Invalid enum constant. enum=" + enumClass.getName() + ", constant=" + constantName,
				e
			);
		}
	}

	private IllegalStateException invalidSpec(HandlerMethod handlerMethod, String message) {
		return new IllegalStateException(buildErrorMessage(handlerMethod, message));
	}

	private IllegalStateException invalidSpec(HandlerMethod handlerMethod, String message, Exception cause) {
		return new IllegalStateException(buildErrorMessage(handlerMethod, message), cause);
	}

	private String buildErrorMessage(HandlerMethod handlerMethod, String message) {
		return "[ApiErrorSpecOperationCustomizer] " +
			handlerMethod.getBeanType().getSimpleName() +
			"#" +
			handlerMethod.getMethod().getName() +
			" - " +
			message;
	}
}