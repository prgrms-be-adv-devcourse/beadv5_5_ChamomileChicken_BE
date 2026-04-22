package jabaclass.admin.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jabaclass.admin.common.error.BusinessException;
import jabaclass.admin.product.domain.model.OutboxEvent;
import jabaclass.admin.product.domain.model.Product;
import jabaclass.admin.product.domain.model.ProductStatus;
import jabaclass.admin.product.domain.repository.OutboxEventRepository;
import jabaclass.admin.product.domain.repository.ProductAdminRepository;
import jabaclass.admin.product.presentation.dto.response.ProductAdminResponseDto;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class ProductAdminServiceTest {

	@Mock
	private ProductAdminRepository productAdminRepository;

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private ProductAdminService productAdminService;

	private UUID productId;
	private Product product;

	@BeforeEach
	void setUp() {
		productId = UUID.randomUUID();
		product = new Product();
		ReflectionTestUtils.setField(product, "id", productId);
		ReflectionTestUtils.setField(product, "sellerId", UUID.randomUUID());
		ReflectionTestUtils.setField(product, "title", "테스트상품");
		ReflectionTestUtils.setField(product, "maxCapacity", 10);
		ReflectionTestUtils.setField(product, "price", new BigDecimal("50000"));
		ReflectionTestUtils.setField(product, "status", ProductStatus.ENABLE);
	}

	@Test
	void 전체_상품_목록을_조회한다() {
		Pageable pageable = PageRequest.of(0, 10);
		given(productAdminRepository.findAll(pageable))
			.willReturn(new PageImpl<>(List.of(product)));

		Page<ProductAdminResponseDto> result = productAdminService.getProducts(pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(productId);
		assertThat(result.getContent().get(0).title()).isEqualTo("테스트상품");
		assertThat(result.getContent().get(0).status()).isEqualTo(ProductStatus.ENABLE);
		then(productAdminRepository).should(times(1)).findAll(pageable);
	}

	@Test
	void 상품을_강제로_내리면_상태가_DISABLE로_변경되고_아웃박스_이벤트가_저장된다() throws Exception {
		given(productAdminRepository.findById(productId)).willReturn(Optional.of(product));
		given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"FORCE_DOWN\"}");

		productAdminService.forceDownProduct(productId);

		assertThat(product.getStatus()).isEqualTo(ProductStatus.DISABLE);
		assertThat(product.getDeleteDt()).isNotNull();
		then(outboxEventRepository).should(times(1)).save(any(OutboxEvent.class));
	}

	@Test
	void 강제로_내린_아웃박스_이벤트는_PENDING_상태로_저장된다() throws Exception {
		given(productAdminRepository.findById(productId)).willReturn(Optional.of(product));
		given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"FORCE_DOWN\"}");

		productAdminService.forceDownProduct(productId);

		then(outboxEventRepository).should(times(1)).save(
			argThat(event -> event.getStatus().name().equals("PENDING")
				&& event.getEventType().getTopic().equals("admin.product"))
		);
	}

	@Test
	void 존재하지_않는_상품_강제_내리기시_예외가_발생한다() {
		given(productAdminRepository.findById(productId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> productAdminService.forceDownProduct(productId))
			.isInstanceOf(BusinessException.class)
			.hasMessage("상품을 찾을 수 없습니다.");
		then(outboxEventRepository).shouldHaveNoInteractions();
	}
}
