package com.douzone.editorserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.douzone.editorserver.repository.DocumentRepository;

@Service
public class DocumentService {
	@Autowired
	private DocumentRepository documentRepository;

	
	public Long getVersion(Long no) {
		return documentRepository.findVersion(no);
	}
}
