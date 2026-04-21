package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.exception.BusinessException;
import jabaclass.product.application.service.ProductService;
import jabaclass.product.application.usecase.ValidateFileUseCase;
import jabaclass.product.common.exception.CommonErrorCode;
import jabaclass.product.domain.model.Product;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ProductSearchRepository;
import jabaclass.product.infrastructure.acl.dto.response.UserResponseDto;
import jabaclass.product.infrastructure.elasticsearch.ProductDocument;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.response.ProductResponseDto;
import jabaclass.product.presentation.dto.response.SearchProductResponseDto;

@ExtendWith(MockitoExtension.class)
class ProductSelectTest {

	@InjectMocks
	private ProductService productService;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductSearchRepository productSearchRepository;

	@Mock
	private SellerRepository sellerRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	@Mock
	private ValidateFileUseCase validateFileUseCase;

	private static final BigDecimal PRICE = new BigDecimal("1000.50");
	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID PRODUCT_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");

	private Product product1;

	@BeforeEach
	void setup() {
		product1 = Product.builder()
			.sellerId(SELLER_ID)
			.title("상품A")
			.maxCapacity(10)
			.description("테스트")
			.price(PRICE)
			.status(ProductStatus.ENABLE)
			.build();
		ReflectionTestUtils.setField(product1, "id", PRODUCT_ID);
	}

	@Test
	void 전체_상품을_조회한다() {
		SearchProductRequestDto request = new SearchProductRequestDto("", 0, 10, ProductStatus.ENABLE);

		ProductDocument doc1 = ProductDocument.builder()
			.id(PRODUCT_ID.toString())
			.sellerId(SELLER_ID.toString())
			.sellerName("판매자")
			.title("상품A")
			.description("테스트")
			.status("ENABLE")
			.price(PRICE)
			.maxCapacity(10)
			.deleted(false)
			.build();
		ProductDocument doc2 = ProductDocument.builder()
			.id(UUID.randomUUID().toString())
			.sellerId(SELLER_ID.toString())
			.sellerName("판매자")
			.title("상품B")
			.description("테스트")
			.status("ENABLE")
			.price(PRICE)
			.maxCapacity(3)
			.deleted(false)
			.build();

		Page<ProductDocument> esPage = new PageImpl<>(List.of(doc1, doc2));
		given(productSearchRepository.findAllEnabled(any(Pageable.class))).willReturn(esPage);

		SearchProductResponseDto result = productService.searchAll(request);

		assertThat(result.content()).extracting(ProductResponseDto::title)
			.containsExactly("상품A", "상품B");
		assertThat(result.totalCount()).isEqualTo(2);
		then(productSearchRepository).should().findAllEnabled(any(Pageable.class));
		then(productRepository).shouldHaveNoInteractions();
	}

	@Test
	void 특정_상품_조회에_성공한다() {
		given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product1));
		given(sellerRepository.findSeller(SELLER_ID))
			.willReturn(Optional.of(new UserResponseDto(SELLER_ID, "테스트판매자", "SELLER")));

		ProductResponseDto result = productService.searchById(PRODUCT_ID);

		assertThat(result.title()).isEqualTo("상품A");
		then(productRepository).should().findById(PRODUCT_ID);
	}

	@Test
	void 특정_상품_조회에_실패한다() {
		given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.searchById(PRODUCT_ID))
			.isInstanceOf(BusinessException.class)
			.hasMessage(CommonErrorCode.PRODUCT_NOT_FOUND.getMessage());
		then(productRepository).should().findById(PRODUCT_ID);
	}
}
