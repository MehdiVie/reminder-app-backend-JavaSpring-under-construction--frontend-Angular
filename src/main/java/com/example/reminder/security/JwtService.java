package com.example.reminder.security;

import com.example.reminder.exception.BadRequestException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret864;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    private JwtParser jwtParser;
    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret864.getBytes(StandardCharsets.UTF_8);
        signingKey = Keys.hmacShaKeyFor(keyBytes);

        jwtParser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(30)
                .build();
    }

    public Claims parseClaim(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    // if token is not valid then Exception
    public  String extractUsername(String token) {
        return parseClaim(token).getSubject();
    }

    public String generateToken(String email, Collection<String> roles) {
        if (email == null || email.isBlank()) {
            throw  new BadRequestException("username(email) required");
        }
        Collection<String> safeRoles = (roles == null) ? List.of() : roles;
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("roles" , safeRoles)
                .setIssuer("myapp")
                .setIssuedAt(now)
                .setExpiration(exp) // after 24 hours
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }


    public boolean isTokenValid(String token, String email) {
        try {
            Claims claims = parseClaim(token);
            // check Issuer
            if (!"myapp".equals(claims.getIssuer())) return false;
            return email.equals(claims.getSubject());
        } catch (JwtException ex) {
            return false;
        }
    }


}
