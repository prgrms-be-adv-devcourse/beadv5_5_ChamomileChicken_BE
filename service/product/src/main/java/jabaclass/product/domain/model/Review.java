package jabaclass.product.domain.model;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@Table(name = "product_reviews", schema = "public")
public class Review extends EntityBase {

	@Column(name = "product_id", updatable = false, nullable = false)
	private UUID productId;

	@Column(name = "user_id", updatable = false, nullable = false)
	private UUID userId;

	@Column(nullable = false)
	int rating;

	@Column(nullable = false)
	String content;

	public void changeRating(int rating) {
		this.rating = rating;
	}

	public void changeContent(String content) {
		this.content = content;
	}

}
