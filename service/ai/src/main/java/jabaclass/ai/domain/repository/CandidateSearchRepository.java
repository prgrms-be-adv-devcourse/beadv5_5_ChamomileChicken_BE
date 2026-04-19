package jabaclass.ai.domain.repository;

import java.util.List;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.domain.model.UserVector;

public interface CandidateSearchRepository {

	List<CandidateClassDto> findTopK(UserVector userVector, int k); // 사용자 벡터 기반으로 유사한 상품 Top-K 찾기

	List<CandidateClassDto> findPopular(int k); // fallback용
}
