package jabaclass.product.infrastructure.elasticsearch;

import jabaclass.product.domain.model.Product;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
public class ProductDocument {

	@Id
	private String id;

	private String sellerId;

	private String sellerName;

	// nori 분석기 적용 — 한국어 형태소 분석으로 부분 검색 지원
	@Field(type = FieldType.Text, analyzer = "nori")
	private String title;

	@Field(type = FieldType.Text, analyzer = "nori")
	private String description;

	@Field(type = FieldType.Keyword)
	private String status;

	@Field(type = FieldType.Double)
	private BigDecimal price;

	private int maxCapacity;

	private String thumbnailPath;

	// deleteDt != null이면 true (소프트 삭제된 상품 필터링용)
	private boolean deleted;

	private LocalDateTime regDt;

	public static ProductDocument from(Product product, String sellerName) {
		return ProductDocument.builder()
			.id(product.getId().toString())
			.sellerId(product.getSellerId().toString())
			.sellerName(sellerName)
			.title(product.getTitle())
			.description(product.getDescription())
			.status(product.getStatus().name())
			.price(product.getPrice())
			.maxCapacity(product.getMaxCapacity())
			.thumbnailPath(product.getThumbnailPath())
			.deleted(product.getDeleteDt() != null)
			.regDt(product.getRegDt())
			.build();
	}
}
