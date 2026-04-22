package com.yourorg.usermgmt.controller;

import com.yourorg.usermgmt.model.LoginRequest;
import com.yourorg.usermgmt.model.LoginResponse;
import com.yourorg.usermgmt.model.UserDto;
import com.yourorg.usermgmt.service.DataStore;
import com.yourorg.usermgmt.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/internal-auth")
public class AuthController {

    private final DataStore dataStore;
    private final JwtService jwtService;

    public AuthController(DataStore dataStore, JwtService jwtService) {
        this.dataStore = dataStore;
        this.jwtService = jwtService;
    }

    @GetMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiate() {
        return ResponseEntity.ok(Map.of(
                "auth_mode", "internal",
                "status", "ready"
        ));
    }

    @PostMapping("/identify")
    public ResponseEntity<Map<String, Object>> identify(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return dataStore.findUserByEmail(email)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "identified", true,
                        "tenant_id", user.tenantId(),
                        "auth_mode", "internal"
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "identified", false
                )));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Map<String, String> credentials = dataStore.getCredentials();
        UserDto user = dataStore.findUserByEmail(request.username()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "User not found"
            ));
        }

        String expectedPassword = credentials.get(request.username());
        if (expectedPassword == null || !expectedPassword.equals(request.password())) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "Invalid password"
            ));
        }

        if (request.tenantId() != null && !request.tenantId().equals(user.tenantId())) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "Tenant mismatch"
            ));
        }

        String token = jwtService.generateToken(
                user.userId(), user.userName(), user.phoneNumber(), user.tenantId());

        LoginResponse response = new LoginResponse(token, "Bearer", 900, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(Map.of("message", "Token refresh not implemented in sample service"));
    }

    @GetMapping("/logged-in-users")
    public ResponseEntity<Map<String, Object>> loggedInUsers() {
        return ResponseEntity.ok(Map.of("count", 0, "users", java.util.List.of()));
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> publicKey() {
        return ResponseEntity.ok(Map.of("key_type", "HMAC", "message", "HMAC keys are symmetric"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll() {
        return ResponseEntity.ok(Map.of("message", "All sessions logged out"));
    }
}
