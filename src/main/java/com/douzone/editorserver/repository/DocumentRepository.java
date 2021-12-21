package com.douzone.editorserver.repository;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DocumentRepository {
	public Long findVersion(Long no);
}
