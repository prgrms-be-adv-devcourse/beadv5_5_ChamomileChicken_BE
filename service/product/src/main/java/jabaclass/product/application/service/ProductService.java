package jabaclass.product.application.service;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.client.SellerClient;
import jabaclass.product.infrastructure.acl.dto.SellerResposeDto;
import jabaclass.product.infrastructure.event.dto.ProductEventResposeDto;
import jabaclass.product.presentation.dto.request.CreateProductRequestDto;
import jabaclass.product.presentation.dto.respose.CreateProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductService implements ProductUseCase {
	private final ProductRepository productRepository;
	private final SellerClient sellerClient;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional
	public CreateProductResponseDto create(CreateProductRequestDto requestDto) {
		// sellerId를 확인
		Optional<SellerResposeDto> seller = Optional.of(sellerClient.findSeller(requestDto.sellerId())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND)));

		// 일반 사용자일 경우
		if ("USER".equals(seller.get().role())) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}

		// repository에 parameter로 사용할 Product 생성
		Product saveProduct = Product.create(
			requestDto.sellerId(),
			requestDto.title(),
			requestDto.maxCapacity(),
			requestDto.description(),
			requestDto.descriptionImage(),
			requestDto.price(),
			requestDto.status()
		);

		Product saved = productRepository.save(saveProduct);

		//	em.persist(saveProduct);

		publisher.publishEvent(new ProductEventResposeDto(saveProduct.getId()));
		return CreateProductResponseDto.form(saved, seller.get().sellerName());
	}
}
