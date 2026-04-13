package jabaclass.user.common.auth;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserRoleArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String USER_ROLE_HEADER = "X-User-Role";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUserRole.class)
			&& parameter.getParameterType().equals(String.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory) {

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (request == null) {
			throw new IllegalStateException("HttpServletRequest를 가져올 수 없습니다.");
		}

		String role = request.getHeader(USER_ROLE_HEADER);

		if (role == null || role.isBlank()) {
			throw new IllegalArgumentException("X-User-Role 헤더가 없습니다. Gateway를 통한 요청인지 확인하세요.");
		}

		return role;
	}
}
