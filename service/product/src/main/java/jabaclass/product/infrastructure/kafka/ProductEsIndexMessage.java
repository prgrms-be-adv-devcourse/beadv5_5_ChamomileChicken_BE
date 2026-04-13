package jabaclass.product.infrastructure.kafka;

import jabaclass.product.infrastructure.elasticsearch.ProductDocument;

public record ProductEsIndexMessage(
	String operation,       // "SAVE" or "DELETE"
	ProductDocument document, // SAVE 시 사용, DELETE 시 null
	String productId        // DELETE 시 사용, SAVE 시 null
) {
	public static ProductEsIndexMessage save(ProductDocument document) {
		return new ProductEsIndexMessage("SAVE", document, null);
	}

	public static ProductEsIndexMessage delete(String productId) {
		return new ProductEsIndexMessage("DELETE", null, productId);
	}
}
