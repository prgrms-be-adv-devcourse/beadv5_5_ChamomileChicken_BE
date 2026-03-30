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
		// 테스트용입니다.
		UUID USER_ID = SecurityUtil.getCurrentUserId();

		return Optional.of(USER_ID);
	}

}
