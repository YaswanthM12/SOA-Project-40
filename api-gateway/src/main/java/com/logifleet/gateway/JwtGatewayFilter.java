package com.logifleet.gateway;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.*; 
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter,Ordered {
	private final SecretKey key;
	public JwtGatewayFilter(@Value("${app.jwt.secret}")String s){
		key=Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
		}
	public Mono<Void> filter(ServerWebExchange x,GatewayFilterChain c){
		String path=x.getRequest().getURI().getPath();
		if(path.startsWith("/auth/"))
			return c.filter(x);
		String h=x.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		
		try{
			if(h==null||!h.startsWith("Bearer "))
				throw new JwtException("Missing");
			Jwts.parser().verifyWith(key).build().parseSignedClaims(h.substring(7));
			return c.filter(x);
		}
		catch(Exception e){
			x.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return x.getResponse().setComplete();
		}
		}
	public int getOrder(){
		return -1;
		}
}
