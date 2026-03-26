package jabaclass.user.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import jabaclass.user.user.domain.model.User;

public interface UserRepository {

	Optional<User> findById(UUID userId);

	boolean existsByEmail(String email);

	User save(User user);

	User saveAndFlush(User user);

	void delete(User user);
}