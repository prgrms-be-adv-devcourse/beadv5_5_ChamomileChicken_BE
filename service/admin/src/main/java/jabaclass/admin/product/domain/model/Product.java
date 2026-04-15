package jabaclass.admin.product.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import jabaclass.admin.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

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

	public void forceDown() {
		this.status = ProductStatus.DISABLE;
	}
}
