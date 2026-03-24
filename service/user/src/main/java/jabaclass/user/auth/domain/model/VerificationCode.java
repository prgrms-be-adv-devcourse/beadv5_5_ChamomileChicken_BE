package jabaclass.user.auth.domain.model;

import java.time.LocalDateTime;

public record VerificationCode(
	String email,
	String code,
	LocalDateTime expiresAt
) {
	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}

	public boolean matches(String inputCode) {
		return code.equals(inputCode);
	}
}