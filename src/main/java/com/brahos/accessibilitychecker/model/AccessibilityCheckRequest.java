package com.brahos.accessibilitychecker.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "scan")

@Data
public class AccessibilityCheckRequest {

//	 private String url;
	private String wcagVersion;
	private String level;

	private Object scanId;
}
