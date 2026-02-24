package com.example.crud.definition;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity definition for "product".
 * <p>
 * This is the <b>only class</b> a developer writes per entity.
 * Everything else (controller, validation, SQL, RBAC) is handled by the framework.
 */
@Component
public class ProductEntityDefinition implements EntityDefinition<ProductEntityDefinition.Product> {

    // ---- Entity POJO ----

    public static class Product {
        private Long id;
        private String name;
        private String description;
        private Double price;
        private String category;
        private String status;
        private Integer stockQuantity;

        // Getters & setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    }

    // ---- Required methods ----

    @Override
    public String getEntityType() {
        return "product";
    }

    @Override
    public String getTableName() {
        return "products";
    }

    @Override
    public Class<Product> getEntityClass() {
        return Product.class;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
                FieldDefinition.of("name", "name", String.class)
                        .required(true)
                        .maxLength(200)
                        .displayName("Product Name"),

                FieldDefinition.of("description", "description", String.class)
                        .maxLength(2000)
                        .displayName("Description"),

                FieldDefinition.of("price", "price", Double.class)
                        .required(true)
                        .minValue(0.01)
                        .maxValue(999999.99)
                        .displayName("Price"),

                FieldDefinition.of("category", "category", String.class)
                        .required(true)
                        .maxLength(100)
                        .displayName("Category"),

                FieldDefinition.of("status", "status", String.class)
                        .required(true)
                        .pattern("^(active|inactive|discontinued)$")
                        .defaultValue("active")
                        .displayName("Status"),

                FieldDefinition.of("stockQuantity", "stock_quantity", Integer.class)
                        .required(true)
                        .minValue(0)
                        .displayName("Stock Quantity")
        );
    }

    // ---- Optional overrides ----

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
                "summary", List.of("id", "name", "price", "status"),
                "detail", List.of("id", "name", "description", "price", "category", "status", "stock_quantity"),
                "inventory", List.of("id", "name", "stock_quantity", "status")
        );
    }

    @Override
    public Map<CrudOperation, String> getRequiredPermissions() {
        return Map.of(
                CrudOperation.GET, "product:read",
                CrudOperation.CREATE, "product:write",
                CrudOperation.UPDATE, "product:write",
                CrudOperation.DELETE, "product:admin"
        );
    }

    @Override
    public Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.GET, CrudOperation.CREATE, CrudOperation.UPDATE, CrudOperation.DELETE);
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
                UniqueConstraint.of("name", "category")
                        .withMessage("A product with this name already exists in this category")
        );
    }

    @Override
    public boolean isSoftDeleteEnabled() {
        return true;
    }

    @Override
    public String getSoftDeleteColumn() {
        return "deleted";
    }

    @Override
    public ValidationResult validate(CrudOperation operation, Product entity) {
        ValidationResult result = ValidationResult.builder();

        // Business rule: discounted products cannot have price > 500
        if ("discontinued".equals(entity.getStatus()) && entity.getPrice() != null && entity.getPrice() > 500) {
            result.addError("price", "Discontinued products cannot have a price greater than 500");
        }

        // Business rule: stock must be 0 for discontinued products
        if ("discontinued".equals(entity.getStatus())
                && entity.getStockQuantity() != null && entity.getStockQuantity() > 0) {
            result.addError("stockQuantity", "Discontinued products must have zero stock");
        }

        return result;
    }

    @Override
    public void beforeSave(CrudOperation operation, Map<String, Object> payload) {
        // Example: trim the product name
        if (payload.containsKey("name") && payload.get("name") instanceof String name) {
            payload.put("name", name.trim());
        }
    }
}
