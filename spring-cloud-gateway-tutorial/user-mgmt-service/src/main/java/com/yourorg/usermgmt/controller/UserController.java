package com.yourorg.usermgmt.controller;

import com.yourorg.usermgmt.model.UserDto;
import com.yourorg.usermgmt.service.DataStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final DataStore dataStore;

    public UserController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @GetMapping("/list")
    public ResponseEntity<List<UserDto>> listUsers(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId) {
        if (tenantId != null) {
            return ResponseEntity.ok(dataStore.findUsersByTenant(tenantId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "X-User-ID", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "X-User-ID header required"));
        }
        return dataStore.findUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<?> getUserById(@PathVariable String resourceId) {
        return dataStore.findUserById(resourceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "User created (sample)", "id", java.util.UUID.randomUUID().toString()));
    }

    @PutMapping("/{resourceId}")
    public ResponseEntity<Map<String, String>> updateUser(@PathVariable String resourceId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "User updated (sample)", "id", resourceId));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String resourceId) {
        return ResponseEntity.ok(Map.of("message", "User deleted (sample)", "id", resourceId));
    }

    @GetMapping("/by-username")
    public ResponseEntity<?> getUserByUsername(
            @RequestParam("username") String username,
            @RequestParam(value = "tenant_id", required = false) String tenantId) {
        return dataStore.findUserByEmail(username)
                .filter(u -> tenantId == null || tenantId.equals(u.tenantId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
