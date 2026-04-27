package jabaclass.admin.user.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

	@Override
	public Map<UUID, String> findEmailsByIds(Collection<UUID> userIds) {
		return userAdminJpaRepository.findAllById(userIds)
			.stream()
			.collect(Collectors.toMap(User::getId, User::getEmail));
	}

	@Override
	public long countAll() {
		return userAdminJpaRepository.count();
	}

	@Override
	public List<Object[]> findMonthlyNewUserStats(int year) {
		return userAdminJpaRepository.findMonthlyNewUserStats(year);
	}
}
