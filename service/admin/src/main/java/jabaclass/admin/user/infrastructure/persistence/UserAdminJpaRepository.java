package jabaclass.admin.user.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.admin.user.domain.model.User;

public interface UserAdminJpaRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	@Query("SELECT MONTH(u.createdAt), COUNT(u) FROM User u WHERE YEAR(u.createdAt) = :year " +
		"GROUP BY MONTH(u.createdAt) ORDER BY MONTH(u.createdAt)")
	List<Object[]> findMonthlyNewUserStats(@Param("year") int year);
}
