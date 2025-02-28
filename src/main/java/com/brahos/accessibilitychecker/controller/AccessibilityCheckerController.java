package com.brahos.accessibilitychecker.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.brahos.accessibilitychecker.model.AccessibilityCheckRequest;
import com.brahos.accessibilitychecker.model.CustomResponse;
import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.model.ScanDataRequest;
import com.brahos.accessibilitychecker.model.ScanDataResponse;
import com.brahos.accessibilitychecker.repository.AccessibilityCheckerScanResponseRepository;
import com.brahos.accessibilitychecker.repository.AccessibilityCheckerService;
import com.brahos.accessibilitychecker.repository.AccessibilityScanResponseService;
import com.brahos.accessibilitychecker.utility.WcagGuidelines_2_0_AllVersion;
import com.brahos.accessibilitychecker.utility.WebDriverFactory;

@RestController
@RequestMapping("/api/accessibility")
public class AccessibilityCheckerController {

	private static final Logger logger = LoggerFactory.getLogger(AccessibilityCheckerController.class);

	@Autowired
	private WcagGuidelines_2_0_AllVersion wcagGuidelines_2_0_AllVersion;

	@Autowired
	private AccessibilityCheckerService accessibilityCheckerService;

	@Autowired
	AccessibilityScanResponseService scanResponseService;
	@Autowired
	AccessibilityCheckerScanResponseRepository accessibilityCheckerScanResponseRepository;

