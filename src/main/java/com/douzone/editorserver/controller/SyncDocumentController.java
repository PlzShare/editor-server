package com.douzone.editorserver.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.douzone.editorserver.annotation.AuthUser;
import com.douzone.editorserver.service.DocumentService;
import com.douzone.editorserver.vo.User;

@RestController
public class SyncDocumentController {
	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;
	@Autowired
	private DocumentService documentService;

	private ConcurrentMap<Long, List<Map>> documentChanges = new ConcurrentHashMap<Long, List<Map>>();
	private ConcurrentMap<Long, List<User>> documentMemberList = new ConcurrentHashMap<Long, List<User>>();
	
	@PostMapping("/pub/{docNo}")
	public void pub(@PathVariable Long docNo, @RequestBody Map incomingChange, @AuthUser User authUser) throws InterruptedException {
		System.out.println(authUser);
		incomingChange.keySet().forEach((key) -> System.out.println("key : " + key + " , value : " + incomingChange.get(key)));		
		incomingChange.put("user", authUser.getNo());
		incomingChange.put("nickname", authUser.getNickname());
		
		List<Map> changeList = documentChanges.get(docNo);
		
		System.out.println("-==========================start========================================================");
		synchronized (changeList) {
			Long version = null;
			
			if(changeList.size() == 0) {
				System.out.println("changeList is Empty");
				version = documentService.getVersion(docNo) + 1;
			}else{
				Map prevChange = changeList.get(changeList.size() - 1);
				version = (Long)prevChange.get("version") + 1;			
			}
			incomingChange.put("version", version);
			//OT - ops를 변환
			//{retain} + {insert : (text | obj)}
			//{retatn} + {delete : number}
			//{retain} + {retain, attribute}
			
			Integer baseVersion = (Integer)incomingChange.get("baseVersion");
			
			Map oldChange = null;
			List<Map> oldOPs = null;
			
			List<Map> incomingOPs = (List<Map>) ((Map)incomingChange.get("delta")).get("ops");
			System.out.println(incomingOPs);
			
			
			for(int i = changeList.size() - 1; i >= 0; i--) {
				oldChange = changeList.get(i);
				if((Long)oldChange.get("version") <= baseVersion){
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
			changeList.add(incomingChange);
			
			simpMessagingTemplate.convertAndSend("/sub/" + docNo, incomingChange);
			System.out.println("-==========================end========================================================");
		}
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
		System.out.println(documentChanges.get(docNo));
		documentChanges.putIfAbsent(docNo, new ArrayList<Map>());
		
		return documentChanges.get(docNo);
	}
	@PostMapping("/join/{docNo}")
	public void notifyJoin(@AuthUser User authUser, @PathVariable Long docNo, String sid) {
		documentMemberList.putIfAbsent(docNo, new ArrayList<>());
		List<User> list = documentMemberList.get(docNo);

		list.add(authUser);
		
		simpMessagingTemplate.convertAndSend("/sub/" + docNo + "/members", authUser);
	}
	
	@PostMapping("/leave/{docNo}")
	public void nofifyLeave(@AuthUser User authUser, @PathVariable Long docNo, String sid) {
		List<User> list = documentMemberList.get(docNo);
		boolean removeIf = list.removeIf((mem) -> mem.getNo().equals(authUser.getNo()));
		
		System.out.println("8888888888888888888888888888888");
		System.out.println(removeIf);
		simpMessagingTemplate.convertAndSend("/sub/" + docNo + "/members", authUser);		
	}
	
	@GetMapping("/members/{docNo}")
	public List<User> getMemberList(@PathVariable Long docNo){
		return documentMemberList.get(docNo);
	}
}