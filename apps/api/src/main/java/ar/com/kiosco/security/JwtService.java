package ar.com.kiosco.security;

import ar.com.kiosco.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation with multi-tenant support.
 */
@Service
public class JwtService {

    private static final String ISSUER = "kiosco";
    private static final String CLAIM_KIOSCO_ID = "kiosco_id";
    private static final String CLAIM_KIOSCO_ROLE = "kiosco_role";
    private static final String CLAIM_USER_ID = "user_id";
    private static final String CLAIM_USER_NAME = "user_name";

    @Value("${jwt.secret:kiosco-secret-key-that-should-be-changed-in-production-environment-for-security}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration; // 24 hours default

    /**
     * Generates a JWT token with multi-tenant context.
     */
    public String generateToken(Usuario usuario, UUID kioscoId, String kioscoRole) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, usuario.getId().toString());
        claims.put(CLAIM_USER_NAME, usuario.getNombre());

        if (kioscoId != null) {
            claims.put(CLAIM_KIOSCO_ID, kioscoId.toString());
        }
        if (kioscoRole != null) {
            claims.put(CLAIM_KIOSCO_ROLE, kioscoRole);
        }

        return buildToken(claims, usuario.getEmail());
    }

    /**
     * Generates a simple token without kiosco context (for account selection).
     */
    public String generateAccountToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, usuario.getId().toString());
        claims.put(CLAIM_USER_NAME, usuario.getNombre());
        return buildToken(claims, usuario.getEmail());
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(ISSUER)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get(CLAIM_USER_ID, String.class));
        return userId != null ? UUID.fromString(userId) : null;
    }

    public UUID extractKioscoId(String token) {
        String kioscoId = extractClaim(token, claims -> claims.get(CLAIM_KIOSCO_ID, String.class));
        return kioscoId != null ? UUID.fromString(kioscoId) : null;
    }

    public String extractKioscoRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_KIOSCO_ROLE, String.class));
    }

    public String extractUserName(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_NAME, String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            return tokenEmail.equals(email) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // Ensure key is at least 512 bits (64 bytes) for HS512, or at least 256 bits for HS256
        String key = secretKey;
        while (key.length() < 64) {
            key = key + key;
        }
        byte[] keyBytes = key.substring(0, 64).getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
