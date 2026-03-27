package jabaclass.auth.util;

import jabaclass.auth.exception.JwtAuthException;
import jabaclass.auth.exception.JwtErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public class SecurityUtil {

    private SecurityUtil() {}

    public static UUID getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new JwtAuthException(JwtErrorCode.AUTHENTICATION_REQUIRED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UUID userId)) {
            throw new JwtAuthException(JwtErrorCode.INVALID_AUTHENTICATION);
        }

        return userId;
    }
}