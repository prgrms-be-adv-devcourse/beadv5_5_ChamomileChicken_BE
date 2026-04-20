package jabaclass.user.mail.application.usecase;

public interface EmailVerificationUseCase {
	void sendVerificationCode(String email);
	String verifyCode(String email, String code);
	void validateVerifiedToken(String email, String verifiedToken);
}