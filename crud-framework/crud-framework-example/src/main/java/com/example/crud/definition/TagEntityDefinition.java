package com.example.crud.definition;

import com.framework.crud.definition.EntityDefinition;
import com.framework.crud.model.CrudOperation;
import com.framework.crud.model.FieldDefinition;
import com.framework.crud.model.ManyToManyRelation;
import com.framework.crud.model.UniqueConstraint;
import com.framework.crud.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Entity definition for "tag".
 * <p>
 * Tags can be associated with products via a many-to-many junction table
 * ({@code product_tags}). This demonstrates:
 * <ul>
 *   <li>A simple entity with auto-increment ID</li>
 *   <li>Uniqueness on tag name</li>
 *   <li>Many-to-many relation — Tag → Product (reverse side)</li>
 * </ul>
 */
@Component
public class TagEntityDefinition implements EntityDefinition<TagEntityDefinition.Tag> {

    // ---- Entity POJO ----

    public static class Tag {
        private Long id;
        private String name;
        private String color;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    // ---- Required methods ----

    @Override
    public String getEntityType() {
        return "tag";
    }

    @Override
    public String getTableName() {
        return "tags";
    }

    @Override
    public Class<Tag> getEntityClass() {
        return Tag.class;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        return List.of(
                FieldDefinition.of("name", "name", String.class)
                        .required(true)
                        .maxLength(100)
                        .displayName("Tag Name"),

                FieldDefinition.of("color", "color", String.class)
                        .maxLength(7)
                        .pattern("^#[0-9A-Fa-f]{6}$")
                        .displayName("Hex Color")
        );
    }

    // ---- Optional overrides ----

    @Override
    public List<UniqueConstraint> getUniqueConstraints() {
        return List.of(
                UniqueConstraint.of("name").withMessage("A tag with this name already exists")
        );
    }

    @Override
    public Map<String, List<String>> getProjectionTypes() {
        return Map.of(
                "summary", List.of("id", "name"),
                "full",    List.of("id", "name", "color")
        );
    }

    @Override
    public Map<CrudOperation, String> getRequiredPermissions() {
        return Map.of(
                CrudOperation.GET,    "product:read",
                CrudOperation.CREATE, "product:write",
                CrudOperation.UPDATE, "product:write",
                CrudOperation.DELETE, "product:admin"
        );
    }

    @Override
    public List<ManyToManyRelation> getManyToManyRelations() {
        return List.of(
                // Reverse side: from a tag, find all products that have this tag
                ManyToManyRelation.builder()
                        .relationName("products")
                        .targetEntityType("product")
                        .junctionTable("product_tags")
                        .sourceJoinColumn("tag_id")
                        .targetJoinColumn("product_id")
                        .build()
        );
    }

    @Override
    public ValidationResult validate(CrudOperation operation, Tag entity) {
        return ValidationResult.success();
    }
}
