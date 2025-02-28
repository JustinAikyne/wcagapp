package com.brahos.accessibilitychecker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class CustomResponse {
	private String message;
	private boolean status;
	private Object  data;
}
