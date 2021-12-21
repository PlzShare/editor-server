package com.douzone.editorserver.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@MapperScan(basePackages="com.douzone.editorserver.repository")
public class DBConfig {
	
}
