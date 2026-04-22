package com.yourorg.usermgmt.controller;

import com.yourorg.usermgmt.model.TenantDto;
import com.yourorg.usermgmt.service.DataStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/organization")
public class OrganizationController {

    private final DataStore dataStore;

    public OrganizationController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping("/details")
    public ResponseEntity<?> getDetails(@RequestParam(value = "tenant_id", required = false) String tenantId) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "tenant_id parameter required"));
        }
        return dataStore.findTenantById(tenantId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/exists/{tenantId}")
    public ResponseEntity<Map<String, Boolean>> exists(@PathVariable String tenantId) {
        return ResponseEntity.ok(Map.of("exists", dataStore.tenantExists(tenantId)));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "Organization registered (sample)"));
    }
}
