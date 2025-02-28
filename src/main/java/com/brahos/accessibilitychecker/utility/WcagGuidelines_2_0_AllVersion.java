package com.brahos.accessibilitychecker.utility;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.brahos.accessibilitychecker.model.GuidelineData;
import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.model.IssueDetails;
import com.brahos.accessibilitychecker.repository.AccessibilityCheckerServiceRepository;
import com.brahos.accessibilitychecker.service.GuidelineExecutorService;
import com.brahos.accessibilitychecker.service.impl.AccessibilityGuidelinesServiceImpl_2_0_A;
import com.brahos.accessibilitychecker.service.impl.GuidelineResponseBuilderServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;

@Component
public class WcagGuidelines_2_0_AllVersion {

	private static final Logger logger = LoggerFactory.getLogger(WcagGuidelines_2_0_AllVersion.class);

	private final GuidelineExecutorService guidelineExecutorService;

	@Autowired
	AccessibilityGuidelinesServiceImpl_2_0_A wcagAccessibilityGuidelines_2_0_A;

	int totalGuidelineCount = 61;

	@Autowired
	private AccessibilityCheckerServiceRepository accessibilityCheckerRepository;

	@Autowired
	GuidelineResponseBuilderServiceImpl guidelineResponseBuilderService;

	public WcagGuidelines_2_0_AllVersion(GuidelineExecutorService guidelineExecutorService) {
		this.guidelineExecutorService = guidelineExecutorService;
	}

	/**
	 * Executes the appropriate WCAG 2.0 guidelines based on the level provided.
	 *
	 * @param url         The URL being checked
	 * @param wcagVersion The WCAG version (e.g., "2.0")
	 * @param level       The WCAG level (A, AA, AAA)
	 * @param scanId
	 * @param document
	 * @param document    The parsed HTML document
	 * @return A GuidelineResponse containing the results of the guideline checks
	 * @throws JsonProcessingException
	 */

	public GuidelineResponse executeFilter(String url, String wcagVersion, String level, Document document,
			String scanId) throws JsonProcessingException {
		List<GuidelineData> combinedData = new ArrayList<>();
		int issueCount = 0;
		int successCount = 0;

		// Handle different levels (A, AA, AAA)
		switch (level) {
		case "A":
			addGuidelineData(combinedData, guidelineExecutorService.executeGuidelinesA(document, url));
			break;
		case "AA":
			addGuidelineData(combinedData, guidelineExecutorService.executeGuidelinesA(document, url));
			addGuidelineData(combinedData, guidelineExecutorService.executeGuidelinesAA(document, url));
			break;
		case "AAA":
			addGuidelineData(combinedData, guidelineExecutorService.executeGuidelinesA(document, url));
			addGuidelineData(combinedData, guidelineExecutorService.executeGuidelinesAA(document, url));
			addGuidelineData(combinedData, guidelineExecutorService.executeGuidelinesAAA(document, url));
			break;
		default:
			logger.error("Unsupported WCAG level: {}", level);
			throw new IllegalArgumentException("Unsupported WCAG level: " + level);
		}

		// Calculate issue and success count
		for (GuidelineData data : combinedData) {
			if (data == null) {
				logger.warn("Null GuidelineData encountered, skipping...");
				continue;
			}

			List<IssueDetails> issueDetails = data.getIssueDetails();
			if (issueDetails != null && !issueDetails.isEmpty()) {
				issueCount++;
				logger.debug("Issue details found: {}", issueDetails);
			} else {
				successCount++;
				logger.debug("Success count incremented. Current success count: {}", successCount);
			}
		}

		logger.info("Total issues found: {}", issueCount);
		logger.info("Total successful guidelines: {}", successCount);

		// Build the response object
		GuidelineResponse response = null;
		if (combinedData.size() > 0) {
			response = buildGuidelineResponse(url, scanId, combinedData, issueCount, successCount);
		}

		// Save the response to MongoDB
		try {
			// accessibilityCheckerRepository.save(response);
			logger.info("GuidelineResponse saved to MongoDB for URL: {}", url);
		} catch (Exception e) {
			logger.error("Failed to save GuidelineResponse to MongoDB.", e);
			throw new RuntimeException("Failed to save data to MongoDB.", e);
		}

		return response;
	}

	/**
	 * Builds the GuidelineResponse object.
	 */
	private GuidelineResponse buildGuidelineResponse(String url, String scanId, List<GuidelineData> combinedData,
			int issueCount, int successCount) {

		int totalSuccessCount = totalGuidelineCount - issueCount;

		int totalIssueCount = wcagAccessibilityGuidelines_2_0_A.getTotalIssueCount();

	

		int totalSuccess = wcagAccessibilityGuidelines_2_0_A.getTotalSuccessCount();

		// totalSuccessCount=totalGuidelineCount-totalIssueCount;

		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String createdAt = now.atZone(ZoneOffset.UTC).format(formatter);

		GuidelineResponse response = new GuidelineResponse();
		response.setData(combinedData);

		response.setTotalIssueCount(totalIssueCount); // Placeholder, adjust if needed some of issue
		response.setTotalSuccessCount(totalSuccess); // Placeholder, adjust if needed some of success

		response.setGuidelineIssueCount(issueCount); // overall
		response.setGuidelineSuccessCount(totalSuccessCount); // overall
		response.setPageUrl(url);
		response.setScanId(scanId);
		response.setCreatedAt(createdAt);
		response.setUpdatedAt(createdAt);
		response.setScanedTime(createdAt);
		response.setTimestamp(createdAt);

		return response;
	}

	private void addGuidelineData(List<GuidelineData> combinedData, GuidelineResponse response) {
		Integer totalSuccessCount = 0;
		if (response != null && response.getData() != null && response.getTotalSuccessCount() > 0) {
			totalSuccessCount = response.getTotalSuccessCount();
		}

		if (response != null && response.getData() != null) {
			combinedData.addAll(response.getData());
		}
	}
}
