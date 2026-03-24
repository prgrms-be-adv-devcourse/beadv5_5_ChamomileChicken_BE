package jabaclass.user.auth.infrastructure.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import jabaclass.user.auth.domain.model.VerificationCode;
import jabaclass.user.auth.domain.repository.VerificationCodeRepository;

@Repository
public class InMemoryVerificationCodeRepository implements VerificationCodeRepository {

	private final Map<String, VerificationCode> store = new ConcurrentHashMap<>();

	@Override
	public void save(VerificationCode verificationCode) {
		store.put(verificationCode.email(), verificationCode);
	}

	@Override
	public Optional<VerificationCode> findByEmail(String email) {
		return Optional.ofNullable(store.get(email));
	}

	@Override
	public void deleteByEmail(String email) {
		store.remove(email);
	}
}