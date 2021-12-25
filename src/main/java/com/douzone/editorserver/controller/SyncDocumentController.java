package com.douzone.editorserver.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.douzone.editorserver.annotation.AuthUser;
import com.douzone.editorserver.service.DocumentService;
import com.douzone.editorserver.vo.Document;
import com.douzone.editorserver.vo.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class SyncDocumentController {
	private final String LOCK="lock";
	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;
	@Autowired
	private DocumentService documentService;

	@Autowired
	private StringRedisTemplate redisTemplate;
	@Autowired
	private ObjectMapper objectMapper;
	
	@PostMapping("/pub/{docNo}")
	public void pub(@PathVariable Long docNo, @RequestBody Map incomingChange, @AuthUser User authUser) throws InterruptedException, JsonProcessingException{
		System.out.println(authUser);
		
		incomingChange.keySet().forEach((key) -> System.out.println("key : " + key + " , value : " + incomingChange.get(key)));		
		incomingChange.put("user", authUser.getNo());
		incomingChange.put("nickname", authUser.getNickname());
				
		
		//waiting, cas
		while(redisTemplate.opsForSet().add("lock:" + docNo, LOCK) == 0) {
			Thread.sleep(50);
			System.out.println("waiting....");
		}
		
		List<String> changeList = redisTemplate.opsForList().range("change:" + docNo, 0, -1);
		
		System.out.println("-==========================start========================================================");
		int version;
		
		if(changeList.size() == 0) {
			System.out.println("changeList is Empty");
			version = (int) (documentService.getVersion(docNo) + 1);			
		}else {
			Map prevChange = null;
			try {
				prevChange = objectMapper.readValue(changeList.get(changeList.size() - 1), Map.class);
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			version = (int) prevChange.get("version") + 1;						
		}
		incomingChange.put("version", version);
		
		Integer baseVersion = (Integer)incomingChange.get("baseVersion");
		
		Map oldChange = null;
		List<Map> oldOPs = null;
		
		List<Map> incomingOPs = (List<Map>) ((Map)incomingChange.get("delta")).get("ops");
		System.out.println(incomingOPs);

		for(int i = changeList.size() - 1; i >= 0; i--) {
			try {
				oldChange = objectMapper.readValue(changeList.get(i), Map.class);
			} catch (JsonMappingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if((int)oldChange.get("version") <= baseVersion){
				System.out.println("=000000000000000000000000탈출11111111111111111111111111111111111");
				break;
			}
			if(oldChange.get("sid").equals(incomingChange.get("sid"))) continue;
			
			// start
			oldOPs = (List<Map>) ((Map)oldChange.get("delta")).get("ops");
			
			int incomingCursor = 0;
			int len;
			
			if(!(incomingOPs.get(0).size() == 1 && incomingOPs.get(0).containsKey("retain"))) {
				// index가 0인 연산
				int offset = getTransformedOffset(0, oldOPs, (String)incomingChange.get("sid"), (String)oldChange.get("sid"));
				if(offset != 0) {
					Map retainOP = new HashMap<>();
					retainOP.put("retain", offset);
					incomingOPs.add(0, retainOP);
					incomingCursor += offset;
				}
			}
			
			for(Map incomingOP : incomingOPs) {			
				if(incomingOP.containsKey("retain")) {
					incomingCursor += (Integer)incomingOP.get("retain");
				}
				
				if(incomingOP.containsKey("insert")) {
					if(incomingOP.get("insert") instanceof String) {
						len = ((String)incomingOP.get("insert")).length();
					}else {
						len = 1;
					}
					incomingCursor += len;
				}
				
				// retain이 아니면 transform 대상이 아님
				if(!(incomingOP.size() == 1 && incomingOP.containsKey("retain"))) continue;
				
				// transform
				int offset = getTransformedOffset(incomingCursor, oldOPs, (String) incomingChange.get("sid"), (String) oldChange.get("sid"));
				if(offset != 0) {
					incomingOP.put("retain", (Integer)incomingOP.get("retain") + offset);						
					incomingCursor += offset;
				}
				System.out.println("transformed");
				System.out.println(incomingOP);
			}
		}
		
		try {
			redisTemplate.opsForList().rightPush("change:" + docNo, objectMapper.writeValueAsString(incomingChange));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		redisTemplate.opsForSet().remove("lock:" + docNo, LOCK);
		
		
		redisTemplate.convertAndSend("/sub/" + docNo, objectMapper.writeValueAsString(incomingChange));
//		simpMessagingTemplate.convertAndSend("/sub/" + docNo, incomingChange);
	}
	
	private int getTransformedOffset(Integer incomingCursor, List<Map> oldOPs, String incomingSid, String oldSid) {
		Integer oldCursor = 0;
		int offset = 0;
		int len;
		
		for(Map oldOP : oldOPs) {
			if(oldCursor > incomingCursor) break;
			
			//insert tie break - sid가 작은쪽이 앞으로 감
			if(oldCursor == incomingCursor && incomingSid.compareTo(oldSid) < 0) break;
			
			if(oldOP.containsKey("retain")) {
				oldCursor += (Integer)oldOP.get("retain");
			}
			
			if(oldOP.containsKey("insert")) {
				if(oldOP.get("insert") instanceof String) {
					len = ((String)oldOP.get("insert")).length();
				}else {
					len = 1;
				}
				offset += len;
				oldCursor += len;
			}
			
			if(oldOP.containsKey("delete")) {
				offset -= (Integer) oldOP.get("delete");
			}
		}
		
		return offset;
	}

	@GetMapping("/history/{docNo}")
	public List getHistory(@PathVariable Long docNo, @AuthUser User authUser) {
		System.out.println("888888888888");
//		System.out.println(documentChanges.get(docNo));
//		documentChanges.putIfAbsent(docNo, new ArrayList<Map>());
//		return documentChanges.get(docNo);
		List<Map> result = new ArrayList<Map>();
		redisTemplate
				.opsForList()
				.range("change:" + docNo, 0, -1)
				.forEach(e -> {
					try {
						result.add(objectMapper.readValue(e, Map.class));
					} catch (JsonMappingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (JsonProcessingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
		System.out.println(result);
		
		return result;
	}
	
	@PutMapping("/save/{docNo}")
	public ResponseEntity save(@PathVariable Long docNo, @RequestBody Document document) throws InterruptedException {
		//waiting, cas
		while(redisTemplate.opsForSet().add("lock:" + docNo, LOCK) == 0) {
			Thread.sleep(50);
			System.out.println("waiting....");
		}
		
		redisTemplate.delete("change:" + docNo);
		
		document.setNo(docNo);
		System.out.println(document);
		documentService.save(document);
		
		redisTemplate.convertAndSend("/sub/" + document.getNo() + "/save", "saved");
		
		
		redisTemplate.opsForSet().remove("lock:" + docNo, LOCK);
		return new ResponseEntity(HttpStatus.OK);
	}
	
	@PostMapping("/join/{docNo}")
	public void notifyJoin(@AuthUser User authUser, @PathVariable Long docNo, String sid) throws JsonProcessingException {
		String userString = objectMapper.writeValueAsString(authUser);
		redisTemplate.opsForList().rightPush("member:" + docNo, userString);
		
		redisTemplate.convertAndSend("/sub/" + docNo + "/members", userString);
//		simpMessagingTemplate.convertAndSend("/sub/" + docNo + "/members", authUser);
	}
	
	@PostMapping("/leave/{docNo}")
	public void nofifyLeave(@AuthUser User authUser, @PathVariable Long docNo, String sid) throws JsonProcessingException {
		String userString = objectMapper.writeValueAsString(authUser);
		redisTemplate.opsForList().remove("member:" + docNo, 0, userString);
		redisTemplate.convertAndSend("/sub/" + docNo + "/members", userString);
//		simpMessagingTemplate.convertAndSend("/sub/" + docNo + "/members", authUser);		
	}
	
	@GetMapping("/members/{docNo}")
	public List getMemberList(@PathVariable Long docNo){
		List<Map> result = new ArrayList<>();
		
		redisTemplate.opsForList()
				.range("member:"+docNo, 0, -1)
				.forEach((e) -> {
					try {
						result.add(objectMapper.readValue(e, Map.class));
					} catch (JsonMappingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (JsonProcessingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
		
		return result;
	}
}