package com.framework.crud.event;

import com.framework.crud.model.CrudOperation;
import com.framework.crud.security.CrudSecurityContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes {@link CrudEvent} instances through Spring's event system.
 */
@Component
public class CrudEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final CrudSecurityContext securityContext;

    public CrudEventPublisher(ApplicationEventPublisher eventPublisher,
                              CrudSecurityContext securityContext) {
        this.eventPublisher = eventPublisher;
        this.securityContext = securityContext;
    }

    public void publishPre(String entityType, CrudOperation operation,
                           Object id, Map<String, Object> payload) {
        CrudEvent event = new CrudEvent(entityType, operation, CrudEvent.Phase.PRE,
                id, payload, securityContext.getCurrentUsername());
        eventPublisher.publishEvent(event);
    }

    public void publishPost(String entityType, CrudOperation operation,
                            Object id, Map<String, Object> payload) {
        CrudEvent event = new CrudEvent(entityType, operation, CrudEvent.Phase.POST,
                id, payload, securityContext.getCurrentUsername());
        eventPublisher.publishEvent(event);
    }
}
