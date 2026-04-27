package jabaclass.settlement.infrastructure.batch.writer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import jabaclass.settlement.application.dto.MonthlySettlementAggregationResult;
import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SettlementAggregationItemWriter implements ItemWriter<MonthlySettlementAggregationResult> {

	private final SettlementRepository settlementRepository;
	private final SellerGradeRepository sellerGradeRepository;

	@Override
	public void write(Chunk<? extends MonthlySettlementAggregationResult> items) {
		List<MonthlySettlementAggregationResult> results = items.getItems().stream()
			.map(MonthlySettlementAggregationResult.class::cast)
			.toList();

		if (results.isEmpty()) {
			return;
		}

		List<Settlement> settlements = new ArrayList<>();
		List<SellerGrade> sellerGrades = new ArrayList<>();
		for (MonthlySettlementAggregationResult result : results) {
			settlements.add(result.settlement());
			sellerGrades.add(result.sellerGrade());
		}

		if (!sellerGrades.isEmpty()) {
			sellerGradeRepository.saveAll(sellerGrades);
		}
		if (!settlements.isEmpty()) {
			settlementRepository.saveAll(settlements);
		}
	}
}
