package jabaclass.user.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtils {
	public static String extractIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Real-IP");
		return (ip != null) ? ip : request.getRemoteAddr();
	}
}
