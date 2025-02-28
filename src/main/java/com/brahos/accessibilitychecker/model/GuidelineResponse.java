package com.brahos.accessibilitychecker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Main response model containing the status, message, guideline data, and
 * timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scan_responses")
public class GuidelineResponse {

	private boolean status;
	private String message;

	@Field
	private String scanId;

	private String pageUrl;

	@Field
	private String scanedTime;

	@CreatedDate
	private String createdAt;

	@LastModifiedDate
	private String updatedAt;

	@Builder.Default
	private List<GuidelineData> data = new ArrayList<>();
	private Object timestamp;

	@Builder.Default
	private Integer totalIssueCount = 0;
	@Builder.Default
	private Integer totalSuccessCount = 0;

	@Builder.Default
	private Integer guidelineIssueCount = 0;
	@Builder.Default
	private Integer guidelineSuccessCount = 0;

	public List<GuidelineData> getData() {
		return Collections.unmodifiableList(data);
	}
}
