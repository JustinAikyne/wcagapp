package com.brahos.accessibilitychecker.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Document(collection = "scan")
@Data
public class ScanDataRequest {

	@Field("_id")
	private ObjectId scanId;
	private String status;
	private String name;
	private String pageUrl;
	private List<String> allPageUrls;

	private List<String> urls;

	private String version;

	private String level;
	private boolean recurringEnabled;

}
