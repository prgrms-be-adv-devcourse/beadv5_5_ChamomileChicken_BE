package jabaclass.admin.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jabaclass.admin.user.domain.dto.UserSearchCondition;
import jabaclass.admin.user.domain.model.User;
import jabaclass.admin.user.domain.repository.UserAdminRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserAdminRepositoryAdapter implements UserAdminRepository {

	private final UserAdminJpaRepository userAdminJpaRepository;

	@Override
	public Page<User> findAll(UserSearchCondition condition, Pageable pageable) {
		return userAdminJpaRepository.findAll(UserAdminSpecification.withCondition(condition), pageable);
	}

	@Override
	public Optional<User> findById(UUID userId) {
		return userAdminJpaRepository.findById(userId);
	}
}
