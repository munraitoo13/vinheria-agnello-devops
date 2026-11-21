package br.com.vinheria.tracking;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final Map<String, Map<String, Object>> records = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(2);

    public TrackingController() {
        Map<String, Object> seed = new LinkedHashMap<>();
        seed.put("trackingCode", "AGN-2026-001");
        seed.put("batchCode", "LOTE-2026-001");
        seed.put("variety", "Cabernet Sauvignon");
        seed.put("harvestYear", 2026);
        seed.put("origin", "Serra Gaucha - Vinheria Agnello");
        seed.put("qrUrl", "https://api.vinheria.local/api/tracking/AGN-2026-001");
        seed.put("registeredBy", "enologo");
        seed.put("registeredAt", "2026-11-21T18:00:00Z");
        records.put("AGN-2026-001", seed);
    }

    @GetMapping
    public Map<String, Object> listTracking(HttpServletRequest request) {
        return Map.of(
                "service", "tracking-service",
                "user", request.getAttribute("auth.user"),
                "role", request.getAttribute("auth.role"),
                "count", records.size(),
                "records", records.values()
        );
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getByCode(
            @PathVariable String code,
            HttpServletRequest request
    ) {
        Map<String, Object> found = records.get(code);
        if (found == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Not Found",
                    "message", "Tracking code not found: " + code
            ));
        }

        Map<String, Object> response = new LinkedHashMap<>(found);
        response.put("consultedBy", request.getAttribute("auth.user"));
        response.put("consultedRole", request.getAttribute("auth.role"));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerTracking(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        String role = String.valueOf(request.getAttribute("auth.role"));
        if (!"ENOLOGO".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Forbidden",
                    "message", "Role ENOLOGO required to register wine lineage"
            ));
        }

        String trackingCode = String.format("AGN-2026-%03d", sequence.getAndIncrement());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("trackingCode", trackingCode);
        record.put("batchCode", payload.getOrDefault("batchCode", "N/A"));
        record.put("variety", payload.getOrDefault("variety", "N/A"));
        record.put("harvestYear", payload.getOrDefault("harvestYear", 2026));
        record.put("origin", payload.getOrDefault("origin", "Serra Gaucha - Vinheria Agnello"));
        record.put("qrUrl", "https://api.vinheria.local/api/tracking/" + trackingCode);
        record.put("registeredBy", request.getAttribute("auth.user"));
        record.put("registeredAt", Instant.now().toString());
        records.put(trackingCode, record);

        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }
}
