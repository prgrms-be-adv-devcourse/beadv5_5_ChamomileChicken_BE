package jabaclass.payment.common.auth;


import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jabaclass.payment.common.error.BusinessException;
import jabaclass.payment.common.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String USER_ID_HEADER = "X-User-Id";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class)
			&& parameter.getParameterType().equals(UUID.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
		}
		String userId = request.getHeader(USER_ID_HEADER);
		if (userId == null || userId.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
		}
		return UUID.fromString(userId);
	}
}

