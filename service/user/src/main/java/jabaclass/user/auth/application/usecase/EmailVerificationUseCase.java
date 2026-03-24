package jabaclass.user.auth.application.usecase;

public interface EmailVerificationUseCase {
	void checkEmailDuplicate(String email);
	void sendVerificationCode(String email);
	void verifyCode(String email, String code);
}