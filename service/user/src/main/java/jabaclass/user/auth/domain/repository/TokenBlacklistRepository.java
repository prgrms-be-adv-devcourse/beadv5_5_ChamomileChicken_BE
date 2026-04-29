package jabaclass.user.auth.domain.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jabaclass.user.auth.domain.model.TokenBlacklist;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

	boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);

	@Modifying
	@Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
	void deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}
