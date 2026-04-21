package jabaclass.product.infrastructure.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer implements ApplicationRunner {

	private final ElasticsearchOperations elasticsearchOperations;

	@Override
	public void run(ApplicationArguments args) {
		IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
		if (!indexOps.exists()) {
			indexOps.createWithMapping();
			log.info("products 인덱스 생성 완료");
		}
	}
}
