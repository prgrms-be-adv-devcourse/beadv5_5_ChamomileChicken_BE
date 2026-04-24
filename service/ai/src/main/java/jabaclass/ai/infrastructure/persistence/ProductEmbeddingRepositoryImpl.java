package jabaclass.ai.infrastructure.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jabaclass.ai.domain.repository.ProductEmbeddingRepository;
import jabaclass.ai.infrastructure.persistence.command.ProductEmbeddingUpsertCommand;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductEmbeddingRepositoryImpl implements ProductEmbeddingRepository {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public Map<UUID, float[]> findAllByProductIds(Collection<UUID> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Collections.emptyMap();
		}

		String placeholders = String.join(",", Collections.nCopies(productIds.size(), "?"));
		String sql = """
			SELECT id, embedding
			FROM product_embeddings
			WHERE id IN (%s)
		""".formatted(placeholders);

		return jdbcTemplate.query(
			sql,
			ps -> {
				int index = 1;
				for (UUID productId : productIds) {
					ps.setObject(index++, productId);
				}
			},
			rs -> {
				Map<UUID, float[]> embeddings = new HashMap<>();
				while (rs.next()) {
					embeddings.put(
						rs.getObject("id", UUID.class),
						parseVector(rs.getString("embedding"))
					);
				}
				return embeddings;
			}
		);
	}

	@Override
	public void upsert(ProductEmbeddingUpsertCommand command) {
		String sql = """
			INSERT INTO product_embeddings (
				id,
				title,
				description,
				price,
				road_address,
				status,
				popularity,
				embedding
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?::vector)
			ON CONFLICT (id) DO UPDATE SET
				title = EXCLUDED.title,
				description = EXCLUDED.description,
				price = EXCLUDED.price,
				road_address = EXCLUDED.road_address,
				status = EXCLUDED.status,
				popularity = EXCLUDED.popularity,
				embedding = EXCLUDED.embedding
		""";

		jdbcTemplate.update(
			sql,
			ps -> {
				ps.setObject(1, command.productId());
				ps.setString(2, command.title());
				ps.setString(3, command.description());
				ps.setBigDecimal(4, command.price());
				ps.setString(5, command.roadAddress());
				ps.setString(6, command.status());
				ps.setObject(7, command.popularity());
				ps.setString(8, toPgVector(command.embedding()));
			}
		);
	}

	@Override
	public void deleteByProductId(UUID productId) {
		jdbcTemplate.update(
			"DELETE FROM product_embeddings WHERE id = ?",
			ps -> ps.setObject(1, productId)
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
