package jabaclass.product.common.auth;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jabaclass.product.application.exception.AuthErrorCode;
import jabaclass.product.application.exception.AuthException;
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
	public Object resolveArgument(MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory) {

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new AuthException(AuthErrorCode.INVALID_REQUEST);
		}
		String userId = request.getHeader(USER_ID_HEADER);
		if (userId == null || userId.isBlank()) {
			CurrentUser currentUser = parameter.getParameterAnnotation(CurrentUser.class);
			if (currentUser != null && !currentUser.required()) {
				return null;
			}
			throw new AuthException(AuthErrorCode.INVALID_REQUEST);
		}
		return UUID.fromString(userId);
	}
}
