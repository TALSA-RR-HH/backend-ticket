package com.talsa.rrhh.backend.security;

import com.talsa.rrhh.backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // ESTA CLAVE DEBE SER SECRETA Y LARGA (Mínimo 256 bits para HS256)
    // En producción, esto va en el application.properties, no aquí.
    private static final String SECRET_KEY = "TalsaRRHH2026_ClaveSuperSecretaParaFirmarTokens_SeguridadTotal";

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(SECRET_KEY.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generar Token (Ahora con datos extra)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Verificamos que userDetails sea de nuestra clase Usuario
        if (userDetails instanceof Usuario) {
            Usuario u = (Usuario) userDetails;
            claims.put("nombre", u.getNombre());
            claims.put("apellidos", u.getApellidos());
            claims.put("rol", u.getRol().toString());
            // Puedes agregar más cosas si quieres, como el ID
            claims.put("idUsuario", u.getId());
        }

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 Horas de validez
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    // Validar Token
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Extraer Username del Token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Métodos auxiliares
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
