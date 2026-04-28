package jabaclass.ai.domain.model;

import java.util.Set;
import java.util.UUID;

public record UserPreferenceState(
	UserVector userVector,
	Set<UUID> excludedProductIds
) {
}
