package com.douzone.weboard.util;

public class ApiResult {
	private String result;
	private Object data;
	private String message;

	public static ApiResult success(Object data) {
		return new ApiResult("success", data, null);
	}
	public static ApiResult fail(String message) {
		return new ApiResult("fail",null,message);
	}
	public ApiResult(String result, Object data, String message) {
		super();
		this.result = result;
		this.data = data;
		this.message = message;
	}
	public String getResult() {
		return result;
	}
	public Object getData() {
		return data;
	}
	public String getMessage() {
		return message;
	}
}
