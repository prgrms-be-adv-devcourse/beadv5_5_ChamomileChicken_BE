package jabaclass.settlement.infrastructure.batch.writer;

import java.util.List;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SellerGradeItemWriter implements ItemWriter<SellerGrade> {

	private final SellerGradeRepository sellerGradeRepository;

	@Override
	public void write(Chunk<? extends SellerGrade> items) {
		List<SellerGrade> sellerGrades = items.getItems().stream()
			.map(SellerGrade.class::cast)
			.toList();

		if (sellerGrades.isEmpty()) {
			return;
		}

		sellerGradeRepository.saveAll(sellerGrades);
	}
}
