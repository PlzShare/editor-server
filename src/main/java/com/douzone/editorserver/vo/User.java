package com.douzone.editorserver.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {
	private Long no;
	private String id;
	private String name;
	private String password;
	private String nickname;
	private String regDate;
	private String leaveDate;
	private String profile;
	
}
