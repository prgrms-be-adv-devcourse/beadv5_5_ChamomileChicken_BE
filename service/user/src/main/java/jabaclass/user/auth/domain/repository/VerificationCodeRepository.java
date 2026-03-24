package jabaclass.user.auth.domain.repository;

import java.util.Optional;

import jabaclass.user.auth.domain.model.VerificationCode;

public interface VerificationCodeRepository {
	void save(VerificationCode verificationCode);
	Optional<VerificationCode> findByEmail(String email);
	void deleteByEmail(String email);
}