	@PostMapping("/get-urls")
	public ResponseEntity<?> getAllUrlsByScanId(@RequestBody ScanDataRequest scanDataRequest) {
		ObjectId scanId = scanDataRequest.getScanId();

		String objScanIdString = scanId.toString();

		if (objScanIdString == null) {
			return ResponseEntity.badRequest().body("Scan ID is mandatory.");
		}

		try {

			ScanDataResponse allUrls = accessibilityCheckerService.getAllDetailsByScanId(objScanIdString);

			return ResponseEntity.ok(allUrls);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping("/check-by-scanid")
	public ResponseEntity<?> checkAccessibilitybyUrl(@RequestBody AccessibilityCheckRequest request) {

		String scanId = (String) request.getScanId();

		if (request.getScanId() == null) {
			logger.error("Scan ID is mandatory.");
			return ResponseEntity.badRequest().body("Scan ID is mandatory.");
		}

		try {

			ScanDataResponse scanDataRequest = accessibilityCheckerService.getAllDetailsByScanId(scanId);

			List<String> allUrls = scanDataRequest.getUrls();
			if (allUrls == null || allUrls.isEmpty()) {
				return ResponseEntity.badRequest().body("No URLs found for the provided Scan ID.");
			}

			logger.info("URLs retrieved for Scan ID {}: {}", request.getScanId(), allUrls);

			String wcagVersion = scanDataRequest.getVersion();
			String level = scanDataRequest.getLevel();

			List<GuidelineResponse> responses = new ArrayList<>();
			for (String url : allUrls) {
				if (!isValidInput(url, level)) {
					logger.error("Invalid input: URL: {}, Level: {}", url, level);
					return ResponseEntity.badRequest()
							.body("Invalid input. Ensure the URL and WCAG level are correct.");
				}

				URI uri = validateAndParseUrl(url);
				Document document = fetchDocumentFromUrl(uri);

				GuidelineResponse guidelineResponse = wcagGuidelines_2_0_AllVersion.executeFilter(url, wcagVersion,
						level, document, scanId);

				logger.info("Accessibility check completed successfully for URL: {}", url);
				if (guidelineResponse != null) {
					responses.add(guidelineResponse);
				}
			}

			WebDriverFactory.quitDriver();
			return ResponseEntity.ok(responses);

		} catch (URISyntaxException e) {
			logger.error("Invalid URL syntax.", e);
			return ResponseEntity.badRequest().body("Invalid URL format.");
		} catch (MalformedURLException e) {
			logger.error("Malformed URL.", e);
			return ResponseEntity.badRequest().body("The URL has an invalid format.");
		} catch (IllegalArgumentException e) {
			logger.error("Unsupported URL scheme or invalid Scan ID.", e);
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (IOException e) {
			logger.error("Failed to fetch or parse content from URL.", e);
			return ResponseEntity.badRequest().body("Unable to fetch or parse the content from the provided URL.");
		} catch (RuntimeException e) {
			logger.error("Unexpected error occurred while processing the request.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
		}
	}

	private boolean isValidInput(String url, String level) {
		return url != null && !url.trim().isEmpty() && (level.equals("A") || level.equals("AA") || level.equals("AAA"));
	}

	private URI validateAndParseUrl(String url) throws URISyntaxException, MalformedURLException {
		URI uri = new URI(url.trim());
		if (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https")) {
			throw new IllegalArgumentException("Unsupported URL scheme");
		}
		uri.toURL();
		return uri;
	}

	private Document fetchDocumentFromUrl(URI uri) throws IOException {
		logger.info("Fetching document from URL: {}", uri);
		return Jsoup.connect(uri.toString()).get();
	}

	
	private ResponseEntity<?> handleException(Exception e) {
		if (e instanceof URISyntaxException) {
			logger.error("Invalid URL syntax.", e);
			return ResponseEntity.badRequest().body("Invalid URL format.");
		} else if (e instanceof MalformedURLException) {
			logger.error("Malformed URL.", e);
			return ResponseEntity.badRequest().body("The URL has an invalid format.");
		} else if (e instanceof IllegalArgumentException) {
			logger.error("Unsupported URL scheme or invalid input.", e);
			return ResponseEntity.badRequest().body(e.getMessage());
		} else if (e instanceof IOException) {
			logger.error("Failed to fetch or parse content from URL.", e);
			return ResponseEntity.badRequest().body("Unable to fetch or parse the content from the provided URL.");
		} else {
			logger.error("Unexpected error occurred while processing the request.", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
		}
	}

	@GetMapping("/fetch")
	public ResponseEntity<CustomResponse> getScanByField(@RequestParam(required = false) String id,
			@RequestParam(required = false) String scanId, @RequestParam(required = false) String pageUrl,
			@RequestParam(required = false) String scanedTime) {

		// Check if id is provided
		if (id != null && !id.isEmpty()) {
			if (!ObjectId.isValid(id)) {
				return ResponseEntity.badRequest()
						.body(CustomResponse.builder().message("Invalid ID format").status(false).data(null).build());
			}
			return scanResponseService.getScanResponeById(new ObjectId(id))
					.map(data -> ResponseEntity
							.ok(CustomResponse.builder().message("Data found").status(true).data(data).build()))
					.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(CustomResponse.builder()
							.message("No data found for ID: " + id).status(false).data(null).build()));
		}

		// Check if scanedTime is provided without scanId
		if (scanedTime != null && !scanedTime.isEmpty() && (scanId == null || scanId.isEmpty())) {
			return ResponseEntity.badRequest().body(CustomResponse.builder()
					.message("scanId is required when scanedTime is provided").status(false).data(null).build());
		}

		// Check if both scanId and scanedTime are provided
		if (scanId != null && !scanId.isEmpty() && scanedTime != null && !scanedTime.isEmpty()) {
			List<GuidelineResponse> responses = accessibilityCheckerScanResponseRepository
					.findBySomeParent_ScanedTime_ScanId(scanedTime, scanId);

			if (!responses.isEmpty()) {
				return ResponseEntity
						.ok(CustomResponse.builder().message("Data found").status(true).data(responses).build());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CustomResponse.builder()
						.message("No data found for scannedTime: " + scanedTime).status(false).data(null).build());
			}
		}

		// Check if scanId is provided
		if (scanId != null && !scanId.isEmpty()) {
			List<GuidelineResponse> scanData = scanResponseService.getByScanId(scanId);
			if (!scanData.isEmpty()) {
				return ResponseEntity
						.ok(CustomResponse.builder().message("Data found").status(true).data(scanData).build());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CustomResponse.builder()
						.message("No data found for scanId: " + scanId).status(false).data(null).build());
			}
		}

		// Check if pageUrl is provided
		if (pageUrl != null && !pageUrl.isEmpty()) {
			List<GuidelineResponse> scanData = scanResponseService.getByPageUrl(pageUrl);
			if (!scanData.isEmpty()) {
				return ResponseEntity
						.ok(CustomResponse.builder().message("Data found").status(true).data(scanData).build());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CustomResponse.builder()
						.message("No data found for pageUrl: " + pageUrl).status(false).data(null).build());
			}
		}

		// If none of the parameters are provided
		return ResponseEntity.badRequest()
				.body(CustomResponse.builder()
						.message("At least one parameter (id, scanId, or pageUrl) must be provided").status(false)
						.data(null).build());
	}

}
