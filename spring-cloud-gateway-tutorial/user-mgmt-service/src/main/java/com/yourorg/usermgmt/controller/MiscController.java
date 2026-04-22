package com.yourorg.usermgmt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class MiscController {

    @GetMapping("/applications")
    public ResponseEntity<List<Map<String, String>>> applications() {
        return ResponseEntity.ok(List.of(
                Map.of("code", "user-mgmt", "name", "User Management"),
                Map.of("code", "emp-mgmt", "name", "Employee Management"),
                Map.of("code", "payroll-mgmt", "name", "Payroll Management")
        ));
    }

    @GetMapping("/permission-categories")
    public ResponseEntity<List<Map<String, String>>> permissionCategories() {
        return ResponseEntity.ok(List.of(
                Map.of("code", "user", "name", "User Permissions"),
                Map.of("code", "admin", "name", "Admin Permissions")
        ));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Map<String, String>>> permissions() {
        return ResponseEntity.ok(List.of(
                Map.of("code", "READ", "name", "Read"),
                Map.of("code", "WRITE", "name", "Write"),
                Map.of("code", "DELETE", "name", "Delete")
        ));
    }

    @GetMapping("/application/{appCode}/resource-types")
    public ResponseEntity<List<Map<String, String>>> resourceTypes(@PathVariable String appCode) {
        return ResponseEntity.ok(List.of(
                Map.of("code", "document", "name", "Document"),
                Map.of("code", "record", "name", "Record")
        ));
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        return ResponseEntity.ok(Map.of("version", "1.0.0", "service", "user-mgmt"));
    }

    @GetMapping("/user-access-control/app-permissions")
    public ResponseEntity<List<Map<String, String>>> appPermissions() {
        return ResponseEntity.ok(List.of(
                Map.of("app", "user-mgmt", "permission", "ADMIN"),
                Map.of("app", "emp-mgmt", "permission", "USER")
        ));
    }

    @GetMapping("/user-access-control/resource-ids")
    public ResponseEntity<List<String>> resourceIds(
            @RequestParam("user_id") String userId,
            @RequestParam("app_id") String appId,
            @RequestParam("resource_type") String resourceType,
            @RequestParam("permission") String permission) {
        return ResponseEntity.ok(List.of("resource-1", "resource-2"));
    }

    @GetMapping("/user-access-control/has-access")
    public ResponseEntity<Boolean> hasAccess(
            @RequestParam("user_id") String userId,
            @RequestParam("resource_id") String resourceId,
            @RequestParam("permission") String permission) {
        return ResponseEntity.ok(true);
    }

    @GetMapping("/org-group/list")
    public ResponseEntity<List<Map<String, String>>> orgGroups() {
        return ResponseEntity.ok(List.of(Map.of("id", "group-1", "name", "Administrators")));
    }

    @PostMapping("/org-group")
    public ResponseEntity<Map<String, String>> createOrgGroup(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "Group created"));
    }

    @GetMapping("/org-group/{resourceId}")
    public ResponseEntity<Map<String, String>> getOrgGroup(@PathVariable String resourceId) {
        return ResponseEntity.ok(Map.of("id", resourceId, "name", "Sample Group"));
    }

    @PutMapping("/org-group/{resourceId}")
    public ResponseEntity<Map<String, String>> updateOrgGroup(@PathVariable String resourceId) {
        return ResponseEntity.ok(Map.of("message", "Group updated"));
    }

    @DeleteMapping("/org-group/{resourceId}")
    public ResponseEntity<Map<String, String>> deleteOrgGroup(@PathVariable String resourceId) {
        return ResponseEntity.ok(Map.of("message", "Group deleted"));
    }

    @GetMapping("/org-policy/list")
    public ResponseEntity<List<Map<String, String>>> orgPolicies() {
        return ResponseEntity.ok(List.of(Map.of("id", "policy-1", "name", "Default Policy")));
    }

    @PostMapping("/org-policy")
    public ResponseEntity<Map<String, String>> createOrgPolicy(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("message", "Policy created"));
    }

    @GetMapping("/org-policy/{resourceId}")
    public ResponseEntity<Map<String, String>> getOrgPolicy(@PathVariable String resourceId) {
        return ResponseEntity.ok(Map.of("id", resourceId, "name", "Sample Policy"));
    }

    @DeleteMapping("/org-policy/{resourceId}")
    public ResponseEntity<Map<String, String>> deleteOrgPolicy(@PathVariable String resourceId) {
        return ResponseEntity.ok(Map.of("message", "Policy deleted"));
    }
}
