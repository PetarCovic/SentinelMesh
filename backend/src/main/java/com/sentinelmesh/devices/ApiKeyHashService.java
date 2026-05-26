package com.sentinelmesh.devices;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

@Service
public class ApiKeyHashService 
{
	public String hash(String rawApiKey)
	{
		try
		{
			MessageDigest digest=MessageDigest.getInstance("SHA-256");
			byte[] hashBytes=digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
			
			return HexFormat.of().formatHex(hashBytes);
		} catch(NoSuchAlgorithmException ex)
		{
			throw new IllegalStateException("SHA-256 algorithm is not available", ex);
		}
	}
	
	public boolean matches(String rawApiKey, String storedHash)
	{
		String computedHash=hash(rawApiKey);
		
		return MessageDigest.isEqual(
				computedHash.getBytes(StandardCharsets.UTF_8), 
				storedHash.getBytes(StandardCharsets.UTF_8));
	}
}