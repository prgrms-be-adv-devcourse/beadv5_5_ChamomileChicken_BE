package jabaclass.user.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.user.user.domain.model.SocialType;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import jabaclass.user.user.domain.model.User;
import jabaclass.user.user.domain.repository.UserRepository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

	private final UserJpaRepository userJpaRepository;

	@Override
	public Optional<User> findById(UUID userId) {
		return userJpaRepository.findById(userId);
	}

	@Override
	public User save(User user) {
		return userJpaRepository.save(user);
	}

	@Override
	public User saveAndFlush(User user) {
		return userJpaRepository.saveAndFlush(user);
	}

	@Override
	public void delete(User user) {
		userJpaRepository.delete(user);
	}

	@Override
	public Optional<User> findByIdWithLock(UUID userId) {
		return userJpaRepository.findByIdWithLock(userId);
	}

	@Override
	public List<User> findAllByIds(List<UUID> userIds) {
		return userJpaRepository.findAllById(userIds);
	}

	@Override
	public Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId) {
		return userJpaRepository.findBySocialTypeAndSocialId(socialType, socialId);
	}

	@Override
	public boolean existsByEmailAndSocialType(String email, SocialType socialType) {
		return userJpaRepository.existsByEmailAndSocialType(email, socialType);
	}

	@Override
	public Optional<User> findByEmailAndSocialType(String email, SocialType socialType) {
		return userJpaRepository.findByEmailAndSocialType(email, socialType);
	}
}
