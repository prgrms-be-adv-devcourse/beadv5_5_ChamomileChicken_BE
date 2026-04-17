package jabaclass.admin.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.user.domain.model.User;

public interface UserAdminRepository {
	Page<User> findAll(Pageable pageable);
	Optional<User> findById(UUID userId);
}
