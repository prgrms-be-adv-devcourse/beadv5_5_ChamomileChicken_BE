package jabaclass.admin.review.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jabaclass.admin.common.model.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "product_reviews", schema = "public")
@AttributeOverrides({
	@AttributeOverride(name = "createdAt", column = @Column(name = "reg_dt", nullable = false, updatable = false)),
	@AttributeOverride(name = "updatedAt", column = @Column(name = "modify_dt", nullable = false))
})
public class Review extends BaseEntity {

	@Column(name = "product_id", updatable = false, nullable = false)
	private UUID productId;

	@Column(name = "user_id", updatable = false, nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private int rating;

	@Column(nullable = false)
	private String content;

	@Column(name = "delete_dt")
	private LocalDateTime deleteDt;

	public void softDelete() {
		this.deleteDt = LocalDateTime.now();
	}
}
