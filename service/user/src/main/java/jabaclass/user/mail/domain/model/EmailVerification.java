package jabaclass.user.mail.domain.model;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class EmailVerification {

	private final String email;
	private final String verificationCode;
	private String verifiedToken;
	private final LocalDateTime expiresAt;

	public EmailVerification(String email, String verificationCode, String verifiedToken, LocalDateTime expiresAt) {
		this.email = email;
		this.verificationCode = verificationCode;
		this.verifiedToken = verifiedToken;
		this.expiresAt = expiresAt;
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}

	public boolean matchesCode(String inputCode) {
		return verificationCode.equals(inputCode);
	}

	public boolean hasVerifiedToken() {
		return verifiedToken != null && !verifiedToken.isBlank();
	}

	public boolean matchesVerifiedToken(String inputToken) {
		return verifiedToken != null && verifiedToken.equals(inputToken);
	}

	public void verify(String verifiedToken) {
		this.verifiedToken = verifiedToken;
	}
}