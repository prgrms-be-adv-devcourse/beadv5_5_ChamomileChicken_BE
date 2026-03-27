package jabaclass.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.domain.repository.ProductUserRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductUserRepositoryAdapter implements ProductUserRepository {

	private final ProductUserJpaRepository productUserJpaRepository;

	@Override
	public List<ProductUser> findByProductScheduleId(UUID scheduleId) {
		return productUserJpaRepository.findByProductScheduleId(scheduleId);
	}

	@Override
	public ProductUser save(ProductUser requestDto) {
		return productUserJpaRepository.save(requestDto);
	}

	@Override
	public Optional<ProductUser> findById(UUID id) {
		return productUserJpaRepository.findById(id);
	}
}
