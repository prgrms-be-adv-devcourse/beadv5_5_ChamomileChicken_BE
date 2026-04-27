package jabaclass.ai.application.port.external;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.domain.model.UserVector;

public interface AiGatewayPort {
	Map<UUID, String> generateRecommendationReasons(
		UserVector userVector,
		List<CandidateClassDto> candidates
	);
}
