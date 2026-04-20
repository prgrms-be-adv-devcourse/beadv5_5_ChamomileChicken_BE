package jabaclass.admin.common.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jabaclass.admin.order.application.usecase.OrderAdminUseCase;
import jabaclass.admin.product.application.usecase.ProductAdminUseCase;
import jabaclass.admin.review.application.usecase.ReviewAdminUseCase;
import jabaclass.admin.settlement.application.usecase.SettlementAdminUseCase;
import jabaclass.admin.user.application.usecase.UserAdminUseCase;

@SuppressWarnings("NonAsciiCharacters")
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
@DisplayNameGeneration(ReplaceUnderscores.class)
class AdminRoleInterceptorTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@MockitoBean
	private UserAdminUseCase userAdminUseCase;

	@MockitoBean
	private ProductAdminUseCase productAdminUseCase;

	@MockitoBean
	private ReviewAdminUseCase reviewAdminUseCase;

	@MockitoBean
	private OrderAdminUseCase orderAdminUseCase;

	@MockitoBean
	private SettlementAdminUseCase settlementAdminUseCase;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void ADMIN_헤더가_없으면_403을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/admins/users"))
			.andExpect(status().isForbidden());
	}

	@Test
	void ADMIN이_아닌_역할이면_403을_반환한다() throws Exception {
		mockMvc.perform(get("/api/v1/admins/users")
				.header("X-User-Role", "USER"))
			.andExpect(status().isForbidden());
	}

	@Test
	void ADMIN_헤더면_유저_목록_조회가_통과된다() throws Exception {
		given(userAdminUseCase.getUsers(any())).willReturn(Page.empty());

		mockMvc.perform(get("/api/v1/admins/users")
				.header("X-User-Role", "ADMIN"))
			.andExpect(status().isOk());
	}

	@Test
	void ADMIN_헤더면_셀러_승인이_통과된다() throws Exception {
		mockMvc.perform(patch("/api/v1/admins/users/{userId}/approve-seller", UUID.randomUUID())
				.header("X-User-Role", "ADMIN"))
			.andExpect(status().isOk());
	}

	@Test
	void ADMIN_헤더면_리뷰_삭제가_통과된다() throws Exception {
		mockMvc.perform(delete("/api/v1/admins/reviews/{reviewId}", UUID.randomUUID())
				.header("X-User-Role", "ADMIN"))
			.andExpect(status().isOk());
	}

	@Test
	void ADMIN_헤더면_상품_강제_내리기가_통과된다() throws Exception {
		mockMvc.perform(patch("/api/v1/admins/products/{productId}/force-down", UUID.randomUUID())
				.header("X-User-Role", "ADMIN"))
			.andExpect(status().isOk());
	}
}
