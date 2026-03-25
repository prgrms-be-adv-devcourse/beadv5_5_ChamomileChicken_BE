package jabaclass.user.mail.domain.model;

public record MailMessage(
	String to,
	String subject,
	String body,
	boolean html
) {
}