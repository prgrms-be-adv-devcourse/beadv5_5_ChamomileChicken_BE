package jabaclass.user.mail.domain.repository;

import java.util.Optional;

import jabaclass.user.mail.domain.model.EmailVerification;

public interface EmailVerificationRepository {
	void save(EmailVerification emailVerification);
	Optional<EmailVerification> findByEmail(String email);
	void deleteByEmail(String email);
}