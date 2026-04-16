package jabaclass.admin.review.application.service;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.admin.review.domain.model.Review;
import jabaclass.admin.review.infrastructure.persistence.ReviewAdminJpaRepository;

@SuppressWarnings("NonAsciiCharacters")
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
@Transactional("productTransactionManager")
@DisplayNameGeneration(ReplaceUnderscores.class)
class ReviewAdminServiceIntegrationTest {

	@Autowired
	private ReviewAdminService reviewAdminService;

	@Autowired
	private ReviewAdminJpaRepository reviewAdminJpaRepository;

	@MockitoBean
	private KafkaTemplate<String, String> kafkaTemplate;

	@Test
	void 리뷰_삭제시_deleteDt가_DB에_반영된다() {
		// given
		Review review = Review.builder()
			.productId(UUID.randomUUID())
			.userId(UUID.randomUUID())
			.rating(3)
			.content("테스트 리뷰")
			.build();
		Review saved = reviewAdminJpaRepository.save(review);
		reviewAdminJpaRepository.flush();

		// when
		reviewAdminService.deleteReview(saved.getId());

		// then
		Review found = reviewAdminJpaRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getDeleteDt()).isNotNull();
	}

	@Test
	void 리뷰_삭제후_행이_물리적으로_남아있다() {
		// given — 소프트딜리트이므로 레코드 자체는 삭제되지 않아야 함
		Review review = Review.builder()
			.productId(UUID.randomUUID())
			.userId(UUID.randomUUID())
			.rating(5)
			.content("좋아요")
			.build();
		Review saved = reviewAdminJpaRepository.save(review);
		reviewAdminJpaRepository.flush();

		// when
		reviewAdminService.deleteReview(saved.getId());

		// then
		assertThat(reviewAdminJpaRepository.findById(saved.getId())).isPresent();
	}
}
