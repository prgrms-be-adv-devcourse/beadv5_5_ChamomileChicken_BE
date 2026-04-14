package jabaclass.product.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class EntityBase {

	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@CreatedDate
	@Column(name = "reg_dt", nullable = false, updatable = false)
	private LocalDateTime regDt;

	@LastModifiedDate
	@Column(name = "modify_dt")
	private LocalDateTime modifyDt;

	@Column(name = "delete_dt")
	private LocalDateTime deleteDt;

	public void changeDelete() {
		this.deleteDt = LocalDateTime.now();
	}
}
