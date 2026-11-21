package br.com.vinheria.production;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private record LabUser(String password, String role) {}

    // Lab demo accounts strictly for educational RBAC demonstration
    private static final Map<String, LabUser> LAB_USERS = Map.of(
            "enologo", new LabUser("safra2026", "ENOLOGO"),
            "cliente", new LabUser("vinho2026", "CLIENTE")
    );

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> issueToken(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");

        LabUser user = LAB_USERS.get(username);
        if (user == null || !user.password().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid credentials"));
        }

        String token = jwtService.generateToken(username, user.role());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "username", username,
                "role", user.role(),
                "expiresIn", JwtService.EXPIRATION_SECONDS
        ));
    }
}
