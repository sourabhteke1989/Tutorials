package com.example.crud.definition;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.IdType;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entity definition for "customer".
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>UUID-based primary key (instead of auto-increment)</li>
 *   <li>Email pattern validation</li>
 *   <li>Read-only fields (createdAt not updatable)</li>
 *   <li>Custom cross-field validation</li>
 *   <li>Different projection types</li>
 * </ul>
 */
@Component
public class CustomerEntityDefinition implements EntityDefinition<CustomerEntityDefinition.Customer> {

    // ---- Entity POJO ----

    public static class Customer {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String country;
        private String status;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // ---- Required methods ----

    @Override
    public String getEntityType() {
        return "customer";
    }

    @Override
    public String getTableName() {
        return "customers";
    }

    @Override
    public Class<Customer> getEntityClass() {
        return Customer.class;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
                FieldDefinition.of("firstName", "first_name", String.class)
                        .required(true)
                        .maxLength(100)
                        .displayName("First Name"),

                FieldDefinition.of("lastName", "last_name", String.class)
                        .required(true)
                        .maxLength(100)
                        .displayName("Last Name"),

                FieldDefinition.of("email", "email", String.class)
                        .required(true)
                        .maxLength(255)
                        .pattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                        .displayName("Email"),

                FieldDefinition.of("phone", "phone", String.class)
                        .maxLength(20)
                        .pattern("^\\+?[0-9\\-\\s]{7,20}$")
                        .displayName("Phone"),

                FieldDefinition.of("address", "address", String.class)
                        .maxLength(500)
                        .displayName("Address"),

                FieldDefinition.of("city", "city", String.class)
                        .maxLength(100)
                        .displayName("City"),

                FieldDefinition.of("country", "country", String.class)
                        .required(true)
                        .maxLength(100)
                        .displayName("Country"),

                FieldDefinition.of("status", "status", String.class)
                        .required(true)
                        .pattern("^(active|inactive|suspended)$")
                        .defaultValue("active")
                        .displayName("Status")
        );
    }

    // ---- Optional overrides ----

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
                "summary", List.of("id", "first_name", "last_name", "email", "status"),
                "contact", List.of("id", "first_name", "last_name", "email", "phone"),
                "full", List.of("id", "first_name", "last_name", "email", "phone",
                        "address", "city", "country", "status")
        );
    }

    @Override
    public Map<CrudOperation, String> getRequiredPermissions() {
        return Map.of(
                CrudOperation.GET, "customer:read",
                CrudOperation.CREATE, "customer:write",
                CrudOperation.UPDATE, "customer:write",
                CrudOperation.DELETE, "customer:admin"
        );
    }

    @Override
    public Set<CrudOperation> getAllowedOperations() {
        return Set.of(CrudOperation.GET, CrudOperation.CREATE, CrudOperation.UPDATE, CrudOperation.DELETE);
    }

    @Override
    public IdType getIdType() {
        return IdType.UUID;
    }

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
                UniqueConstraint.of("email")
                        .withMessage("A customer with this email address already exists")
        );
    }

    @Override
    public ValidationResult validate(CrudOperation operation, Customer entity) {
        ValidationResult result = ValidationResult.builder();

        // Business rule: suspended customers must have a phone number
        if ("suspended".equals(entity.getStatus())
                && (entity.getPhone() == null || entity.getPhone().isBlank())) {
            result.addError("phone", "Phone is required for suspended customers (for contact)");
        }

        return result;
    }
}
