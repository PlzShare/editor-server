package com.douzone.editorserver.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	
	private Map<Long, List<Map>> documentChanges = new HashMap<Long, List<Map>>();
	
	
	@PostMapping("/pub/{docNo}")
	synchronized public void pub(@PathVariable Long docNo, @RequestBody Map change, @AuthUser User authUser) throws InterruptedException {
		System.out.println("-==========================start========================================================");
		System.out.println(authUser);
		
		change.keySet().forEach((key) -> System.out.println("key : " + key + " , value : " + change.get(key)));		
		change.put("user", authUser.getNo());

		List<Map> changeList = documentChanges.get(docNo);
		Thread.sleep(1000);
		
		if(changeList == null) {
			System.out.println("changeList is Null");
			changeList = new ArrayList<Map>();
			
			Long version = documentService.getVersion(docNo);
			change.put("version", version + 1);
			
			documentChanges.put(docNo, changeList);
		}else if(changeList.size() == 0){
			System.out.println("changeList is Empty");
			throw new RuntimeException("impossible when syncronized ");
		}else {
			Map prevChange = changeList.get(changeList.size() - 1);
			change.put("version", (Long)prevChange.get("version") + 1);
			System.out.println("the document is on the version " + change.get("version"));
			
			//OT
			
			
		}
		
		changeList.add(change);
		simpMessagingTemplate.convertAndSend("/sub/" + docNo, change);
		System.out.println("-==========================end========================================================");
	}
	
	@GetMapping("/history/{docNo}")
	public List getHistory(@PathVariable Long docNo, @AuthUser User authUser) {
		System.out.println("888888888888");
		System.out.println(documentChanges.get(docNo));
		return documentChanges.get(docNo);
	}
}