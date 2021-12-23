package com.douzone.editorserver.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.douzone.editorserver.interceptor.AuthInterceptor;
import com.douzone.editorserver.interceptor.CorsInterceptor;
import com.douzone.editorserver.util.AuthUserArgumentResolver;
import com.douzone.editorserver.util.TokenValidator;

@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer{
	
	@Bean
	public TokenValidator tokenValidator() {
		return new TokenValidator();
	}
	@Bean
	public AuthInterceptor authInterceptor() {
		return new AuthInterceptor();
	}
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// TODO Auto-generated method stub
		registry.addInterceptor(new CorsInterceptor())
				.addPathPatterns("/**");
		
		registry.addInterceptor(authInterceptor())
				.addPathPatterns("/pub/**","/history/**", "/join/**", "/leave/**");
	}
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		// TODO Auto-generated method stub
		resolvers.add(new AuthUserArgumentResolver());
	}

	
	
}
