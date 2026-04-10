package jabaclass.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import jabaclass.product.application.acl.SellerRepository;
import jabaclass.product.application.service.AuditorAwareService;
import jabaclass.product.application.service.ProductService;
import jabaclass.product.domain.model.status.ProductStatus;
import jabaclass.product.domain.repository.ProductRepository;
import jabaclass.product.domain.repository.ProductSearchRepository;
import jabaclass.product.infrastructure.acl.client.FileConfirmClient;
import jabaclass.product.infrastructure.elasticsearch.ProductDocument;
import jabaclass.product.presentation.dto.request.SearchProductRequestDto;
import jabaclass.product.presentation.dto.respose.ProductResponseDto;
import jabaclass.product.presentation.dto.respose.SearchProductResponseDto;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProductSearchTest {

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
	private AuditorAwareService auditorAwareService;

	@Mock
	private FileConfirmClient fileConfirmClient;

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
	void 키워드_없으면_전체_ENABLE_상품을_ES에서_조회한다() {
		SearchProductRequestDto request = new SearchProductRequestDto("", 0, 10, ProductStatus.ENABLE);
		Page<ProductDocument> esPage = new PageImpl<>(List.of(doc1, doc2));
		given(productSearchRepository.findAllEnabled(any(Pageable.class))).willReturn(esPage);

		SearchProductResponseDto result = productService.searchAll(request);

		assertThat(result.totalCount()).isEqualTo(2);
		assertThat(result.content()).extracting(ProductResponseDto::title)
			.containsExactlyInAnyOrder("향수 공방 클래스", "캔들 공방 클래스");
		then(productSearchRepository).should().findAllEnabled(any(Pageable.class));
		then(productSearchRepository).should(never()).searchByKeyword(any(), any());
	}

	@Test
	void 키워드가_있으면_ES_검색을_호출한다() {
		SearchProductRequestDto request = new SearchProductRequestDto("향수", 0, 10, ProductStatus.ENABLE);
		Page<ProductDocument> esPage = new PageImpl<>(List.of(doc1));
		given(productSearchRepository.searchByKeyword(eq("향수"), any(Pageable.class))).willReturn(esPage);

		SearchProductResponseDto result = productService.searchAll(request);

		assertThat(result.totalCount()).isEqualTo(1);
		assertThat(result.content()).extracting(ProductResponseDto::title)
			.containsExactly("향수 공방 클래스");
		then(productSearchRepository).should().searchByKeyword(eq("향수"), any(Pageable.class));
		then(productSearchRepository).should(never()).findAllEnabled(any());
	}

	@Test
	void ES_검색결과에_sellerName이_포함된다() {
		SearchProductRequestDto request = new SearchProductRequestDto("공방", 0, 10, ProductStatus.ENABLE);
		Page<ProductDocument> esPage = new PageImpl<>(List.of(doc1, doc2));
		given(productSearchRepository.searchByKeyword(eq("공방"), any(Pageable.class))).willReturn(esPage);

		SearchProductResponseDto result = productService.searchAll(request);

		assertThat(result.content()).extracting(ProductResponseDto::sellerName)
			.containsOnly("판매자A");
		// ES에 sellerName이 비정규화되어 있어 user 서비스 호출이 없어야 함
		then(sellerRepository).shouldHaveNoInteractions();
	}

	@Test
	void 검색_결과가_없으면_빈_리스트를_반환한다() {
		SearchProductRequestDto request = new SearchProductRequestDto("존재하지않는키워드", 0, 10, ProductStatus.ENABLE);
		given(productSearchRepository.searchByKeyword(any(), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of()));

		SearchProductResponseDto result = productService.searchAll(request);

		assertThat(result.totalCount()).isZero();
		assertThat(result.content()).isEmpty();
	}

	@Test
	void 페이징_정보가_올바르게_반환된다() {
		SearchProductRequestDto request = new SearchProductRequestDto("", 2, 5, ProductStatus.ENABLE);
		Page<ProductDocument> esPage = new PageImpl<>(
			List.of(doc1),
			org.springframework.data.domain.PageRequest.of(2, 5),
			11  // 전체 11개
		);
		given(productSearchRepository.findAllEnabled(any(Pageable.class))).willReturn(esPage);

		SearchProductResponseDto result = productService.searchAll(request);

		assertThat(result.thisPage()).isEqualTo(2);
		assertThat(result.totalCount()).isEqualTo(11);
		assertThat(result.totalPage()).isEqualTo(3);  // 11 / 5 = ceil(2.2) = 3
	}
}