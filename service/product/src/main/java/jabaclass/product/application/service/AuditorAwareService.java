package jabaclass.product.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import jabaclass.auth.util.SecurityUtil;

@Component
public class AuditorAwareService implements AuditorAware<UUID> {
	@Override
	public Optional<UUID> getCurrentAuditor() {
		try {
			return Optional.of(SecurityUtil.getCurrentUserId());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
