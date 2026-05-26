package com.sentinelmesh.devices;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class DeviceApiKeyService 
{
	private static final SecureRandom SECURE_RANDOM=new SecureRandom();
	private static final int KEY_BYTES=32;
	
	public String generateRawApiKey()
	{
		byte[] bytes=new byte[KEY_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		
		String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		
		return "sm_live_"+token;
	}
}
