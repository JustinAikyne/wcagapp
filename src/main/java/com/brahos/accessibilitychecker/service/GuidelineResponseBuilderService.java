package com.brahos.accessibilitychecker.service;

import java.util.List;

import org.jsoup.nodes.Document;

import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.model.IssueDetails;

public interface GuidelineResponseBuilderService {

	public GuidelineResponse aggregateResponses(List<GuidelineResponse> guidelineResponses);

	GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, boolean status, int issueCount, int successCount);

	public GuidelineResponse buildTotalGuidelineResponse(String description, int totalIssueCount, int totalSuccessCount);

	public GuidelineResponse buildGuidelineResponseN(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, Boolean status, int successCount, int issueCount, Document doc);

}
