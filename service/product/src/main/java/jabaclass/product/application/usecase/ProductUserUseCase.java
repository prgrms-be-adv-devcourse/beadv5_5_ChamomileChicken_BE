package jabaclass.product.application.usecase;

import java.util.List;
import java.util.UUID;

import jabaclass.product.domain.model.ProductUser;
import jabaclass.product.presentation.dto.request.CreateProductUserRequestDto;
import jabaclass.product.presentation.dto.respose.ProductUserResponseDto;

public interface ProductUserUseCase {

	// 스케줄별 예약자 조회
	List<ProductUserResponseDto> getUser(UUID userId);

	// 스케줄별 예약자 생성
	ProductUserResponseDto create(CreateProductUserRequestDto requestDto);

	ProductUserResponseDto findById(UUID id);

	ProductUser innerFindById(UUID id);

	List<ProductUser> innserUserList(UUID scheduleId);

}
