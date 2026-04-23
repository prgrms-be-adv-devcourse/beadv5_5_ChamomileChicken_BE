package jabaclass.admin.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jabaclass.admin.user.domain.dto.UserSearchCondition;
import jabaclass.admin.user.domain.model.User;

public interface UserAdminRepository {
	Page<User> findAll(UserSearchCondition condition, Pageable pageable);
	Optional<User> findById(UUID userId);
}
