package com.veterinaria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        // TODO: Implement JWT authentication when user management is added
        return ResponseEntity.ok(Map.of(
            "mensaje", "Endpoint de login preparado - pendiente implementación",
            "token", "placeholder-jwt-token"
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request) {
        // TODO: Implement user registration when user management is added
        return ResponseEntity.ok(Map.of(
            "mensaje", "Endpoint de registro preparado - pendiente implementación"
        ));
    }
}
