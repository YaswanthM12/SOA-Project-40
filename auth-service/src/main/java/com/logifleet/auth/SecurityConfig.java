package com.logifleet.auth;
import org.springframework.context.annotation.*; 
import org.springframework.security.config.annotation.web.builders.HttpSecurity; 
import org.springframework.security.crypto.bcrypt.*; 
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.security.web.SecurityFilterChain;

@Configuration 
public class SecurityConfig {
	@Bean 
	PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
		}
	@Bean
	SecurityFilterChain security(HttpSecurity h)throws Exception
	{
		return h.csrf(c->c.disable())
				.authorizeHttpRequests(a->a.anyRequest()
						.permitAll()).build();
	}
}
