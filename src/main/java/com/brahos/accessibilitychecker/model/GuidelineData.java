package com.brahos.accessibilitychecker.model;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * s Model representing each guideline's data including level and issues found.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuidelineData {

	private String guideline;
	private String level;
	private String wcagVersion;
	private ArrayList<IssueDetails> issueDetails;
	private Integer issueCount;
	private Integer successCount;

}
