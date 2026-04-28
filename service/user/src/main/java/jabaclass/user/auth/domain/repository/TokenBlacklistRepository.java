package jabaclass.user.auth.domain.repository;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository {

	boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
}
