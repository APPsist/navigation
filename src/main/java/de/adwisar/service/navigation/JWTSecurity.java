package de.adwisar.service.navigation;

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.json.JsonObject;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

public class JWTSecurity {
	
	// method check if the provided signed json web token is valid
	// valid means it can be decrypted using secret and
	// (i) expiration time is greater than issuedAt time
	// (ii) expiration time is greater than the current time
	//
	// timestamps extracted from the jwt need to be multiplied by 1000 to resemble milliseconds 
	public static boolean _isValid(String compactJws, String JWTSecret){
		Jws<Claims> claim = parseClaim(compactJws, JWTSecret);
		if (null != claim){
			JsonObject claimData = new JsonObject(claim.getBody());
			long exp = claimData.getLong("exp")* 1000;
			long iat = claimData.getLong("iat")* 1000;
			long now = System.currentTimeMillis();
			if (exp-iat>0 && exp-now>0){
				return true;
			}
		}
		return false;
	}
	
	
	
	public static String extractUserId(String compactJws, String JWTSecret){
		String userId="";
		Jws<Claims> claim = parseClaim(compactJws, JWTSecret);
		if (null != claim){
			JsonObject claimData = new JsonObject(claim.getBody());
			userId = claimData.getString("id");
		}
		return userId;
	}
	
	
	static private Jws<Claims> parseClaim(String compactJws, String JWTSecret){
		Jws<Claims> x = null;
		try {
			 x = Jwts.parser()
					   .setSigningKey(JWTSecret.getBytes("UTF-8"))
					   .parseClaimsJws(compactJws);
		} catch (io.jsonwebtoken.JwtException jwtE){
			jwtE.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return x;
	}
}
