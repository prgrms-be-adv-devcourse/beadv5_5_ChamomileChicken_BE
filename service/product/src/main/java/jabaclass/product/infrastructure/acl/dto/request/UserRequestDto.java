package jabaclass.product.infrastructure.acl.dto.request;

import java.util.List;
import java.util.UUID;

public record UserRequestDto(
	List<UUID> userIds
) {

	public static UserRequestDto from(List<UUID> userIds) {
		return new UserRequestDto(
			userIds
		);
	}

}
