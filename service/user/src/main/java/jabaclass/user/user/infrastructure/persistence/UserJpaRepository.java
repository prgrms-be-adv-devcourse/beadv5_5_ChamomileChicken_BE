package jabaclass.user.user.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.user.user.domain.model.User;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

	boolean existsByEmail(String email);
}