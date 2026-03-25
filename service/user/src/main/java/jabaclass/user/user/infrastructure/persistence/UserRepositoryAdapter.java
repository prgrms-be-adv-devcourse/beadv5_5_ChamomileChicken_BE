package jabaclass.user.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

	private final UserJpaRepository userJpaRepository;

	@Override
	public Optional<User> findById(UUID userId) {
		return userJpaRepository.findById(userId);
	}

	@Override
	public boolean existsByEmail(String email) {
		return userJpaRepository.existsByEmail(email);
	}

	@Override
	public Optional<User> findByIdWithLock(UUID userId) {
		return userJpaRepository.findByIdWithLock(userId);
	}
}