package jabaclass.admin.common.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jabaclass.admin.common.error.AdminErrorCode;
import jabaclass.admin.common.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

	private static final String USER_ROLE_HEADER = "X-User-Role";
	private static final String ADMIN_ROLE = "ADMIN";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String role = request.getHeader(USER_ROLE_HEADER);
		if (!ADMIN_ROLE.equals(role)) {
			throw new BusinessException(AdminErrorCode.FORBIDDEN);
		}
		return true;
	}
}
