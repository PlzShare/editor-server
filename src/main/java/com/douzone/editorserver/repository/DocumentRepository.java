package com.douzone.editorserver.repository;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.douzone.editorserver.vo.Document;

@Mapper
@Repository
public interface DocumentRepository {
	public Long findVersion(Long no);
	public boolean update(Document document);
}
