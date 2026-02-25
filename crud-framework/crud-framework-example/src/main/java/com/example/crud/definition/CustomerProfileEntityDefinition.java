package com.example.crud.definition;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.PermissionConfig;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity definition for "customer_profile".
 * <p>
 * Demonstrates a <b>one-to-one</b> relationship with the {@code customer} entity.
 * Each customer has exactly one profile, linked via the {@code customer_id} foreign key.
 * <p>
 * To fetch the profile for a specific customer, use the standard GET operation with filters:
 * <pre>
 * {
 *   "entityType": "customer_profile",
 *   "operation": "GET",
 *   "filters": { "customer_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d" }
 * }
 * </pre>
 */
@Component
public class CustomerProfileEntityDefinition implements EntityDefinition<CustomerProfileEntityDefinition.CustomerProfile> {

    // ---- Entity POJO ----

    public static class CustomerProfile {
        private Long id;
        private String customerId;
        private String bio;
        private String avatarUrl;
        private String loyaltyTier;
        private Integer totalOrders;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getLoyaltyTier() { return loyaltyTier; }
        public void setLoyaltyTier(String loyaltyTier) { this.loyaltyTier = loyaltyTier; }
        public Integer getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    }

    // ---- Required methods ----

    @Override
    public String getEntityType() {
        return "customer_profile";
    }

    @Override
    public String getTableName() {
        return "customer_profiles";
    }

    @Override
    public Class<CustomerProfile> getEntityClass() {
        return CustomerProfile.class;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
                FieldDefinition.of("customerId", "customer_id", String.class)
                        .required(true)
                        .maxLength(36)
                        .displayName("Customer ID"),

                FieldDefinition.of("bio", "bio", String.class)
                        .maxLength(1000)
                        .displayName("Biography"),

                FieldDefinition.of("avatarUrl", "avatar_url", String.class)
                        .maxLength(500)
                        .displayName("Avatar URL"),

                FieldDefinition.of("loyaltyTier", "loyalty_tier", String.class)
                        .required(true)
                        .pattern("^(bronze|silver|gold|platinum)$")
                        .defaultValue("bronze")
                        .displayName("Loyalty Tier"),

                FieldDefinition.of("totalOrders", "total_orders", Integer.class)
                        .required(true)
                        .minValue(0)
                        .defaultValue("0")
                        .displayName("Total Orders")
        );
    }

    // ---- Optional overrides ----

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
                "summary", List.of("id", "customer_id", "loyalty_tier", "total_orders"),
                "full", List.of("id", "customer_id", "bio", "avatar_url", "loyalty_tier", "total_orders")
        );
    }

    @Override
    public PermissionConfig getPermissionConfig() {
        return PermissionConfig.builder()
                .listPermission("ListCustomerProfile")
                .getPermission("GetCustomerProfile")
                .createPermission("CreateCustomerProfile")
                .updatePermission("UpdateCustomerProfile")
                .deletePermission("DeleteCustomerProfile")
                .build();
    }

    @Override
    public Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.GET, CrudOperation.CREATE, CrudOperation.UPDATE, CrudOperation.DELETE);
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
                UniqueConstraint.of("customerId")
                        .withMessage("A profile already exists for this customer")
        );
    }

    @Override
    public ValidationResult validate(CrudOperation operation, CustomerProfile entity) {
        ValidationResult result = ValidationResult.builder();

        // Business rule: platinum tier requires at least 25 orders
        if ("platinum".equals(entity.getLoyaltyTier())
                && entity.getTotalOrders() != null && entity.getTotalOrders() < 25) {
            result.addError("loyaltyTier", "Platinum tier requires at least 25 total orders");
        }

        return result;
    }
}
