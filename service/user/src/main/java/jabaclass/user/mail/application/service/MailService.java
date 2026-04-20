package jabaclass.user.mail.application.service;

import org.springframework.stereotype.Service;

import jabaclass.user.mail.domain.model.MailMessage;
import jabaclass.user.mail.domain.sender.MailSender;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

	private final MailSender mailSender;

	public void send(String to, String subject, String body, boolean html) {
		mailSender.send(new MailMessage(to, subject, body, html));
	}
}