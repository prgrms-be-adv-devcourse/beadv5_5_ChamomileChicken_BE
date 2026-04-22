package jabaclass.admin.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import jabaclass.admin.order.domain.model.Order;
import jabaclass.admin.order.domain.model.OrderStatus;
import jabaclass.admin.order.domain.repository.OrderAdminRepository;
import jabaclass.admin.order.presentation.dto.response.OrderAdminResponseDto;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class OrderAdminServiceTest {

	@Mock
	private OrderAdminRepository orderAdminRepository;

	@InjectMocks
	private OrderAdminService orderAdminService;

	private UUID orderId;
	private Order order;

	@BeforeEach
	void setUp() {
		orderId = UUID.randomUUID();
		order = new Order();
		ReflectionTestUtils.setField(order, "id", orderId);
		ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2025, 4, 1, 12, 0));
		ReflectionTestUtils.setField(order, "productScheduleId", UUID.randomUUID());
		ReflectionTestUtils.setField(order, "userId", UUID.randomUUID());
		ReflectionTestUtils.setField(order, "sellerId", UUID.randomUUID());
		ReflectionTestUtils.setField(order, "quantity", 2);
		ReflectionTestUtils.setField(order, "price", new BigDecimal("100000"));
		ReflectionTestUtils.setField(order, "status", OrderStatus.PAID);
	}

	@Test
	void 전체_주문_목록을_조회한다() {
		// given
		Pageable pageable = PageRequest.of(0, 10);
		given(orderAdminRepository.findAll(pageable))
			.willReturn(new PageImpl<>(List.of(order)));

		// when
		Page<OrderAdminResponseDto> result = orderAdminService.getOrders(pageable);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).id()).isEqualTo(orderId);
		assertThat(result.getContent().get(0).status()).isEqualTo(OrderStatus.PAID);
		assertThat(result.getContent().get(0).sellerId()).isNotNull();
		assertThat(result.getContent().get(0).createdAt()).isEqualTo(LocalDateTime.of(2025, 4, 1, 12, 0));
		then(orderAdminRepository).should(times(1)).findAll(pageable);
	}
}
