package com.framework.crud.validation;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.exception.CrudValidationException;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates the request payload against the entity's field definitions and
 * then delegates to the developer's custom {@code EntityDefinition.validate()} method.
 * <p>
 * Built-in checks:
 * <ul>
 *   <li>Required fields present and non-null</li>
 *   <li>No unknown fields (optional strict mode)</li>
 *   <li>String max-length</li>
 *   <li>Numeric min/max value</li>
 *   <li>Regex pattern matching</li>
 *   <li>Field insertability / updatability</li>
 * </ul>
 */
@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private final ObjectMapper objectMapper;

    public ValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Run full validation pipeline:
     * <ol>
     *   <li>Structural validation based on FieldDefinitions</li>
     *   <li>Custom validation via EntityDefinition.validate()</li>
     * </ol>
     *
     * @throws CrudValidationException if any errors are found
     */
    @SuppressWarnings("unchecked")
    public <T> void validate(EntityDefinition<T> definition, CrudOperation operation,
                             Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            if (operation == CrudOperation.CREATE) {
                throw new CrudValidationException(
                        "Payload is required for CREATE operation",
                        Map.of("payload", "Payload must not be empty"));
            }
            if (operation == CrudOperation.UPDATE) {
                throw new CrudValidationException(
                        "Payload is required for UPDATE operation",
                        Map.of("payload", "Payload must not be empty"));
            }
            return; // GET/DELETE don't need payload validation
        }

        List<FieldDefinition> fields = definition.getFieldDefinitions();
        ValidationResult result = ValidationResult.builder();

        // ---- 1. Structural validation ----
        for (FieldDefinition field : fields) {
            String fieldName = field.getFieldName();
            Object value = payload.get(fieldName);
            boolean present = payload.containsKey(fieldName) && value != null;

            // Required check
            if (operation == CrudOperation.CREATE && field.isRequired() && !present) {
                result.addError(fieldName,
                        field.getDisplayName() + " is required");
                continue;
            }
            if (operation == CrudOperation.UPDATE && field.isRequiredOnUpdate() && !present) {
                result.addError(fieldName,
                        field.getDisplayName() + " is required for update");
                continue;
            }

            // Insertable / Updatable check
            if (operation == CrudOperation.CREATE && !field.isInsertable() && present) {
                result.addError(fieldName,
                        field.getDisplayName() + " cannot be set on create");
                continue;
            }
            if (operation == CrudOperation.UPDATE && !field.isUpdatable() && present) {
                result.addError(fieldName,
                        field.getDisplayName() + " cannot be modified");
                continue;
            }

            // If value is not present, skip further checks
            if (!present) {
                continue;
            }

            // Max-length check (for String values)
            if (field.getMaxLength() != null && value instanceof String str) {
                if (str.length() > field.getMaxLength()) {
                    result.addError(fieldName,
                            field.getDisplayName() + " exceeds maximum length of " + field.getMaxLength());
                }
            }

            // Numeric range checks
            if (field.getMinValue() != null && value instanceof Number num) {
                if (num.doubleValue() < field.getMinValue().doubleValue()) {
                    result.addError(fieldName,
                            field.getDisplayName() + " must be >= " + field.getMinValue());
                }
            }
            if (field.getMaxValue() != null && value instanceof Number num) {
                if (num.doubleValue() > field.getMaxValue().doubleValue()) {
                    result.addError(fieldName,
                            field.getDisplayName() + " must be <= " + field.getMaxValue());
                }
            }

            // Regex pattern check (for String values)
            if (field.getPattern() != null && value instanceof String str) {
                if (!Pattern.matches(field.getPattern(), str)) {
                    result.addError(fieldName,
                            field.getDisplayName() + " does not match required pattern");
                }
            }
        }

        // ---- 2. Check for unknown fields ----
        for (String payloadKey : payload.keySet()) {
            boolean known = fields.stream()
                    .anyMatch(f -> f.getFieldName().equals(payloadKey));
            if (!known) {
                result.addError(payloadKey, "Unknown field: " + payloadKey);
            }
        }

        // ---- 3. Custom validation from EntityDefinition ----
        if (result.isValid()) {
            try {
                T entity = objectMapper.convertValue(payload, definition.getEntityClass());
                ValidationResult customResult = definition.validate(operation, entity);
                result.merge(customResult);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to convert payload to entity class {}: {}",
                        definition.getEntityClass().getSimpleName(), e.getMessage());
                result.addError("payload",
                        "Payload cannot be converted to " + definition.getEntityClass().getSimpleName()
                                + ": " + e.getMessage());
            }
        }

        // ---- 4. Throw if invalid ----
        if (!result.isValid()) {
            throw new CrudValidationException(result.getErrors());
        }

        log.debug("Validation passed for {} on entity '{}'", operation, definition.getEntityType());
    }
}
