package jabaclass.product.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareService implements AuditorAware<UUID> {
	@Override
	public Optional<UUID> getCurrentAuditor() {

		// TODO: 나중에 Security 붙이면 여기서 꺼냄
		return Optional.of(UUID.randomUUID());
	}
}
