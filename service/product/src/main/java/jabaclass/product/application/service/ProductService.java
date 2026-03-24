package jabaclass.product.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.client.SellerClient;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.event.dto.ProductEventResponseDto;
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
		SellerResponseDto seller = sellerClient.findSeller(requestDto.sellerId())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		if (!"SELLER".equals(seller.role())) {
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

		publisher.publishEvent(new ProductEventResponseDto(saveProduct.getId()));
		return CreateProductResponseDto.form(saved, seller.sellerName());
	}
}
