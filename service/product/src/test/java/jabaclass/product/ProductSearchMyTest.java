package jabaclass.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.service.ProductService;
import jabaclass.product.application.usecase.ValidateFileUseCase;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ProductSearchRepository;
import jabaclass.product.infrastructure.elasticsearch.ProductDocument;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.response.ProductResponseDto;
import jabaclass.product.presentation.dto.response.SearchProductResponseDto;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class ProductSearchMyTest {

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

	private static final UUID SELLER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final BigDecimal PRICE = new BigDecimal("1000.50");

	private ProductDocument doc1;
	private ProductDocument doc2;

	@BeforeEach
	void setUp() {
		doc1 = ProductDocument.builder()
			.id(UUID.randomUUID().toString())
			.sellerId(SELLER_ID.toString())
			.sellerName("판매자A")
			.title("향수 공방 클래스")
			.description("직접 향수를 만들어보는 클래스입니다.")
			.status("ENABLE")
			.price(PRICE)
			.maxCapacity(10)
			.deleted(false)
			.build();

		doc2 = ProductDocument.builder()
			.id(UUID.randomUUID().toString())
			.sellerId(SELLER_ID.toString())
			.sellerName("판매자A")
			.title("캔들 공방 클래스")
			.description("직접 캔들을 만들어보는 클래스입니다.")
			.status("ENABLE")
			.price(PRICE)
			.maxCapacity(5)
			.deleted(false)
			.build();
	}

	@Test
	void 내_상품_목록_조회_title_없을때_성공() {
		// given
		SearchProductRequestDto request = new SearchProductRequestDto(null, 0, 10, ProductStatus.ENABLE);
		Page<ProductDocument> esPage = new PageImpl<>(List.of(doc1, doc2));
		given(productSearchRepository.findAllBySellerId(eq(SELLER_ID.toString()), any(Pageable.class)))
			.willReturn(esPage);

		// when
		SearchProductResponseDto result = productService.searchMy(request, SELLER_ID);

		// then
		assertThat(result.totalCount()).isEqualTo(2);
		assertThat(result.content()).extracting(ProductResponseDto::title)
			.containsExactlyInAnyOrder("향수 공방 클래스", "캔들 공방 클래스");
		then(productSearchRepository).should().findAllBySellerId(eq(SELLER_ID.toString()), any(Pageable.class));
		then(productSearchRepository).should(never()).searchByKeywordAndSellerId(any(), any(), any());
	}

	@Test
	void 내_상품_목록_조회_title_있을때_성공() {
		// given
		SearchProductRequestDto request = new SearchProductRequestDto("향수", 0, 10, ProductStatus.ENABLE);
		Page<ProductDocument> esPage = new PageImpl<>(List.of(doc1));
		given(productSearchRepository.searchByKeywordAndSellerId(eq("향수"), eq(SELLER_ID.toString()), any(Pageable.class)))
			.willReturn(esPage);

		// when
		SearchProductResponseDto result = productService.searchMy(request, SELLER_ID);

		// then
		assertThat(result.totalCount()).isEqualTo(1);
		assertThat(result.content()).extracting(ProductResponseDto::title)
			.containsExactly("향수 공방 클래스");
		then(productSearchRepository).should().searchByKeywordAndSellerId(eq("향수"), eq(SELLER_ID.toString()), any(Pageable.class));
		then(productSearchRepository).should(never()).findAllBySellerId(any(), any());
	}

	@Test
	void 내_상품_목록_조회_결과_비어있을때_성공() {
		// given
		SearchProductRequestDto request = new SearchProductRequestDto("없는키워드", 0, 10, ProductStatus.ENABLE);
		given(productSearchRepository.searchByKeywordAndSellerId(eq("없는키워드"), eq(SELLER_ID.toString()), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of()));

		// when
		SearchProductResponseDto result = productService.searchMy(request, SELLER_ID);

		// then
		assertThat(result.totalCount()).isZero();
		assertThat(result.content()).isEmpty();
	}
}