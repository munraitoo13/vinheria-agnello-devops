package br.com.vinheria.production;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private static final String ISSUER = "vinheria-agnello";
    public static final long EXPIRATION_SECONDS = 3600L;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(@Value("${vinheria.jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(this.algorithm).withIssuer(ISSUER).build();
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(EXPIRATION_SECONDS))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
