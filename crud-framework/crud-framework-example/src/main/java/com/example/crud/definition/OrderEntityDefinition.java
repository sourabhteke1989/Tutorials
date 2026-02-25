package com.example.crud.definition;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.FilterPermission;
import com.framework.crud.model.PermissionConfig;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity definition for "order".
 * <p>
 * Demonstrates a <b>one-to-many</b> relationship with the {@code customer} entity.
 * Each customer can have multiple orders, linked via the {@code customer_id} foreign key.
 * <p>
 * To fetch all orders for a specific customer, use the standard GET operation with filters:
 * <pre>
 * {
 *   "entityType": "order",
 *   "operation": "GET",
 *   "filters": { "customer_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d" },
 *   "sortBy": "created_at",
 *   "sortDirection": "DESC"
 * }
 * </pre>
 */
@Component
public class OrderEntityDefinition implements EntityDefinition<OrderEntityDefinition.Order> {

    // ---- Entity POJO ----

    public static class Order {
        private Long id;
        private String customerId;
        private String orderNumber;
        private Double totalAmount;
        private String status;
        private String shippingAddress;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    }

    // ---- Required methods ----

    @Override
    public String getEntityType() {
        return "order";
    }

    @Override
    public String getTableName() {
        return "orders";
    }

    @Override
    public Class<Order> getEntityClass() {
        return Order.class;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
                FieldDefinition.of("customerId", "customer_id", String.class)
                        .required(true)
                        .maxLength(36)
                        .displayName("Customer ID"),

                FieldDefinition.of("orderNumber", "order_number", String.class)
                        .required(true)
                        .maxLength(50)
                        .displayName("Order Number"),

                FieldDefinition.of("totalAmount", "total_amount", Double.class)
                        .required(true)
                        .minValue(0.01)
                        .maxValue(999999.99)
                        .displayName("Total Amount"),

                FieldDefinition.of("status", "status", String.class)
                        .required(true)
                        .pattern("^(pending|confirmed|shipped|delivered|cancelled)$")
                        .defaultValue("pending")
                        .displayName("Order Status"),

                FieldDefinition.of("shippingAddress", "shipping_address", String.class)
                        .maxLength(500)
                        .displayName("Shipping Address")
        );
    }

    // ---- Optional overrides ----

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
                "summary", List.of("id", "customer_id", "order_number", "total_amount", "status"),
                "full", List.of("id", "customer_id", "order_number", "total_amount", "status",
                        "shipping_address", "created_at", "updated_at")
        );
    }

    @Override
    public PermissionConfig getPermissionConfig() {
        return PermissionConfig.builder()
                .listPermission("ListOrder")
                .getPermission("GetOrder")
                .createPermission("CreateOrder")
                .updatePermission("UpdateOrder")
                .deletePermission("DeleteOrder")
                .filterPermission(
                        FilterPermission.of(Set.of("customer_id"), "ListCustomerOrders")
                                .description("List orders for a specific customer")
                )
                .build();
    }

    @Override
    public Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.GET, CrudOperation.CREATE, CrudOperation.UPDATE, CrudOperation.DELETE);
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
                UniqueConstraint.of("orderNumber")
                        .withMessage("An order with this order number already exists")
        );
    }

    @Override
    public ValidationResult validate(CrudOperation operation, Order entity) {
        ValidationResult result = ValidationResult.builder();

        // Business rule: cancelled orders cannot have a total above 0
        if ("cancelled".equals(entity.getStatus())
                && entity.getTotalAmount() != null && entity.getTotalAmount() > 0) {
            result.addError("totalAmount", "Cancelled orders must have a total amount of 0");
        }

        return result;
    }

    @Override
    public void beforeSave(CrudOperation operation, Map<String, Object> payload) {
        // Auto-uppercase order number
        if (payload.containsKey("orderNumber") && payload.get("orderNumber") instanceof String orderNum) {
            payload.put("orderNumber", orderNum.toUpperCase());
        }
    }
}
