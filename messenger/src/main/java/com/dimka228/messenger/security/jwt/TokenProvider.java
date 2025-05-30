package com.dimka228.messenger.security.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.dimka228.messenger.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenProvider {

	@Value("${messenger.jwt.token-validity}")
	public long TOKEN_VALIDITY = 100000;

	@Value("${messenger.jwt.key}")
	@SuppressWarnings("FieldMayBeFinal")
	private String jwtSigningKey = "9a4f2c8d3b7a1e6f45c8a0b3f267d8b1d4e6f3c8a9d2b5f8e3a9c8b5f6v8a3d9";

	public String extractUserName(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		if (userDetails instanceof User customUserDetails) {
			claims.put("id", customUserDetails.getId());
			claims.put("login", customUserDetails.getLogin());
			claims.put("password", customUserDetails.getPassword());
		}
		return generateToken(claims, userDetails);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String userName = extractUserName(token);
		return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
		final Claims claims = extractAllClaims(token);
		return claimsResolvers.apply(claims);
	}

	private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder()
			.claims(extraClaims)
			.subject(userDetails.getUsername())
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + 1000 * TOKEN_VALIDITY * 60))
			.signWith(getSigningKey())
			.compact();
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}

}
