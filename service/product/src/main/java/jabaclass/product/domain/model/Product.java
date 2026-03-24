package jabaclass.product.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "\"product\"", schema = "public")
public class Product {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false)
	private int maxCapacity;

	// null이 가능하게 하고 백단이든, 프론트 단이든 둘 중 하나라도 있게 체크하는게 좋을듯
	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "description_image")
	private String descriptionImage;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 20)
	private ProductStatus status;

	// AuditorAware 통해 세팅 가능-> 추후 확인
	@Column(name = "reg_id", nullable = false, updatable = false)
	private UUID regId;

	@Column(name = "reg_dt", nullable = false, updatable = false)
	private LocalDateTime regDt;

	@Column(name = "modify_id")
	private UUID modifyId;

	@Column(name = "modify_dt")
	private LocalDateTime modifyDt;

	@PrePersist
	public void prePersist() {
		this.id = UUID.randomUUID();
		if (this.status == null) {
			this.status = ProductStatus.ENABLE;
		}
		this.regDt = LocalDateTime.now();
		this.regId = sellerId;
	}

	@PreUpdate
	public void chageProduct() {
		this.modifyDt = LocalDateTime.now();
		this.modifyId = sellerId;
	}

	private Product(UUID id,
		UUID sellerId,
		String title,
		int maxCapacity,
		String description,
		String descriptionImage,
		BigDecimal price,
		ProductStatus status) {
		this.id = id;
		this.sellerId = sellerId;
		this.title = title;
		this.maxCapacity = maxCapacity;
		this.description = description;
		this.descriptionImage = descriptionImage;
		this.price = price;
		this.status = status;
	}

	public static Product create(
		UUID sellerId,
		String title,
		int maxCapacity,
		String description,
		String descriptionImage,
		BigDecimal price,
		ProductStatus status
	) {
		return new Product(
			UUID.randomUUID(),
			sellerId,
			title,
			maxCapacity,
			description,
			descriptionImage,
			price,
			status
		);
	}

}
