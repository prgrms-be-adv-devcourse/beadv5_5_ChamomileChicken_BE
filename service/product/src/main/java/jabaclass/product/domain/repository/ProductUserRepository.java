package jabaclass.product.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.product.domain.model.ProductUser;

public interface ProductUserRepository {
	// 스케줄별 예약자 조회
	List<ProductUser> findByProductScheduleId(UUID scheduleId);

	// 스케줄별 예약자 생성
	ProductUser save(ProductUser requestDto);

	Optional<ProductUser> findById(UUID id);

}
