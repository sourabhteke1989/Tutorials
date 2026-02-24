package com.framework.crud.model;

/**
 * Metadata for a single field/column in an entity.
 * <p>
 * Used by the framework to:
 * <ul>
 *   <li>Map JSON payload keys to database column names</li>
 *   <li>Validate mandatory/optional fields on CREATE and UPDATE</li>
 *   <li>Enforce max-length, regex patterns, and type checks</li>
 *   <li>Control which fields are insertable/updatable</li>
 * </ul>
 */
public class FieldDefinition {

    /** Logical field name (used in JSON payload). */
    private String fieldName;

    /** Corresponding database column name. */
    private String columnName;

    /** Java type of the field. */
    private Class<?> fieldType;

    /** Whether this field is mandatory on CREATE. */
    private boolean required;

    /** Whether this field is mandatory on UPDATE. */
    private boolean requiredOnUpdate;

    /** Whether this field can be set during CREATE. */
    private boolean insertable;

    /** Whether this field can be modified during UPDATE. */
    private boolean updatable;

    /** Default value if not provided. */
    private Object defaultValue;

    /** Maximum string length (null = no limit). */
    private Integer maxLength;

    /** Minimum value for numeric fields (null = no limit). */
    private Number minValue;

    /** Maximum value for numeric fields (null = no limit). */
    private Number maxValue;

    /** Regex pattern the value must match (null = no pattern). */
    private String pattern;

    /** Human-readable description for error messages. */
    private String displayName;

    // ---- Constructors ----

    public FieldDefinition() {
    }

    // ---- Builder-style static factory ----

    public static FieldDefinition of(String fieldName, String columnName, Class<?> fieldType) {
        FieldDefinition fd = new FieldDefinition();
        fd.fieldName = fieldName;
        fd.columnName = columnName;
        fd.fieldType = fieldType;
        fd.required = false;
        fd.requiredOnUpdate = false;
        fd.insertable = true;
        fd.updatable = true;
        fd.displayName = fieldName;
        return fd;
    }

    public FieldDefinition required(boolean required) {
        this.required = required;
        return this;
    }

    public FieldDefinition requiredOnUpdate(boolean requiredOnUpdate) {
        this.requiredOnUpdate = requiredOnUpdate;
        return this;
    }

    public FieldDefinition insertable(boolean insertable) {
        this.insertable = insertable;
        return this;
    }

    public FieldDefinition updatable(boolean updatable) {
        this.updatable = updatable;
        return this;
    }

    public FieldDefinition defaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public FieldDefinition maxLength(Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public FieldDefinition minValue(Number minValue) {
        this.minValue = minValue;
        return this;
    }

    public FieldDefinition maxValue(Number maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public FieldDefinition pattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public FieldDefinition displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    // ---- Getters ----

    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isRequiredOnUpdate() {
        return requiredOnUpdate;
    }

    public boolean isInsertable() {
        return insertable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public Number getMinValue() {
        return minValue;
    }

    public Number getMaxValue() {
        return maxValue;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDisplayName() {
        return displayName;
    }
}
