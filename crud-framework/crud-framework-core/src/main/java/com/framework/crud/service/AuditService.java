package com.framework.crud.service;

import com.framework.crud.model.CrudOperation;
import com.framework.crud.security.CrudSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Simple audit logging service.
 * <p>
 * Logs every CRUD operation with entity type, operation, user, and ID.
 * Replace this bean with your own implementation to write audit records
 * to a database table, message queue, or external audit system.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final CrudSecurityContext securityContext;

    public AuditService(CrudSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    /**
     * Record an audit entry for a completed operation.
     */
    public void audit(String entityType, CrudOperation operation, Object id,
                      Map<String, Object> payload) {
        String username = securityContext.getCurrentUsername();
        log.info("AUDIT | user={} | entity={} | operation={} | id={} | payloadKeys={}",
                username,
                entityType,
                operation,
                id,
                payload != null ? payload.keySet() : "N/A");
    }
}
