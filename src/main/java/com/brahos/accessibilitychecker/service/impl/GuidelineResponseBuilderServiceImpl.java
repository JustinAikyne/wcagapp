package com.brahos.accessibilitychecker.service.impl;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import com.brahos.accessibilitychecker.exception.GuidelineServiceException;
import com.brahos.accessibilitychecker.helper.service.IGuidelineDataTransformer;
import com.brahos.accessibilitychecker.model.GuidelineData;
import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.model.IssueDetails;
import com.brahos.accessibilitychecker.service.GuidelineResponseBuilderService;

@Component
public class GuidelineResponseBuilderServiceImpl implements GuidelineResponseBuilderService {

	private final IGuidelineDataTransformer guidelineDataTransformer;

	private int totalIssueCount = 0;

	private int totalSuccessCount = 0;

	public GuidelineResponseBuilderServiceImpl(IGuidelineDataTransformer guidelineDataTransformer) {
		this.guidelineDataTransformer = guidelineDataTransformer;
	}

//	@Override
//	public GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
//			List<IssueDetails> issueList, boolean status, int successCount, int issueCount) {
//		
//		// Validate input parameters
//		validateInputs(guideline, level, wcagVersion, issueList);
//
//		// Delegate the creation of GuidelineData to the transformer
//		GuidelineData guidelineData = guidelineDataTransformer.transform(guideline, level, wcagVersion, issueList,
//				issueCount, successCount);
//
//		// Use Lombok builder to create the GuidelineResponse with the provided counts
//		try {
//			return GuidelineResponse.builder().status(status).message("Accessibility check complete.")
//					.data(new ArrayList<>(List.of(guidelineData))) // Wrap data in a new ArrayList
//					.timestamp(LocalTime.now().toString()).guidelineIssueCount(issueCount)
//					.guidelineSuccessCount(successCount).build();
//		} catch (Exception e) {
//			throw new GuidelineServiceException("Error while building the GuidelineResponse.", e);
//		}
//	}

//	@Override
//	public GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
//			List<IssueDetails> issueList, boolean status, int successCount, int issueCount) {
//		
//
//
//		System.out.println("issueCount   in side buildGuidelineResponse   sss  ccccccccc"+issueCount);
//		
//		
//		System.out.println("successCount     in side buildGuidelineResponse   ccccccccc"+successCount);
//		
//		
//		validateInputs(guideline, level, wcagVersion, issueList);
//
//		GuidelineData guidelineData = guidelineDataTransformer.transform(guideline, level, wcagVersion, issueList,
//				issueCount, successCount);
//
//		try {
//			return GuidelineResponse.builder().status(status).message("Accessibility check complete.")
//					.data(guidelineData != null ? new ArrayList<>(List.of(guidelineData)) : new ArrayList<>()) // Handle
//																												// null
//					.timestamp(LocalTime.now().toString()).totalIssueCount(issueCount)
//					.totalSuccessCount(successCount).build();
//		} catch (Exception e) {
//			throw new GuidelineServiceException("Error while building the GuidelineResponse.", e);
//		}
//	}

	@Override
	public GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, boolean status, int successCount, int issueCount) {

		// Validate input parameters
		validateInputs(guideline, level, wcagVersion, issueList);

		// Transform guideline data
		GuidelineData guidelineData = null;

		if (issueList.size() > 0) {
			guidelineData = guidelineDataTransformer.transform(guideline, level, wcagVersion, issueList, issueCount,
					successCount);
		}

		// Update totalIssueCount and totalSuccessCount
		synchronized (this) {
			totalIssueCount = issueCount;
			totalSuccessCount = successCount;
		}

		try {
			return GuidelineResponse.builder().status(status).message("Accessibility check complete.")
					.data(guidelineData != null ? new ArrayList<>(List.of(guidelineData)) : new ArrayList<>())
					.timestamp(LocalTime.now().toString()).totalIssueCount(totalIssueCount)
					.totalSuccessCount(totalSuccessCount).build();
		} catch (Exception e) {
			throw new GuidelineServiceException("Error while building the GuidelineResponse.", e);
		}
	}

	// Method to retrieve the total issue count if needed elsewhere
	public int getTotalIssueCount() {
		return totalIssueCount;
	}

	// Method to retrieve the total success count if needed elsewhere
	public int getTotalSuccessCount() {
		return totalSuccessCount;
	}

