package jabaclass.ai.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_activities")
public class UserActivity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private UUID productId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ActionType actionType;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public UserActivity(UUID userId, UUID productId, ActionType actionType) {
		this.userId = userId;
		this.productId = productId;
		this.actionType = actionType;
		this.createdAt = LocalDateTime.now();
	}

	public static UserActivity create(UUID userId, UUID productId, ActionType actionType) {
		return new UserActivity(userId, productId, actionType);
	}
}
