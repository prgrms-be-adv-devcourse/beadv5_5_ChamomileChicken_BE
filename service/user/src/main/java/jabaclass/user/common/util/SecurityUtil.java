package jabaclass.user.common.util;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.exception.AuthException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public class SecurityUtil {

    private SecurityUtil() {}

    public static UUID getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UUID userId)) {
            throw new AuthException(AuthErrorCode.INVALID_AUTHENTICATION);
        }

        return userId;
    }
}