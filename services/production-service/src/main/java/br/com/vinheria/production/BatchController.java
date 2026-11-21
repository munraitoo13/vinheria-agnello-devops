package br.com.vinheria.production;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final Map<String, Map<String, Object>> batches = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(2);
    private final RestClient restClient;
    private final String trackingUrl;

    public BatchController(
            RestClient restClient,
            @Value("${vinheria.tracking.url}") String trackingUrl
    ) {
        this.restClient = restClient;
        this.trackingUrl = trackingUrl;

        Map<String, Object> seed = new LinkedHashMap<>();
        seed.put("batchCode", "LOTE-2026-001");
        seed.put("variety", "Cabernet Sauvignon");
        seed.put("harvestYear", 2026);
        seed.put("volumeLiters", 5000);
        seed.put("status", "ENGARRAFADO");
        seed.put("trackingCode", "AGN-2026-001");
        seed.put("createdBy", "enologo");
        seed.put("createdAt", "2026-11-21T18:00:00Z");
        batches.put("LOTE-2026-001", seed);
    }

    @GetMapping
    public Map<String, Object> listBatches(HttpServletRequest request) {
        return Map.of(
                "service", "production-service",
                "user", request.getAttribute("auth.user"),
                "role", request.getAttribute("auth.role"),
                "count", batches.size(),
                "batches", batches.values()
        );
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBatch(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request
    ) {
        String role = String.valueOf(request.getAttribute("auth.role"));
        if (!"ENOLOGO".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Forbidden",
                    "message", "Role ENOLOGO required to close and register production batches"
            ));
        }

        int next = sequence.getAndIncrement();
        String batchCode = payload.containsKey("batchCode")
                ? String.valueOf(payload.get("batchCode"))
                : String.format("LOTE-2026-%03d", next);
        String variety = String.valueOf(payload.getOrDefault("variety", "Merlot Reserva"));
        int harvestYear = Integer.parseInt(String.valueOf(payload.getOrDefault("harvestYear", 2026)));
        int volumeLiters = Integer.parseInt(String.valueOf(payload.getOrDefault("volumeLiters", 3200)));

        String bearerToken = String.valueOf(request.getAttribute("auth.token"));
        Map<String, Object> trackingRequest = Map.of(
                "batchCode", batchCode,
                "variety", variety,
                "harvestYear", harvestYear,
                "origin", "Serra Gaucha - Vinheria Agnello"
        );

        Map<String, Object> trackingResponse = restClient.post()
                .uri(trackingUrl + "/api/tracking")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(trackingRequest)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        String trackingCode = trackingResponse != null
                ? String.valueOf(trackingResponse.get("trackingCode"))
                : "PENDING";

        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("batchCode", batchCode);
        batch.put("variety", variety);
        batch.put("harvestYear", harvestYear);
        batch.put("volumeLiters", volumeLiters);
        batch.put("status", "FECHADO_PARA_RASTREAMENTO");
        batch.put("trackingCode", trackingCode);
        batch.put("createdBy", request.getAttribute("auth.user"));
        batch.put("createdAt", Instant.now().toString());
        batches.put(batchCode, batch);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Batch created and registered in tracking-service via internal DNS",
                "resolvedTrackingUrl", trackingUrl,
                "batch", batch,
                "tracking", trackingResponse != null ? trackingResponse : Map.of()
        ));
    }
}
