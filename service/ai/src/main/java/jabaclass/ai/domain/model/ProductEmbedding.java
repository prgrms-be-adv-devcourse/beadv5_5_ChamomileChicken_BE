package jabaclass.ai.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "product_embeddings")
public class ProductEmbedding {

	@Id
	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "embedding", nullable = false, columnDefinition = "vector(768)")
	private String embedding;

	public ProductEmbedding(UUID productId, String embedding) {
		this.productId = productId;
		this.embedding = embedding;
	}

	public static ProductEmbedding create(UUID productId, String embedding) {
		return new ProductEmbedding(productId, embedding);
	}

	public void changeEmbedding(String embedding) {
		this.embedding = embedding;
	}
}
