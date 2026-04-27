package jabaclass.ai.domain.model;

import java.math.BigDecimal;
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
	private UUID id;

	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	private BigDecimal price;

	@Column(name = "road_address")
	private String roadAddress;

	private String status;

	private Integer popularity;

	@Column(columnDefinition = "vector(768)", nullable = false)
	private String embedding;
}
