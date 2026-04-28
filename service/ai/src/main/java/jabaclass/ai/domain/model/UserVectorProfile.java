package jabaclass.ai.domain.model;

import java.time.LocalDateTime;

public record UserVectorProfile(
	UserVector userVector,
	LocalDateTime lastUpdatedAt,
	int version
) {
}
