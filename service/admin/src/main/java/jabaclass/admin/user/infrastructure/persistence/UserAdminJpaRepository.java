package jabaclass.admin.user.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jabaclass.admin.user.domain.model.User;

public interface UserAdminJpaRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
}
