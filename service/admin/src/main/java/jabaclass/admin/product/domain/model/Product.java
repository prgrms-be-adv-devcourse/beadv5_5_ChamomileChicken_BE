package jabaclass.admin.product.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "reg_dt", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(name = "max_capacity", nullable = false)
	private int maxCapacity;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status;

	@Column(name = "delete_dt")
	private LocalDateTime deleteDt;

	public void forceDown() {
		this.status = ProductStatus.DISABLE;
		this.deleteDt = LocalDateTime.now();
	}
}
