package com.douzone.editorserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber implements MessageListener{

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		simpMessagingTemplate.convertAndSend(new String(pattern), message.toString());
	}

}
