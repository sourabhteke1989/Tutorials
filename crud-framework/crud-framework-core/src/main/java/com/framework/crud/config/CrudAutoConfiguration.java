package com.framework.crud.config;

import com.framework.crud.security.CrudSecurityContext;
import com.framework.crud.security.SpringSecurityCrudContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration for the CRUD Framework.
 * <p>
 * This class is registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so that any application that has {@code crud-framework-core} on its classpath
 * automatically gets all framework beans configured.
 * <p>
 * Beans provided with {@code @ConditionalOnMissingBean} can be overridden
 * by the application simply by declaring its own bean of the same type.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.framework.crud")
public class CrudAutoConfiguration {

    /**
     * Default security context backed by Spring Security.
     * Override by providing your own {@link CrudSecurityContext} bean.
     */
    @Bean
    @ConditionalOnMissingBean(CrudSecurityContext.class)
    public CrudSecurityContext crudSecurityContext() {
        return new SpringSecurityCrudContext();
    }
}
