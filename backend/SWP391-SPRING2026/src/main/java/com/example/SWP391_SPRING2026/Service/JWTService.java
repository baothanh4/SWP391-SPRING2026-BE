package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JWTService {
    private static final Logger log = LoggerFactory.getLogger(JWTService.class);

    @Value("${jwt.secret}")
    private String SECRET_KEY ;

    public String generateAccessToken(Users user){
        log.warn("🔐 [GEN] JWT_SECRET length = {}",
                SECRET_KEY == null ? "NULL" : SECRET_KEY.length());
        log.warn("🔐 [GEN] userId = {}", user.getId());
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("role",user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+1000*60*15))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String generateRefreshToken(Users user){

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+1000L*60*60*24*7))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .compact();

    }

    public Claims extractClaimsJws(String token) {
        log.warn("🔐 [VERIFY] JWT_SECRET length = {}",
                SECRET_KEY == null ? "NULL" : SECRET_KEY.length());
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(
                        SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
