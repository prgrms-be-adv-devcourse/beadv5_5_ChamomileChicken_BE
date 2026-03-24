package jabaclass.product.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.usecase.ProductUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.infrastructure.acl.dto.SellerResponseDto;
import jabaclass.product.infrastructure.acl.dto.SellerRole;
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
	private final SellerRepository sellerRepository;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional
	public CreateProductResponseDto create(CreateProductRequestDto requestDto) {

		// sellerId를 확인
		SellerResponseDto seller = sellerRepository.findSeller(requestDto.sellerId())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SELLER_NOT_FOUND));

		SellerRole role = SellerRole.from(seller.role());

		if (role != SellerRole.SELLER) {
			throw new BusinessException(CommonErrorCode.NOT_SELLER);
		}

		Product product = Product.builder()
			.sellerId(requestDto.sellerId())
			.title(requestDto.title())
			.maxCapacity(requestDto.maxCapacity())
			.description(requestDto.description())
			.descriptionImage(requestDto.descriptionImage())
			.price(requestDto.price())
			.status(requestDto.status())
			.build();

		Product saved = productRepository.save(product);

		publisher.publishEvent(new ProductEventResponseDto(saved.getId()));
		return CreateProductResponseDto.form(saved, seller.sellerName());
	}
}
