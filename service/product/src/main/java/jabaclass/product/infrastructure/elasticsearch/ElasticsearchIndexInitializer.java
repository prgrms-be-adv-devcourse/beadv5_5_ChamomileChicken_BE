package jabaclass.product.infrastructure.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
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

	@Value("${spring.jpa.hibernate.ddl-auto:none}")
	private String ddlAuto;

	@Override
	public void run(ApplicationArguments args) {
		IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);

		if ("create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
			if (indexOps.exists()) {
				indexOps.delete();
				log.info("products 인덱스 삭제 (DB 초기화 감지)");
			}
			indexOps.createWithMapping();
			log.info("products 인덱스 재생성 완료");
		} else if (!indexOps.exists()) {
			indexOps.createWithMapping();
			log.info("products 인덱스 생성 완료");
		}
	}
}
