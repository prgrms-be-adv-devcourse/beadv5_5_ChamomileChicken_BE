package jabaclass.user.auth.application.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import jabaclass.user.auth.application.exception.AuthErrorCode;
import jabaclass.user.auth.application.usecase.EmailVerificationUseCase;
import jabaclass.user.auth.domain.model.VerificationCode;
import jabaclass.user.auth.domain.repository.VerificationCodeRepository;
import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.service.MailService;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService implements EmailVerificationUseCase {

	private static final long EXPIRE_MINUTES = 5L;
	private static final int CODE_BOUND = 1_000_000;

	private final MailService mailService;
	private final VerificationCodeRepository verificationCodeRepository;
	private final UserRepository userRepository;

	@Override
	public void checkEmailDuplicate(String email) {
		validateDuplicatedEmail(email);
	}

	@Override
	public void sendVerificationCode(String email) {
		validateDuplicatedEmail(email);

		String code = generateCode();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);

		verificationCodeRepository.save(new VerificationCode(email, code, expiresAt));

		String subject = "[ChamomileChicken] 이메일 인증코드를 확인해주세요";
		String body = createVerificationEmailBody(code);

		mailService.send(email, subject, body, true);
	}

	@Override
	public void verifyCode(String email, String code) {
		VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND));

		if (verificationCode.isExpired(LocalDateTime.now())) {
			verificationCodeRepository.deleteByEmail(email);
			throw new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
		}

		if (!verificationCode.matches(code)) {
			throw new BusinessException(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
		}

		verificationCodeRepository.deleteByEmail(email);
	}

	private void validateDuplicatedEmail(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}
	}

	private String generateCode() {
		SecureRandom random = new SecureRandom();
		int number = random.nextInt(CODE_BOUND);
		return String.format("%06d", number);
	}

	private String createVerificationEmailBody(String code) {
		return """
				<div style="background-color:#f5f7fb;padding:40px 20px;font-family:Arial,sans-serif;">
				  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:40px 32px;box-shadow:0 4px 20px rgba(0,0,0,0.08);">
				
				    <h1 style="margin:0 0 8px;font-size:24px;color:#111827;">이메일 인증</h1>
				    <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#4b5563;">
				      안녕하세요, <strong>ChamomileChicken</strong>입니다.<br>
				      이메일 인증을 완료하려면 아래 인증코드를 입력해주세요.
				    </p>
				
				    <div style="margin:24px 0;padding:20px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:12px;text-align:center;">
				      <div style="font-size:13px;color:#6b7280;margin-bottom:8px;">인증코드</div>
				      <div style="font-size:32px;font-weight:700;letter-spacing:8px;color:#111827;">%s</div>
				    </div>
				
				    <p style="margin:24px 0 8px;font-size:14px;color:#374151;">
				      이 코드는 <strong>%d분</strong> 동안 유효합니다.
				    </p>
				    <p style="margin:0;font-size:14px;line-height:1.6;color:#6b7280;">
				      본인이 요청하지 않은 경우 이 메일은 무시하셔도 됩니다.
				    </p>
				
				    <hr style="border:none;border-top:1px solid #e5e7eb;margin:32px 0;">
				
				    <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;">
				      © ChamomileChicken. All rights reserved.
				    </p>
				  </div>
				</div>
				""".formatted(code, EXPIRE_MINUTES);
	}
}