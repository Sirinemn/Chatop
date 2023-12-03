package com.openclassrooms.jwt;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;


@Component
public class TokenProvider {
	
	private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    @Value("${app.secret}")
    private String secretKey;

    @Value("${app.tokenValidityInMilliseconds}")
    private long tokenValidityInMilliseconds;


    @SuppressWarnings("deprecation")
	public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

       return Jwts.builder()
    		   .subject(authentication.getName())
    		   .claim("Role", authorities)
    		   .signWith(getSigningKey())
    		   .setExpiration(validity)//TODO changer la méthode
    		   .compact();
    		 
    }
    private Key getSigningKey() {
    	  byte[] keyBytes = Decoders.BASE64.decode(this.secretKey);
    	  return Keys.hmacShaKeyFor(keyBytes);
    	}
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
            log.warn("Error");
        }
        return false;
    }
}