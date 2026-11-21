package br.com.vinheria.tracking;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ISSUER = "vinheria-agnello";
    private final JWTVerifier verifier;

    public JwtService(@Value("${vinheria.jwt.secret}") String secret) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
