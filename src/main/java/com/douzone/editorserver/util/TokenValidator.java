package com.douzone.editorserver.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.douzone.editorserver.vo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TokenValidator {

	@Value("${authServer.url}")
	private String authServerUrl;
	@Autowired
	private ObjectMapper objectMapper;
	public User getAuthUser(String token) {
		if(token == null) return null;
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", token);
		HttpEntity requestEntity = new HttpEntity(headers);
		RestTemplate restTemplate = new RestTemplate();
		
		try {
			ResponseEntity<String> responseEntity = restTemplate.exchange(authServerUrl + "/validate", HttpMethod.GET,
					requestEntity, String.class);
			User authUser = objectMapper.readValue(responseEntity.getBody(), User.class);
			
			return authUser;
		}catch (RestClientException | JsonProcessingException e) {
			System.out.println(e);
			return null;
		}

	
		
	}
	
	
}
