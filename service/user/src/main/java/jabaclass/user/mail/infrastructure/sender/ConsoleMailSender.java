package jabaclass.user.mail.infrastructure.sender;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jabaclass.user.mail.domain.model.MailMessage;
import jabaclass.user.mail.domain.sender.MailSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"local", "test"})
public class ConsoleMailSender implements MailSender {

	@Override
	public void send(MailMessage mailMessage) {
		log.info("[MAIL SEND] to={}, subject={}", mailMessage.to(), mailMessage.subject());
	}
}