package jabaclass.user.mail.domain.sender;

import jabaclass.user.mail.domain.model.MailMessage;

public interface MailSender {
	void send(MailMessage mailMessage);
}