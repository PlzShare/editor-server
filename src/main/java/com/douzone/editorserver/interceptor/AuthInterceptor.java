package com.douzone.editorserver.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import com.douzone.editorserver.util.TokenValidator;
import com.douzone.editorserver.vo.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
	@Value("${authServer.url}")
	private String authServerUrl;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private TokenValidator tokenValidator;
	
	private static final String HEADER_AUTH = "Authorization";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if(request.getMethod().equals("OPTIONS")) return true;
		
		RestTemplate restTemplate = new RestTemplate();
		final String token = request.getHeader(HEADER_AUTH);
		User authUser = tokenValidator.getAuthUser(token);
		if(authUser == null) {
			response.setStatus(401);
			return false;			
		}
		request.setAttribute("authUser", authUser);
		return true;
	}
}
