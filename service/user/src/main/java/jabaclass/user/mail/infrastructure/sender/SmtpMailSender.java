package jabaclass.user.mail.infrastructure.sender;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jabaclass.user.common.error.BusinessException;
import jabaclass.user.mail.application.exception.MailErrorCode;
import jabaclass.user.mail.domain.model.MailMessage;
import jabaclass.user.mail.domain.sender.MailSender;
import jabaclass.user.mail.infrastructure.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

	private final JavaMailSender javaMailSender;
	private final MailProperties mailProperties;

	@Override
	public void send(MailMessage mailMessage) {
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

			helper.setFrom(mailProperties.from());
			helper.setTo(mailMessage.to());
			helper.setSubject(mailMessage.subject());
			helper.setText(mailMessage.body(), mailMessage.html());

			javaMailSender.send(mimeMessage);
		} catch (MessagingException | MailException e) {
			log.error("메일 전송 실패. to={}, subject={}", mailMessage.to(), mailMessage.subject(), e);
			throw new BusinessException(MailErrorCode.MAIL_SEND_FAILED);
		}
	}
}