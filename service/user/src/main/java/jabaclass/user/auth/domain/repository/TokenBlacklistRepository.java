package jabaclass.user.auth.domain.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.user.auth.domain.model.TokenBlacklist;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

	boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
}
