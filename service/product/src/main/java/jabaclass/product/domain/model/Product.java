package jabaclass.product.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.common.exception.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "\"product\"", schema = "public")
public class Product extends ProductBase {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "seller_id", nullable = false)
	private UUID sellerId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(name = "max_capacity", nullable = false)
	private int maxCapacity;

	// null이 가능하게 하고 백단이든, 프론트 단이든 둘 중 하나라도 있게 체크하는게 좋을듯
	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "description_image")
	private String descriptionImage;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status;

	@PrePersist
	public void prePersist() {
		if (this.status == null) {
			this.status = ProductStatus.ENABLE;
		}
	}

	public void changeTitle(String title) {
		if (title.isBlank()) {
			throw new BusinessException(CommonErrorCode.NOT_TITLE);
		}
		this.title = title;
	}

	public void changeMaxCapacity(int maxCapacity) {
		if (maxCapacity <= 0) {
			throw new BusinessException(CommonErrorCode.NOT_MAXCAPACITY);
		}
		this.maxCapacity = maxCapacity;
	}

	public void changePrice(BigDecimal price) {
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(CommonErrorCode.NOT_PRICE);
		}
		this.price = price;
	}

	public void changeDescription(String description) {
		this.description = description;
	}

	public void changeDescriptionImage(String descriptionImage) {
		this.descriptionImage = descriptionImage;
	}

	public void changeStatus(ProductStatus status) {
		this.status = status;
	}

}
