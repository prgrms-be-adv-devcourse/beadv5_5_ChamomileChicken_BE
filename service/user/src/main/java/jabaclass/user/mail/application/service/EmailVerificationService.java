package jabaclass.user.mail.application.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.exception.MailErrorCode;
import jabaclass.user.mail.application.usecase.EmailVerificationUseCase;
import jabaclass.user.mail.domain.model.EmailVerification;
import jabaclass.user.mail.domain.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService implements EmailVerificationUseCase {

	private static final long EXPIRE_MINUTES = 5L;
	private static final int VERIFICATION_CODE_BOUND = 1_000_000;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final MailService mailService;
	private final EmailVerificationRepository emailVerificationRepository;

	@Override
	public void sendVerificationCode(String email) {
		String code = generateCode();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);

		EmailVerification emailVerification = new EmailVerification(
			email,
			code,
			null,
			expiresAt
		);

		emailVerificationRepository.save(emailVerification);

		String subject = "[ChamomileChicken] 이메일 인증코드를 확인해주세요";
		String body = createVerificationEmailBody(code);

		mailService.send(email, subject, body, true);
	}

	@Override
	public String verifyCode(String email, String code) {
		EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(MailErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

		if (emailVerification.isExpired(LocalDateTime.now())) {
			emailVerificationRepository.deleteByEmail(email);
			throw new BusinessException(MailErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
		}

		if (!emailVerification.matchesCode(code)) {
			throw new BusinessException(MailErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
		}

		String verifiedToken = UUID.randomUUID().toString();
		emailVerification.verify(verifiedToken);
		emailVerificationRepository.save(emailVerification);

		return verifiedToken;
	}

	@Override
	public void validateVerifiedToken(String email, String verifiedToken) {
		EmailVerification emailVerification = emailVerificationRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(MailErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND));

		if (emailVerification.isExpired(LocalDateTime.now())) {
			emailVerificationRepository.deleteByEmail(email);
			throw new BusinessException(MailErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED);
		}

		if (!emailVerification.hasVerifiedToken() || !emailVerification.matchesVerifiedToken(verifiedToken)) {
			throw new BusinessException(MailErrorCode.EMAIL_VERIFICATION_TOKEN_MISMATCH);
		}

		emailVerificationRepository.deleteByEmail(email);
	}

	private String generateCode() {
		int number = RANDOM.nextInt(VERIFICATION_CODE_BOUND);
		return String.format("%06d", number);
	}

	private String createVerificationEmailBody(String code) {
		return """
			<div style="margin:0;padding:0;background-color:#f3f6fb;">
			  <div style="width:100%%;background-color:#f3f6fb;padding:40px 16px;">
			    <div style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,0.08);">
			
			      <!-- Header -->
			      <div style="background:linear-gradient(135deg,#111827 0%%,#1f2937 100%%);padding:36px 32px 28px;text-align:left;">
			        <div style="display:inline-block;padding:6px 12px;background:rgba(255,255,255,0.12);border-radius:999px;font-size:12px;font-weight:700;letter-spacing:0.4px;color:#f9fafb;margin-bottom:16px;">
			          EMAIL VERIFICATION
			        </div>
			        <h1 style="margin:0;font-size:30px;line-height:1.3;font-weight:800;color:#ffffff;">
			          이메일 인증
			        </h1>
			        <p style="margin:12px 0 0;font-size:15px;line-height:1.7;color:#d1d5db;">
			          회원가입을 안전하게 완료할 수 있도록 인증코드를 보내드렸어요.
			        </p>
			      </div>
			
			      <!-- Body -->
			      <div style="padding:36px 32px 32px;">
			        <p style="margin:0 0 14px;font-size:16px;line-height:1.7;color:#374151;">
			          안녕하세요, <strong style="color:#111827;">잡아 클래스</strong>입니다.
			        </p>
			        <p style="margin:0 0 28px;font-size:15px;line-height:1.7;color:#4b5563;">
			          아래 인증코드를 입력하면 이메일 인증이 완료됩니다.
			        </p>
			
			        <!-- Code Card -->
			        <div style="margin:0 0 28px;padding:28px 20px;background:linear-gradient(180deg,#f9fbff 0%%,#f3f6fb 100%%);border:1px solid #dbe3f0;border-radius:18px;text-align:center;">
			          <div style="font-size:13px;font-weight:700;letter-spacing:0.2px;color:#6b7280;margin-bottom:12px;">
			            인증코드
			          </div>
			          <div style="display:inline-block;padding:14px 22px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;box-shadow:inset 0 1px 0 rgba(255,255,255,0.8);font-size:40px;line-height:1;font-weight:800;letter-spacing:10px;color:#111827;">
			            %s
			          </div>
			        </div>
			
			        <!-- Info Box -->
			        <div style="margin:0 0 24px;padding:18px 20px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:14px;">
			          <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#374151;">
			            이 코드는 <strong>%d분</strong> 동안 유효합니다.
			          </p>
			          <p style="margin:0;font-size:14px;line-height:1.6;color:#6b7280;">
			            본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
			          </p>
			        </div>
			
			        <p style="margin:0;font-size:13px;line-height:1.7;color:#9ca3af;">
			          보안을 위해 인증코드는 타인과 공유하지 마세요.
			        </p>
			      </div>
			
			      <!-- Footer -->
			      <div style="padding:20px 32px;background:#fafafa;border-top:1px solid #e5e7eb;">
			        <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;">
			          © Jaba Class. All rights reserved.
			        </p>
			      </div>
			
			    </div>
			  </div>
			</div>
			""".formatted(code, EXPIRE_MINUTES);
	}
}