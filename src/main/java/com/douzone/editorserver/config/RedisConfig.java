package com.douzone.editorserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.douzone.editorserver.service.RedisMessageSubscriber;

@Configuration
public class RedisConfig {
	@Autowired
	private RedisTemplate redisTemplate;
	
	@Bean
	public MessageListenerAdapter messageListener() {
		return new MessageListenerAdapter(new RedisMessageSubscriber());
	}
	
	@Bean
	public RedisMessageListenerContainer rediContainer() {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		System.out.println(redisTemplate.getConnectionFactory());
		container.setConnectionFactory(redisTemplate.getConnectionFactory());
		return container;
	}
}
