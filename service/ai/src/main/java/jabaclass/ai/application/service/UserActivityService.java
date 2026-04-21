package jabaclass.ai.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.ai.domain.model.ActionType;
import jabaclass.ai.domain.model.UserActivity;
import jabaclass.ai.domain.repository.UserActivityRepository;
import jabaclass.ai.infrastructure.kafka.ProductViewedEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActivityService {

	private final UserActivityRepository userActivityRepository;

	@Transactional
	public void recordProductView(ProductViewedEvent event) {
		userActivityRepository.save(UserActivity.create(event.userId(), event.productId(), ActionType.VIEW));
	}
}
