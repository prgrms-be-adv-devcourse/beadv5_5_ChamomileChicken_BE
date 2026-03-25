package jabaclass.user.mail.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.exception.MailErrorCode;
import jabaclass.user.mail.domain.model.EmailVerification;
import jabaclass.user.mail.domain.repository.EmailVerificationRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

	@InjectMocks
	private EmailVerificationService emailVerificationService;

	@Mock
	private MailService mailService;

	@Mock
	private EmailVerificationRepository emailVerificationRepository;

	@Captor
	private ArgumentCaptor<EmailVerification> emailVerificationCaptor;

	@Captor
	private ArgumentCaptor<String> emailCaptor;

	@Captor
	private ArgumentCaptor<String> subjectCaptor;

	@Captor
	private ArgumentCaptor<String> bodyCaptor;

	@Captor
	private ArgumentCaptor<Boolean> htmlCaptor;

	@Test
	void 인증코드를_생성하고_저장한_후_메일을_전송한다() {
		// given
		String email = "test@example.com";

		// when
		emailVerificationService.sendVerificationCode(email);

		// then
		then(emailVerificationRepository).should().save(emailVerificationCaptor.capture());
		EmailVerification saved = emailVerificationCaptor.getValue();

		assertThat(saved.getEmail()).isEqualTo(email);
		assertThat(saved.getVerificationCode()).hasSize(6).matches("\\d{6}");
		assertThat(saved.getVerifiedToken()).isNull();
		assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

		then(mailService).should().send(
			emailCaptor.capture(),
			subjectCaptor.capture(),
			bodyCaptor.capture(),
			htmlCaptor.capture()
		);

		assertThat(emailCaptor.getValue()).isEqualTo(email);
		assertThat(subjectCaptor.getValue()).isEqualTo("[ChamomileChicken] 이메일 인증코드를 확인해주세요");
		assertThat(bodyCaptor.getValue()).contains(saved.getVerificationCode());
		assertThat(bodyCaptor.getValue()).contains("5분");
		assertThat(htmlCaptor.getValue()).isTrue();
	}

	@Test
	void 올바른_인증코드를_검증하면_verifiedToken을_발급하고_저장한다() {
		// given
		String email = "test@example.com";
		String code = "123456";
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

		EmailVerification emailVerification = new EmailVerification(
			email,
			code,
			null,
			expiresAt
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(emailVerification));

		// when
		String verifiedToken = emailVerificationService.verifyCode(email, code);

		// then
		assertThat(verifiedToken).isNotBlank();
		assertThat(emailVerification.getVerifiedToken()).isEqualTo(verifiedToken);

		then(emailVerificationRepository).should(times(1)).findByEmail(email);
		then(emailVerificationRepository).should().save(emailVerification);
		then(emailVerificationRepository).should(never()).deleteByEmail(anyString());
	}

	@Test
	void 인증코드_검증시_이메일_인증정보가_없으면_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String code = "123456";

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_NOT_FOUND.getMessage());

		then(emailVerificationRepository).should(times(1)).findByEmail(email);
		then(emailVerificationRepository).should(never()).save(any());
		then(emailVerificationRepository).should(never()).deleteByEmail(anyString());
	}

	@Test
	void 인증코드가_만료되었으면_삭제하고_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String code = "123456";

		EmailVerification expiredVerification = new EmailVerification(
			email,
			code,
			null,
			LocalDateTime.now().minusMinutes(1)
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(expiredVerification));

		// when & then
		assertThatThrownBy(() -> emailVerificationService.verifyCode(email, code))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED.getMessage());

		then(emailVerificationRepository).should().deleteByEmail(email);
		then(emailVerificationRepository).should(never()).save(any());
	}

	@Test
	void 인증코드가_일치하지_않으면_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String savedCode = "123456";
		String inputCode = "654321";

		EmailVerification emailVerification = new EmailVerification(
			email,
			savedCode,
			null,
			LocalDateTime.now().plusMinutes(5)
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(emailVerification));

		// when & then
		assertThatThrownBy(() -> emailVerificationService.verifyCode(email, inputCode))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH.getMessage());

		then(emailVerificationRepository).should(never()).save(any());
		then(emailVerificationRepository).should(never()).deleteByEmail(anyString());
	}

	@Test
	void 검증된_토큰이_유효하면_인증정보를_삭제한다() {
		// given
		String email = "test@example.com";
		String verifiedToken = "verified-token";

		EmailVerification emailVerification = new EmailVerification(
			email,
			"123456",
			verifiedToken,
			LocalDateTime.now().plusMinutes(5)
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(emailVerification));

		// when
		emailVerificationService.validateVerifiedToken(email, verifiedToken);

		// then
		then(emailVerificationRepository).should().deleteByEmail(email);
	}

	@Test
	void 검증된_토큰_조회시_인증정보가_없으면_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String verifiedToken = "verified-token";

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> emailVerificationService.validateVerifiedToken(email, verifiedToken))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_TOKEN_NOT_FOUND.getMessage());

		then(emailVerificationRepository).should(never()).deleteByEmail(anyString());
	}

	@Test
	void 검증된_토큰이_만료되었으면_삭제하고_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String verifiedToken = "verified-token";

		EmailVerification expiredVerification = new EmailVerification(
			email,
			"123456",
			verifiedToken,
			LocalDateTime.now().minusMinutes(1)
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(expiredVerification));

		// when & then
		assertThatThrownBy(() -> emailVerificationService.validateVerifiedToken(email, verifiedToken))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_TOKEN_EXPIRED.getMessage());

		then(emailVerificationRepository).should().deleteByEmail(email);
	}

	@Test
	void 검증된_토큰이_없으면_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String verifiedToken = "verified-token";

		EmailVerification emailVerification = new EmailVerification(
			email,
			"123456",
			null,
			LocalDateTime.now().plusMinutes(5)
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(emailVerification));

		// when & then
		assertThatThrownBy(() -> emailVerificationService.validateVerifiedToken(email, verifiedToken))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_TOKEN_MISMATCH.getMessage());

		then(emailVerificationRepository).should(never()).deleteByEmail(anyString());
	}

	@Test
	void 검증된_토큰이_일치하지_않으면_예외가_발생한다() {
		// given
		String email = "test@example.com";
		String savedToken = "saved-token";
		String inputToken = "wrong-token";

		EmailVerification emailVerification = new EmailVerification(
			email,
			"123456",
			savedToken,
			LocalDateTime.now().plusMinutes(5)
		);

		given(emailVerificationRepository.findByEmail(email))
			.willReturn(Optional.of(emailVerification));

		// when & then
		assertThatThrownBy(() -> emailVerificationService.validateVerifiedToken(email, inputToken))
			.isInstanceOf(BusinessException.class)
			.hasMessage(MailErrorCode.EMAIL_VERIFICATION_TOKEN_MISMATCH.getMessage());

		then(emailVerificationRepository).should(never()).deleteByEmail(anyString());
	}
}