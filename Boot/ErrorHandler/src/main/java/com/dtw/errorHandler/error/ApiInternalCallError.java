package com.dtw.errorHandler.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class ApiInternalCallError extends ApiSubError {

	private Integer status;
	private String httpMethod;
	private String url;
	private Object body;
	
	public ApiInternalCallError(Integer status, String httpMethod, String url) {
		this.status = status;
		this.httpMethod = httpMethod;
		this.url = url;
	}
}
