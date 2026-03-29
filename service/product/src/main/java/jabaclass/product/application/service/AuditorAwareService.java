package jabaclass.product.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareService implements AuditorAware<UUID> {
	//TODO 추후 시큐리티 적용하면 수정
	@Override
	public Optional<UUID> getCurrentAuditor() {
		// 테스트용입니다.
		UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

		// TODO: 나중에 Security 붙이면 여기서 꺼냄
		return Optional.of(SELLER_ID);
	}
}
