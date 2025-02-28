package com.brahos.accessibilitychecker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing details of each issue's Fix found.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueFixDetails {

	private String title;
    private String topic;
    private String guideline;
    private String fix;

}
