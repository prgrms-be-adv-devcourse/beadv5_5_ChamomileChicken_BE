package jabaclass.ai.infrastructure.persistence;

import java.util.List;

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

	private final JdbcTemplate jdbcTemplate;

	@Override
	public List<CandidateClassDto> findTopK(UserVector userVector, int k) {

		if (userVector == null || userVector.isEmpty()) {
			return List.of();
		}

		String sql = """
            SELECT 
                id,
                title,
                description,
                price,
                road_address
            FROM product_embeddings
            WHERE status = 'ENABLE'
            ORDER BY embedding <=> (?::vector)  -- cosine distance (pgvector)
            LIMIT ?
        """;

		return jdbcTemplate.query(
			sql,
			ps -> {
				ps.setString(1, toPgVector(userVector.vector()));
				ps.setInt(2, k);
			},
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
	public List<CandidateClassDto> findPopular(int k) {

		String sql = """
            SELECT 
                id,
                title,
                description,
                price,
                road_address
            FROM product_embeddings
            WHERE status = 'ENABLE'
            ORDER BY popularity DESC
            LIMIT ?
        """;

		return jdbcTemplate.query(
			sql,
			ps -> ps.setInt(1, k),
			(rs, rowNum) -> new CandidateClassDto(
				rs.getObject("id", java.util.UUID.class),
				rs.getString("title"),
				rs.getString("description"),
				rs.getBigDecimal("price"),
				rs.getString("road_address")
			)
		);
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
