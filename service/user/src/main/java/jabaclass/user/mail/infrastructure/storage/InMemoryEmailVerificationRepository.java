package jabaclass.user.mail.infrastructure.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import jabaclass.user.mail.domain.model.EmailVerification;
import jabaclass.user.mail.domain.repository.EmailVerificationRepository;

@Repository
public class InMemoryEmailVerificationRepository implements EmailVerificationRepository {

	private final Map<String, EmailVerification> store = new ConcurrentHashMap<>();

	@Override
	public void save(EmailVerification emailVerification) {
		store.put(emailVerification.getEmail(), emailVerification);
	}

	@Override
	public Optional<EmailVerification> findByEmail(String email) {
		return Optional.ofNullable(store.get(email));
	}

	@Override
	public void deleteByEmail(String email) {
		store.remove(email);
	}
}