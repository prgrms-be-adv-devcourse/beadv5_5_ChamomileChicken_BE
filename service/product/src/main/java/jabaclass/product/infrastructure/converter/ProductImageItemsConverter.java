package jabaclass.product.infrastructure.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jabaclass.product.domain.model.ProductImageItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;

@Converter
public class ProductImageItemsConverter
        implements AttributeConverter<List<ProductImageItem>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(List<ProductImageItem> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        return objectMapper.writeValueAsString(attribute);
    }

    @Override
    @SneakyThrows
    public List<ProductImageItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        return objectMapper.readValue(dbData,
                new TypeReference<List<ProductImageItem>>() {});
    }
}
