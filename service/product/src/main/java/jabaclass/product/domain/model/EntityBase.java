package jabaclass.product.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
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
	// AuditorAware 통해 세팅 가능-> 추후 확인
	@CreatedBy
	@Column(name = "reg_id", nullable = false, updatable = false)
	private UUID regId;

	@CreatedDate
	@Column(name = "reg_dt", nullable = false, updatable = false)
	private LocalDateTime regDt;

	@LastModifiedBy
	@Column(name = "modify_id")
	private UUID modifyId;

	@LastModifiedDate
	@Column(name = "modify_dt")
	private LocalDateTime modifyDt;

	@Column(name = "delete_dt")
	private LocalDateTime deleteDt;

	public void changeDelete() {
		this.deleteDt = LocalDateTime.now();
	}
}
