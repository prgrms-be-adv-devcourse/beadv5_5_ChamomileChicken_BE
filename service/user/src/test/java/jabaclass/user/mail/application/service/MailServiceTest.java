package jabaclass.user.mail.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jabaclass.user.mail.domain.model.MailMessage;
import jabaclass.user.mail.domain.sender.MailSender;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

	@InjectMocks
	private MailService mailService;

	@Mock
	private MailSender mailSender;

	@Captor
	private ArgumentCaptor<MailMessage> mailMessageCaptor;

	@Test
	void 메일을_전송한다() {
		// given
		String to = "test@example.com";
		String subject = "인증 메일입니다";
		String body = "<h1>인증코드 123456</h1>";
		boolean html = true;

		// when
		mailService.send(to, subject, body, html);

		// then
		then(mailSender).should().send(mailMessageCaptor.capture());

		MailMessage captured = mailMessageCaptor.getValue();
		assertThat(captured.to()).isEqualTo(to);
		assertThat(captured.subject()).isEqualTo(subject);
		assertThat(captured.body()).isEqualTo(body);
		assertThat(captured.html()).isEqualTo(html);
	}

	@Test
	void html이_false인_텍스트_메일을_전송한다() {
		// given
		String to = "plain@example.com";
		String subject = "텍스트 메일";
		String body = "본문입니다";
		boolean html = false;

		// when
		mailService.send(to, subject, body, html);

		// then
		then(mailSender).should().send(mailMessageCaptor.capture());

		MailMessage captured = mailMessageCaptor.getValue();
		assertThat(captured.to()).isEqualTo(to);
		assertThat(captured.subject()).isEqualTo(subject);
		assertThat(captured.body()).isEqualTo(body);
		assertThat(captured.html()).isFalse();
	}
}