//	@Override
//	public GuidelineResponse aggregateResponses(List<GuidelineResponse> guidelineResponses) {
//		if (guidelineResponses == null || guidelineResponses.isEmpty()) {
//			throw new GuidelineServiceException("The list of guideline responses cannot be null or empty.");
//		}
//
//		// Initialize counts and list for all guideline data
//		int successCount = 0;
//		int issueCount = 0;
//		List<GuidelineData> allGuidelineData = new ArrayList<>();
//
//		// Aggregate data and counts from all responses
//		try {
//			for (GuidelineResponse response : guidelineResponses) {
//				allGuidelineData.addAll(response.getData());
//				issueCount += response.getGuidelineIssueCount();
//				successCount += response.getGuidelineSuccessCount();
//			}
//
//			// Build the final response with aggregated data
//			return GuidelineResponse.builder().data(new ArrayList<>(allGuidelineData)).guidelineIssueCount(issueCount)
//					.guidelineSuccessCount(successCount).message("Accessibility checks completed.")
//					.status(issueCount == 0) // Status is successful if no issues
//					.timestamp(LocalTime.now().toString()).build();
//		} catch (Exception e) {
//			throw new GuidelineServiceException("Error while aggregating guideline responses.", e);
//		}
//	}

	@Override
	public GuidelineResponse aggregateResponses(List<GuidelineResponse> guidelineResponses) {

		if (guidelineResponses == null || guidelineResponses.isEmpty()) {

			return GuidelineResponse.builder().data(Collections.emptyList()).totalIssueCount(0).totalSuccessCount(0)
					.message("No guideline responses to aggregate.").status(true).timestamp(LocalTime.now().toString())
					.build();
		}

		int successCount = 0;
		int issueCount = 0;
		List<GuidelineData> allGuidelineData = new ArrayList<>();

		try {
			for (GuidelineResponse response : guidelineResponses) {
				if (response == null) {
					continue;
				}

				if (response.getData() != null && !response.getData().isEmpty()) {
					allGuidelineData.addAll(response.getData());
				}
			}

			if (allGuidelineData.isEmpty()) {

//	            return GuidelineResponse.builder()
//	                    .data(Collections.emptyList()) // Default to an empty list
//	                    .guidelineIssueCount(issueCount)
//	                    .guidelineSuccessCount(successCount)
//	                    .message("No aggregated guideline data available.")
//	                    .status(true) // Status is successful if no data and no issues
//	                    .timestamp(LocalTime.now().toString())
//	                    .build();

				return null;
			}
			return GuidelineResponse.builder().data(new ArrayList<>(allGuidelineData)).totalIssueCount(issueCount)
					.totalSuccessCount(successCount).message("Accessibility checks completed.").status(issueCount == 0)
					.timestamp(LocalTime.now().toString()).build();
		} catch (Exception e) {

			throw new GuidelineServiceException("Error while aggregating guideline responses.", e);
		}
	}

	private void validateInputs(String guideline, String level, String wcagVersion, List<IssueDetails> issueList) {
		if (guideline == null || guideline.trim().isEmpty()) {
			throw new GuidelineServiceException("Guideline cannot be null or empty.");
		}

		if (level == null || level.trim().isEmpty()) {
			throw new GuidelineServiceException("Level cannot be null or empty.");
		}

		if (wcagVersion == null || wcagVersion.trim().isEmpty()) {
			throw new GuidelineServiceException("WCAG Version cannot be null or empty.");
		}
	}

	@Override
	public GuidelineResponse buildTotalGuidelineResponse(String description, int totalIssueCount,
			int totalSuccessCount) {

		GuidelineData guidelineData = guidelineDataTransformer.transformDataOfIssueNull(description, description,
				description, null, totalIssueCount, totalSuccessCount, totalSuccessCount, totalSuccessCount);

		try {
			return GuidelineResponse.builder().message("Accessibility check complete." + "issue not found")
					.data(guidelineData != null ? new ArrayList<>(List.of(guidelineData)) : new ArrayList<>())

					.timestamp(LocalTime.now().toString()).totalIssueCount(totalSuccessCount)
					.totalSuccessCount(totalSuccessCount).build();
		} catch (Exception e) {
			throw new GuidelineServiceException("Error while building the GuidelineResponse.", e);
		}
	}

	@Override
	public GuidelineResponse buildGuidelineResponseN(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, Boolean status, int successCount, int issueCount, Document doc) {

		// Validate input parameters
		validateInputs(guideline, level, wcagVersion, issueList);

		// Transform guideline data
		GuidelineData guidelineData = null;

		if (issueList.size() > 0) {
			guidelineData = guidelineDataTransformer.transform(guideline, level, wcagVersion, issueList, issueCount,
					successCount);
		}

		// Update totalIssueCount and totalSuccessCount
		synchronized (this) {
			totalIssueCount = issueCount;
			totalSuccessCount = successCount;
		}

		try {
			return GuidelineResponse.builder().status(status).message("Accessibility check complete.")
					.data(guidelineData != null ? new ArrayList<>(List.of(guidelineData)) : new ArrayList<>())
					.timestamp(LocalTime.now().toString()).totalIssueCount(totalIssueCount)
					.totalSuccessCount(totalSuccessCount).build();
		} catch (Exception e) {
			throw new GuidelineServiceException("Error while building the GuidelineResponse.", e);
		}
	}

}
