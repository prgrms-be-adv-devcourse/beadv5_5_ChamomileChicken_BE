package jabaclass.user.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jabaclass.user.user.domain.model.User;
import jakarta.persistence.LockModeType;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

	boolean existsByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM User u WHERE u.id = :id")
	Optional<User> findByIdWithLock(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByEmail(String email);
}