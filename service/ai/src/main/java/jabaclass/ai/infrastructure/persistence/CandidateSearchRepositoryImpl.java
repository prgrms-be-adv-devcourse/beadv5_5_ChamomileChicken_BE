package jabaclass.ai.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jabaclass.ai.application.dto.CandidateClassDto;
import jabaclass.ai.domain.model.UserVector;
import jabaclass.ai.domain.repository.CandidateSearchRepository;
import lombok.RequiredArgsConstructor;

// 추천 대상이 될 "클래스 목록"을 가져옴
// UserVector → DB(pgvector) → 유사한 상품 Top-K 조회
@Repository
@RequiredArgsConstructor
public class CandidateSearchRepositoryImpl implements CandidateSearchRepository {

	private static final double MAX_COSINE_DISTANCE = 0.35;

	private final JdbcTemplate jdbcTemplate;

	@Override
	public List<CandidateClassDto> findTopK(UserVector userVector, Set<UUID> excludedProductIds, int k) {

		if (userVector == null || userVector.isEmpty()) {
			return List.of();
		}

		String vector = toPgVector(userVector.vector());
		String exclusionClause = buildExclusionClause(excludedProductIds);

		String sql = """
            SELECT 
                id,
                title,
                description,
                price,
                road_address
            FROM product_embeddings
            WHERE status = 'ENABLE'
              AND embedding <=> (?::vector) <= ?
        """ + exclusionClause + """
            ORDER BY embedding <=> (?::vector)  -- cosine distance (pgvector)
            LIMIT ?
        """;

		List<Object> params = new ArrayList<>();
		params.add(vector);
		params.add(MAX_COSINE_DISTANCE);
		addExcludedProductIds(params, excludedProductIds);
		params.add(vector);
		params.add(k);

		return jdbcTemplate.query(
			sql,
			params.toArray(),
			(rs, rowNum) -> new CandidateClassDto(
				rs.getObject("id", java.util.UUID.class),
				rs.getString("title"),
				rs.getString("description"),
				rs.getBigDecimal("price"),
				rs.getString("road_address")
			)
		);
	}

	@Override
	public List<CandidateClassDto> findPopular(Set<UUID> excludedProductIds, int k) {
		String exclusionClause = buildExclusionClause(excludedProductIds);

		String sql = """
            SELECT 
                id,
                title,
                description,
                price,
                road_address
            FROM product_embeddings
            WHERE status = 'ENABLE'
              AND COALESCE(popularity, 0) > 0
        """ + exclusionClause + """
            ORDER BY popularity DESC
            LIMIT ?
        """;

		List<Object> params = new ArrayList<>();
		addExcludedProductIds(params, excludedProductIds);
		params.add(k);

		return jdbcTemplate.query(
			sql,
			params.toArray(),
			(rs, rowNum) -> new CandidateClassDto(
				rs.getObject("id", java.util.UUID.class),
				rs.getString("title"),
				rs.getString("description"),
				rs.getBigDecimal("price"),
				rs.getString("road_address")
			)
		);
	}

	private String buildExclusionClause(Set<UUID> excludedProductIds) {
		if (excludedProductIds == null || excludedProductIds.isEmpty()) {
			return "";
		}

		return "\n              AND id NOT IN (" + "?,".repeat(excludedProductIds.size() - 1) + "?" + ")";
	}

	private void addExcludedProductIds(List<Object> params, Set<UUID> excludedProductIds) {
		if (excludedProductIds == null || excludedProductIds.isEmpty()) {
			return;
		}

		params.addAll(excludedProductIds);
	}

	private String toPgVector(float[] vector) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < vector.length; i++) {
			sb.append(vector[i]);
			if (i < vector.length - 1) sb.append(",");
		}
		sb.append("]");
		return sb.toString();
	}
}
