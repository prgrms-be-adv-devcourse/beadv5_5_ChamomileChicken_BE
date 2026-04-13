package jabaclass.order.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jabaclass.order.common.error.BusinessException;
import jabaclass.order.common.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentUserRoleArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserRole.class)
            && parameter.getParameterType().equals(String.class);
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
        String role = request.getHeader(USER_ROLE_HEADER);
        if (role == null || role.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        return role;
    }
}
