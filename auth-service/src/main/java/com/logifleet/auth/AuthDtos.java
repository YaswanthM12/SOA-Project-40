package com.logifleet.auth;
public final class AuthDtos {
	private AuthDtos(){} 
	public record Credentials(String username,String password,String role){}
	public record TokenResponse(String token,String username,String role){} 
	}
