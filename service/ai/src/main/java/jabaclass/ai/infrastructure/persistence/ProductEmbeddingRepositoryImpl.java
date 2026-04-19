package jabaclass.ai.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductEmbeddingRepositoryImpl implements ProductEmbeddingRepository {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public float[] findEmbeddingByProductId(UUID productId) {

		String sql = """
            SELECT embedding
            FROM products_ai
            WHERE id = ?
        """;

		return jdbcTemplate.query(
			sql,
			ps -> ps.setObject(1, productId),
			rs -> {
				if (!rs.next()) return null;

				// pgvector → 문자열 형태 "[1,2,3]"로 온다고 가정
				String vectorStr = rs.getString("embedding");

				return parseVector(vectorStr);
			}
		);
	}

	private float[] parseVector(String vectorStr) {

		if (vectorStr == null) return null;

		vectorStr = vectorStr.replace("[", "").replace("]", "");

		String[] parts = vectorStr.split(",");

		float[] result = new float[parts.length];

		for (int i = 0; i < parts.length; i++) {
			result[i] = Float.parseFloat(parts[i]);
		}

		return result;
	}
}
