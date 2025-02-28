package com.brahos.accessibilitychecker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing details of each issue found.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDetails {

	public String title;
	public String tagName;
	public String snippet;
	public String selector;
	public String fixes;

}
