package jabaclass.ai.infrastructure.persistence.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductEmbeddingUpsertCommand(
	UUID productId,
	String title,
	String description,
	BigDecimal price,
	String roadAddress,
	String status,
	Integer popularity,
	float[] embedding
) {
}
