package jabaclass.product.infrastructure.event.dto;

import jabaclass.product.infrastructure.elasticsearch.ProductDocument;

public record ProductEsSaveEvent(
	ProductDocument document
) {
}
