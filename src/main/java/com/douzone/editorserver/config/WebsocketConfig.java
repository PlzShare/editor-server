package com.douzone.editorserver.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.douzone.editorserver.service.RedisMessageSubscriber;
import com.douzone.editorserver.util.TokenValidator;
import com.douzone.editorserver.vo.User;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {
	@Autowired
	private TokenValidator tokenValidator;
	@Value("{allowedOrigin}")
	private String allowedOrigin;
	
	@Autowired
	private RedisMessageListenerContainer messageListenerContainer;
	
	private RedisMessageSubscriber redisMessageSubscriber;
	
	@Autowired
	public WebsocketConfig(@Lazy RedisMessageSubscriber redisMessageSubscriber) {
		this.redisMessageSubscriber = redisMessageSubscriber;
	}
	
	
	private Set<String> topicSet = new HashSet<>();
	
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// TODO Auto-generated method stub
		
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// TODO Auto-generated method stub
		registry.setApplicationDestinationPrefixes("/pub").enableSimpleBroker("/sub");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {

			@Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
            	
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if(StompCommand.CONNECT.equals(accessor.getCommand())){
                    System.out.println("Connect ");
                    String token = (String)accessor.getNativeHeader("token").get(0);
                    User authUser = tokenValidator.getAuthUser(token);
                    if(authUser == null) throw new MessagingException("invalid token");
                    
                } else if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
                	
                	String destination = accessor.getDestination();
                    System.out.println("Subscribe : " + destination);
                    if(!topicSet.contains(destination)) {
                    	messageListenerContainer.addMessageListener(redisMessageSubscriber, new ChannelTopic(destination));
                    }
                } else if(StompCommand.SEND.equals(accessor.getCommand())){
                    System.out.println("Send message " );
                } else if(StompCommand.DISCONNECT.equals(accessor.getCommand())){
                    System.out.println("Exit ");
                } else {
                	System.out.println(accessor);
                }
                return message;
            }
        });
	}

}
