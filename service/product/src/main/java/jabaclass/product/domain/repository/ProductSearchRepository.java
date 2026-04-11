package jabaclass.product.domain.repository;

import jabaclass.product.infrastructure.elasticsearch.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductSearchRepository {

	// ES에 상품 색인
	ProductDocument save(ProductDocument document);

	// ES 일괄 색인 (초기 마이그레이션용)
	void saveAll(List<ProductDocument> documents);

	// ES에서 상품 삭제
	void deleteById(String id);

	// 키워드로 상품 검색 (title, description 대상)
	Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);

	// 키워드 없이 전체 조회 (ENABLE 상품만)
	Page<ProductDocument> findAllEnabled(Pageable pageable);
}
