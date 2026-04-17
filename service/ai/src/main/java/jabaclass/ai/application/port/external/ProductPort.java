package jabaclass.ai.application.port.external;

import java.util.List;

import jabaclass.ai.application.dto.CandidateClassDto;

public interface ProductPort {

	List<CandidateClassDto> getCandidates(float[] userVector);
}
