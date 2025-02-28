package com.brahos.accessibilitychecker.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.helper.service.IGuidelineDataTransformer;
import com.brahos.accessibilitychecker.model.GuidelineData;
import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.model.IssueDetails;
import com.brahos.accessibilitychecker.model.IssueFixDetails;
import com.brahos.accessibilitychecker.model.WcagFixesData;
import com.brahos.accessibilitychecker.repository.AccessibilityIssueFixesRepository;
import com.brahos.accessibilitychecker.service.GuidelineResponseBuilderService;
import com.brahos.accessibilitychecker.service.HelperGuidelinesExecutorService;
import com.brahos.accessibilitychecker.service.WcagAccessibilityGuidelines_2_0_A;
import com.brahos.accessibilitychecker.utility.WebDriverFactory;
import com.brahos.accessibilitychecker_enum.WcagGuidelineA;

import ch.qos.logback.classic.Logger;
import io.netty.handler.timeout.TimeoutException;
import jakarta.annotation.PostConstruct;

/**
 * This class provides methods for checking web accessibility compliance
 * according to WCAG (Web Content Accessibility Guidelines).
 */
@Service
public class AccessibilityGuidelinesServiceImpl_2_0_A implements WcagAccessibilityGuidelines_2_0_A {

	private static final Logger logger = (Logger) LoggerFactory
			.getLogger(AccessibilityGuidelinesServiceImpl_2_0_A.class);

	private HelperGuidelinesExecutorService helperGuidelinesExecutorService;

	private GuidelineResponseBuilderService guidelineResponseBuilderService;

	private IGuidelineDataTransformer iGuidelineDataTransformer;

	private WebDriverFactory webDriverFactory;

	// Overall counts for the entire service
	private int totalIssueCount = 0;
	private int totalSuccessCount = 0;

	public synchronized void updateCounts(int issueCount, int successCount) {
		totalIssueCount += issueCount;
		totalSuccessCount += successCount;
	}

	public int getTotalIssueCount() {
		return totalIssueCount;
	}

	public int getTotalSuccessCount() {
		return totalSuccessCount;
	}

	private Boolean status = true;

	private int guidelineIssueCount = 0;
	private int guidelineSuccessCount = 0;

	// Constants
	private final String level = "A";
	private final String wcagVersion = "2.0";

	@Autowired
	private AccessibilityIssueFixesRepository issueFixesRepository;

	private Map<String, IssueFixDetails> fixes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	@PostConstruct
	public void init() {
		fixes = getAllFixesDataByVersion("2.0", "A");
	}

	public Map<String, IssueFixDetails> getAllFixesDataByVersion(String wcagVersion, String level) {
		Optional<WcagFixesData> fixesData = issueFixesRepository.findByWcagVersionAndLevel(wcagVersion, level);
		return fixesData.map(data -> {
			Map<String, IssueFixDetails> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			caseInsensitiveMap.putAll(data.getData());
			return caseInsensitiveMap;
		}).orElse(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
	}

//	public void getAllFixesDataByVersionTest(String wcagVersion, String level) {
//		System.out.println(fixes);
//		System.out.println(fixes.get("Missing Alt Attribute").getFix());
//		
//	}

	// private static final Pattern UNIT_PATTERN =
	// Pattern.compile("(-?\\d*i\\.?\\d+)(px|em|%|rem|deg|rad|grad)?");

	public AccessibilityGuidelinesServiceImpl_2_0_A(HelperGuidelinesExecutorService helperGuidelinesExecutorService,
			GuidelineResponseBuilderService guidelineResponseBuilderService,
			IGuidelineDataTransformer iGuidelineDataTransformer, WebDriverFactory webDriverFactory) {
		this.helperGuidelinesExecutorService = helperGuidelinesExecutorService;
		this.guidelineResponseBuilderService = guidelineResponseBuilderService;
		this.iGuidelineDataTransformer = iGuidelineDataTransformer;
		this.webDriverFactory = webDriverFactory;
	}

	public GuidelineResponse evaluateNonTextContent(Document doc) {
		String guideline = "1.1.1 Non-text Content";

		Set<String> seenAltTexts = new HashSet<>();
		List<IssueDetails> issueList = new ArrayList<>();

		// Check various elements
		checkImageElements(doc, seenAltTexts, issueList);
		checkVideoElements(doc, issueList);
		checkAudioElements(doc, issueList);
		checkInputElements(doc, issueList);

		// Calculate issue and success count
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);
	}

	private void checkImageElements(Document doc, Set<String> seenAltTexts, List<IssueDetails> issueList) {
		System.out.println("..............fixes.................." + fixes.get("Missing Alt Attribute").getFix());
		Elements images = doc.select("img");
		for (Element img : images) {
			String altText = img.attr("alt").trim();
			String tempImg = img.toString();

			if (!img.hasAttr("alt")) {
				addIssueN(issueList, img, tempImg, "Missing alt attribute", doc);
			} else if (altText.isEmpty() && helperGuidelinesExecutorService.isFunctionalOrInformative(img)) {
				addIssueN(issueList, img, tempImg, "Empty alt text", doc);
			} else {
				handleAltText(img, altText, seenAltTexts, issueList, tempImg);
			}
		}
	}

	private void handleAltText(Element img, String altText, Set<String> seenAltTexts, List<IssueDetails> issueList,
			String tempImg) {
		if (seenAltTexts.contains(altText)) {
			addIssueN(issueList, img, tempImg, "Duplicate alt text", null);
		} else {
			seenAltTexts.add(altText);
		}
		if (helperGuidelinesExecutorService.isPlaceholderText(altText)) {
			addIssueN(issueList, img, tempImg, "Placeholder alt text", null);
		}
		if (!helperGuidelinesExecutorService.isAltTextSufficient(altText, img)) {
			addIssueN(issueList, img, tempImg, "Insufficient alt text", null);
		}
	}

	private void checkVideoElements(Document doc, List<IssueDetails> issueList) {
		Elements videos = doc.select("video");
		for (Element video : videos) {
			if (video.select("track[kind=subtitles], track[kind=captions]").isEmpty()) {
				addIssueN(issueList, video, video.toString(), "Missing captions/subtitles", doc);
			}
		}
	}

	private void checkAudioElements(Document doc, List<IssueDetails> issueList) {
		Elements audios = doc.select("audio");
		for (Element audio : audios) {
			if (audio.select("track[kind=captions]").isEmpty()) {
				addIssueN(issueList, audio, audio.toString(), "Missing captions/transcripts", doc);
			}
		}
	}

	private void checkInputElements(Document doc, List<IssueDetails> issueList) {
		Elements inputs = doc.select("input:not([type='hidden'])");
		for (Element input : inputs) {
			if (!input.hasAttr("aria-label") && !input.hasAttr("aria-labelledby")) {
				addIssueN(issueList, input, input.toString(), "Missing label for input field", doc);
			}
		}
	}

	private void addIssueN(List<IssueDetails> issueList, Element element, String tempElement, String issueDescription,
			Document doc) {
//		iGuidelineDataTransformer.addIssueUsingJsoupForCss(issueList, element, tempElement, issueDescription, level);
		iGuidelineDataTransformer.addIssueUsingJsoupForCss(issueList, element, tempElement, issueDescription,
				getFix(issueDescription));

	}

	private void checkImagesForAltText(Document doc, List<IssueDetails> issuesList, Set<String> processedAltTexts,
			String accessibilityLevel) {
		try {
			Elements images = doc.select("img");
			for (Element img : images) {

				String altText = img.attr("alt").trim();
				String imageHtmlString = img.toString();

				if (!img.hasAttr("alt")) {
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, img, imageHtmlString,
							"Missing alt attribute", accessibilityLevel);
				} else if (altText.isEmpty() && helperGuidelinesExecutorService.isFunctionalOrInformative(img)) {
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, img, imageHtmlString, "Empty alt text",
							accessibilityLevel);
				} else {
					if (processedAltTexts.contains(altText)) {
						iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, img, imageHtmlString,
								"Duplicate alt text", accessibilityLevel);
					} else {
						processedAltTexts.add(altText);
					}

					if (helperGuidelinesExecutorService.isPlaceholderText(altText)) {
						iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, img, imageHtmlString,
								"Placeholder alt text", accessibilityLevel);
					}

					if (!helperGuidelinesExecutorService.isAltTextSufficient(altText, img)) {
						iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, img, imageHtmlString,
								"Insufficient alt text", accessibilityLevel);
					}
				}
			}
		} catch (Exception e) {

		}
	}

	private void checkVideosForCaptions(Document doc, List<IssueDetails> issuesList, String accessibilityLevel) {
		try {
			Elements videos = doc.select("video");
			for (Element video : videos) {
				if (video.select("track[kind=subtitles], track[kind=captions]").isEmpty()) {
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, video, video.toString(),
							"Missing captions/subtitles", accessibilityLevel);
				}
			}
		} catch (Exception e) {

		}
	}

	private void checkAudiosForCaptions(Document doc, List<IssueDetails> issuesList, String accessibilityLevel) {
		try {
			Elements audios = doc.select("audio");
			for (Element audio : audios) {
				if (audio.select("track[kind=captions]").isEmpty()) {
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, audio, audio.toString(),
							"Missing captions/transcripts", accessibilityLevel);
				}
			}
		} catch (Exception e) {

		}
	}

	private void checkInputFieldsForLabels(Document doc, List<IssueDetails> issuesList, String accessibilityLevel) {
		try {
			Elements inputs = doc.select("input:not([type='hidden']):not([aria-label]):not([aria-labelledby])");
			for (Element input : inputs) {
				if (!input.hasAttr("aria-label") && !input.hasAttr("aria-labelledby")) {
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, input, input.toString(),
							"Missing label for input field", accessibilityLevel);
				}
			}
		} catch (Exception e) {

		}
	}

	private int getTotalCheckedElements(Document doc) {
		if (doc == null) {
			return 0; // Prevent NullPointerException
		}

		try {
			return doc.select("img, svg, picture, figure, video, audio, source, track, "
					+ "input, textarea, select, button, label, "
					+ "[role=img], [role=button], [role=checkbox], [role=link]").size();
		} catch (Exception e) {

			return 0;
		}
	}

	public GuidelineResponse audioVideoOnlyContent(Document doc) {

		ArrayList<IssueDetails> issueList = new ArrayList<>();

		try {
			Elements audioElements = doc.select("audio");
			Elements videoElements = doc.select("video");

			for (Element audio : audioElements) {
				if (helperGuidelinesExecutorService.isPrerecorded(audio)) {
					if (!helperGuidelinesExecutorService.hasTextAlternative(audio)) {
						// TODO TITLE -->Missing Text Alternative for Audio Content
						String key = "Missing Text Alternative for Audio Content";
						issueList.add(createIssueDetails(audio, "audio", getFix(key)));
						status = false;
					}
				}
			}

			for (Element video : videoElements) {
				if (helperGuidelinesExecutorService.isPrerecorded(video)) {
					if (!helperGuidelinesExecutorService.hasAudioTrack(video)
							&& !helperGuidelinesExecutorService.hasAudioTrack(video)) {
						// TODO TITLE -->Missing Alternative for Video-only Content
						String key = "Missing Alternative for Video-only Content";
						issueList.add(createIssueDetails(video, "video", getFix(key)));
						status = false;
					}
				}
			}

		} catch (Exception e) {
			status = false;
			e.printStackTrace();
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global issue & success counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(
				WcagGuidelineA.AUDIO_ONLY_VIDEO_ONLY.getDescription(), level, wcagVersion, issueList, false,
				successCount, issueCount);

	}

	private IssueDetails createIssueDetails(Element element, String title, String fix) {
		IssueDetails issueOb = new IssueDetails();
		issueOb.setTitle(title);
		issueOb.setTagName(element.tagName());
		issueOb.setSnippet(element.toString());
		issueOb.setSelector(element.cssSelector());
		issueOb.setFixes(fix);
		return issueOb;
	}

//	public GuidelineResponse captionsPrerecorded(Document doc) {
//		
//		  issueCount = 0;
//		  successCount = 0;
//
//		ArrayList<GuidelineData> guidelineDataList = new ArrayList<>();
//		ArrayList<IssueDetails> issueList = new ArrayList<>();
//
//		try {
//			Elements videoElements = doc.select("video");
//
//			issueCount += helperGuidelinesExecutorService.analyzeCaptionsForVideos(videoElements, "video", level,
//					issueList);
//			successCount += videoElements.size() - issueCount;
//
//		} catch (Exception e) {
//			logger.error("Error occurred while checking captions for prerecorded videos", e);
//			status = false;
//		}
//
//		return (issueList != null && !issueList.isEmpty())
//				? guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.CAPTIONS.getDescription(),
//						level, wcagVersion, issueList, false, successCount, successCount)
//				: null;
//
////		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
////				successCount, successCount);
//	}

	public GuidelineResponse captionsPrerecorded(Document doc) {

		// Initialize data storage

		ArrayList<IssueDetails> issueList = new ArrayList<>();

		try {
			// Select all video elements in the document
			Elements videoElements = doc.select("video");

			// Analyze each video for captions
			// TODO TITLE -->Missing Captions/Subtitles
			String key = "Missing Captions/Subtitles";
			// issueCount +=
			// helperGuidelinesExecutorService.analyzeCaptionsForVideos(videoElements,
			// "video", level,
			// issueList, getFix(key));

		} catch (Exception e) {
			logger.error("Error occurred while checking captions for prerecorded videos", e);
			status = false; // Indicating an error in the process
		}

		// Calculate issue and success count
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Check if there were issues found and build the response accordingly
		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.CAPTIONS.getDescription(),

				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	public GuidelineResponse audioDescriptionOrMediaAlternative(Document doc) {

		List<IssueDetails> issueList = new ArrayList<>();

		Elements videos = doc.select("video");
		for (Element video : videos) {
			boolean hasAudioDescription = !video.select("track[kind=descriptions]").isEmpty();
			boolean hasMediaAlternative = video.select("track[kind=transcript]").size() > 0;

			if (!hasAudioDescription && !hasMediaAlternative) {
				String key = "Missing audio description or media alternative";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, video, video.toString(), key, getFix(key));
			}
		}

		Elements audios = doc.select("audio");
		for (Element audio : audios) {
			boolean hasTranscript = audio.select("track[kind=transcript]").size() > 0;

			if (!hasTranscript) {
				String key = "Missing transcript or audio description";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, audio, audio.toString(), key, getFix(key));
			}
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.AUDIO_DESCRIPTION.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);

	}

//	public GuidelineResponse infoAndRelationships(Document doc) {
//
//		List<IssueDetails> issueList = new ArrayList<>();
//		int totalCheckedElements = 0;
//		int successfulElements = 0;
//
//		Elements formElements = doc.select("form");
//		for (Element form : formElements) {
//			totalCheckedElements++;
//
//			Elements requiredFields = form.select("input[required], select[required], textarea[required]");
//			for (Element field : requiredFields) {
//
//				boolean hasLabel = field.hasAttr("aria-label") || field.previousElementSibling() != null;
//
//				if (!hasLabel) {
//					status = false;
//					iGuidelineDataTransformer.addIssue(issueList, field, field.toString(),
//							"Required form field is not programmatically associated with a label or description.",
//							level);
//				} else {
//					successfulElements++;
//				}
//			}
//		}
//
//		Elements tables = doc.select("table");
//		for (Element table : tables) {
//			totalCheckedElements++;
//
//			Elements headers = table.select("th");
//			boolean hasColumnHeaders = table.select("th[scope=col]").size() > 0;
//			boolean hasRowHeaders = table.select("th[scope=row]").size() > 0;
//
//			if (!hasColumnHeaders || !hasRowHeaders) {
//				status = false;
//				iGuidelineDataTransformer.addIssue(issueList, table, table.toString(),
//						"Table headers for rows and columns are missing or not programmatically determined.", level);
//			} else {
//				successfulElements++;
//			}
//		}
//
//		Elements listElements = doc.select("ul, ol");
//		for (Element list : listElements) {
//			totalCheckedElements++;
//
//			Elements listItems = list.select("li");
//			if (listItems.isEmpty()) {
//				status = false;
//				iGuidelineDataTransformer.addIssue(issueList, list, list.toString(),
//						"List is missing list items, breaking the structural relationship.", level);
//			} else {
//				successfulElements++;
//			}
//		}
//
//		int issueCount = issueList.size();
//		int successCount = successfulElements;
//
//		// Build and return the GuidelineResponse with the results
////		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
////				successCount, issueCount);
//
//		return (issueList != null && !issueList.isEmpty()) ? guidelineResponseBuilderService.buildGuidelineResponse(
//				WcagGuidelineA.INFORMATION_RELATIONSHIPS.getDescription(), level, wcagVersion, issueList, false,
//				successCount, successCount) : null;
//	}

//	public GuidelineResponse infoAndRelationships(Document doc, String url) {
//		List<IssueDetails> issueList = new ArrayList<>();
//		int totalCheckedElements = 0;
//		int successfulElements = 0;
//
//		WebDriver driver = WebDriverFactory.getDriver();
//
//		try {
//			driver.get(url);
//
//			// List of expected ARIA landmark roles
//			List<String> landmarkRoles = Arrays.asList("banner", "navigation", "main", "complementary", "contentinfo");
//
//			for (String role : landmarkRoles) {
//				List<WebElement> landmarks = driver.findElements(By.cssSelector("[role='" + role + "']"));
//				totalCheckedElements += landmarks.size();
//
//				
////				public void addIssueSelenium(List<IssueDetails> issueList, List<WebElement> landmarks, String tempImg, String issueDescription,
////						String level,WebElement webElement,	WebDriver driver);
////				
//				
//				
//				if (landmarks.isEmpty()) {
//					// Log missing landmarks
//					String issueDescription = "Missing required landmark role: " + role;
//					iGuidelineDataTransformer.addIssueSelenium(issueList, landmarks, "", issueDescription, "A",  driver);
//				} else {
//					for (WebElement landmark : landmarks) {
//						if (landmark != null) {
//							String htmlSnippet = landmark.getAttribute("outerHTML");
//							String cssSelector = generateValidCssSelector(landmark);
//
//							// Check if multiple landmarks of the same role exist and lack proper labels
//							boolean hasAriaLabel = landmark.getAttribute("aria-label") != null;
//							String issueDescription = "Landmark role '" + role + "' is correctly used.";
//
//							if (landmarks.size() > 1 && !hasAriaLabel) {
//								issueDescription = "Multiple '" + role + "' landmarks found without labels.";
//							}
//
//							if (!issueDescription.isEmpty()) {
//								iGuidelineDataTransformer.addIssueSelenium(issueList, landmarks, htmlSnippet,
//										issueDescription, "A", driver);
//							}
//							successfulElements++;
//						}
//					}
//				}
//			}
//
//			// Check Semantic HTML Elements
//			List<String> semanticElements = Arrays.asList("header", "nav", "main", "article", "section", "aside");
//			for (String tag : semanticElements) {
//				List<WebElement> elements = driver.findElements(By.tagName(tag));
//				totalCheckedElements += elements.size();
//				if (elements.isEmpty()) {
//					String issueDescription = "Missing semantic tag: <" + tag + ">";
//					iGuidelineDataTransformer.addIssueSelenium(issueList, null, "", issueDescription, "A", null);
//				} else {
//					successfulElements += elements.size();
//				}
//			}
//
//			// Check Headings
//			List<WebElement> headings = driver.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));
//			for (WebElement heading : headings) {
//				totalCheckedElements++;
//				if (heading.getAttribute("role") == null) {
//					iGuidelineDataTransformer.addIssueSelenium(issueList, null, heading.getAttribute("outerHTML"),
//							"Heading is missing 'role=heading'.", "A", driver);
//				} else {
//					successfulElements++;
//				}
//			}
//
//			// Check Tables
//			List<WebElement> tables = driver.findElements(By.tagName("table"));
//			for (WebElement table : tables) {
//				totalCheckedElements++;
//				boolean hasColumnHeaders = !table.findElements(By.cssSelector("th[scope='col']")).isEmpty();
//				boolean hasRowHeaders = !table.findElements(By.cssSelector("th[scope='row']")).isEmpty();
//				boolean isLayoutTable = table.getAttribute("role") != null
//						&& table.getAttribute("role").equals("presentation");
//
//				if (!hasColumnHeaders || !hasRowHeaders || isLayoutTable) {
//					iGuidelineDataTransformer.addIssueSelenium(issueList, null, table.getAttribute("outerHTML"),
//							"Table accessibility issue.", "A", driver);
//				} else {
//					successfulElements++;
//				}
//			}
//
//			// Check Forms and Labels
//			List<WebElement> inputs = driver.findElements(By.cssSelector("input, select, textarea"));
//			for (WebElement input : inputs) {
//				totalCheckedElements++;
//				boolean hasLabel = input.getAttribute("aria-label") != null
//						|| input.getAttribute("aria-labelledby") != null
//						|| (!input.getAttribute("id").isEmpty()
//								&& !driver.findElements(By.cssSelector("label[for='" + input.getAttribute("id") + "']"))
//										.isEmpty());
//
//				if (!hasLabel) {
//					iGuidelineDataTransformer.addIssueSelenium(issueList, null, input.getAttribute("outerHTML"),
//							"Form control missing accessible label.", "A", driver);
//				} else {
//					successfulElements++;
//				}
//			}
//
//			// Check Lists
//			List<WebElement> lists = driver.findElements(By.cssSelector("ul, ol"));
//			for (WebElement list : lists) {
//				totalCheckedElements++;
//				List<WebElement> items = list.findElements(By.tagName("li"));
//				boolean hasValidItems = items.stream().anyMatch(item -> !item.getText().trim().isEmpty());
//
//				if (!hasValidItems) {
//					iGuidelineDataTransformer.addIssueSelenium(issueList, null, list.getAttribute("outerHTML"),
//							"List missing meaningful items.", "A", driver);
//				} else {
//					successfulElements++;
//				}
//			}
//
//			// Return guideline response
//			return guidelineResponseBuilderService.buildGuidelineResponse(
//					WcagGuidelineA.INFORMATION_RELATIONSHIPS.getDescription(), "A", wcagVersion, issueList,
//					issueList.isEmpty(), successfulElements, totalCheckedElements);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return guidelineResponseBuilderService.buildGuidelineResponse("Error executing guidelines.", "A",
//					wcagVersion, issueList, true, successfulElements, totalCheckedElements);
//		}
//	}

	public GuidelineResponse infoAndRelationships(Document doc, String url) {

		List<IssueDetails> issueList = new ArrayList<>();

		Elements formElements = doc.select("form");
		for (Element form : formElements) {

			Elements requiredFields = form.select("input[required], select[required], textarea[required]");
			for (Element field : requiredFields) {

				boolean hasLabel = field.hasAttr("aria-label") || field.previousElementSibling() != null;

				if (!hasLabel) {
					status = false;
					String key = "Required form field is not programmatically associated with a label or description";
					iGuidelineDataTransformer.addIssue(issueList, field, field.toString(),
							"Required form field is not programmatically associated with a label or description.",
							getFix(key));
				} else {

				}
			}
		}

		Elements tables = doc.select("table");
		for (Element table : tables) {

			boolean headers = table.select("th").size() > 0;
			boolean hasColumnHeaders = table.select("th[scope=col]").size() > 0;
			boolean hasRowHeaders = table.select("th[scope=row]").size() > 0;

			if (!hasColumnHeaders || !hasRowHeaders) {
				status = false;
				String key = "Table headers for rows and columns are missing or not programmatically determined";
				iGuidelineDataTransformer.addIssue(issueList, table, table.toString(),
						"Table headers for rows and columns are missing or not programmatically determined.",
						getFix(key));
			}
		}

		Elements listElements = doc.select("ul, ol");
		for (Element list : listElements) {

			Elements listItems = list.select("li");
			if (listItems.isEmpty()) {
				status = false;
				String key = "List is missing list items, breaking the structural relationship";
				iGuidelineDataTransformer.addIssue(issueList, list, list.toString(),
						"List is missing list items, breaking the structural relationship.", getFix(key));
			}
		}

		Elements menuElements = doc.select("[id*=menu], [class*=menu]");
		for (Element menu : menuElements) {

			boolean insideNav = menu.parents().stream().anyMatch(parent -> parent.tagName().equals("nav"));
			boolean hasValidRole = menu.hasAttr("role")
					&& (menu.attr("role").equals("navigation") || menu.attr("role").equals("menu"));

			if (!insideNav && !hasValidRole) {
				status = false;
				String key = "Menu Element is Not Inside a <nav> and Lacks Role='Navigation' or 'Menu'";
				iGuidelineDataTransformer.addIssue(issueList, menu, menu.toString(), key, getFix(key));
			}
		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Build and return the GuidelineResponse with the results
		return guidelineResponseBuilderService.buildGuidelineResponse(
				WcagGuidelineA.INFORMATION_RELATIONSHIPS.getDescription(), level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

//	private String generateCssSelector(WebElement element) {
//		if (element == null) {
//			throw new IllegalArgumentException("Element cannot be null");
//		}
//
//		StringBuilder selector = new StringBuilder();
//		WebElement current = element;
//
//		while (current != null) {
//			String tagName = current.getTagName();
//			String id = current.getAttribute("id");
//			String classList = current.getAttribute("class");
//
//			// If the element has an ID, use it (IDs are unique, so we stop here)
//			if (id != null && !id.isEmpty()) {
//				selector.insert(0, "#" + id);
//				break; // ID is unique, so no need to continue
//			} else {
//				StringBuilder part = new StringBuilder(tagName);
//
//				// If the element has classes, include them in the selector
//				if (classList != null && !classList.isEmpty()) {
//					String[] classes = classList.split("\\s+");
//					for (String cls : classes) {
//						part.append(".").append(cls);
//					}
//				}
//
//				// Handle sibling indexing for non-unique elements
//				List<WebElement> siblings = current.findElements(By.xpath("preceding-sibling::" + tagName));
//				if (siblings.size() > 1) {
//					int index = siblings.size() + 1; // XPath indexes start at 1
//					part.append(":nth-of-type(").append(index).append(")");
//				}
//
//				// Add the part to the selector and navigate upwards
//				selector.insert(0, part + " > ");
//			}
//
//			// Move to the parent element
//			try {
//				current = current.findElement(By.xpath("..")); // Move to parent element
//			} catch (NoSuchElementException e) {
//				break; // If no parent exists, stop the loop
//			}
//		}
//
//		// Remove the trailing " > " if present
//		return selector.toString().replaceAll(" > $", "");
//	}

	public GuidelineResponse meaningfulSequence(Document doc) {

		List<IssueDetails> issueList = new ArrayList<>();

		Elements elements = doc
				.select("div, section, header, footer, article, aside, nav, main, h1, h2, h3, p, ol, ul, li, table");
		for (Element element : elements) {

			boolean isValidSequence = true;

			if (element.tagName().equals("article") || element.tagName().equals("section")) {
				Elements siblings = element.parent().children();
				int index = siblings.indexOf(element);

				for (int i = 0; i < siblings.size(); i++) {
					if (i != index) {
						Element sibling = siblings.get(i);

						if (sibling.tagName().equals(element.tagName()) && sibling.siblingIndex() < index) {
							isValidSequence = false;
						}
					}
				}
			}

			if (!isValidSequence) {
				status = false;
				String key = "The order of content elements breaks meaningful sequence or relationship";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(),
						"The order of content elements breaks meaningful sequence or relationship.", getFix(key));
			}
		}

		Elements navigationSections = doc.select("nav, header, footer");
		for (Element section : navigationSections) {

			boolean isNavigationSectionValid = true;
			Elements siblings = section.parent().children();
			int index = siblings.indexOf(section);

			if (index > 0) {
				Element prevSibling = siblings.get(index - 1);
				if (prevSibling.tagName().equals("nav") || prevSibling.tagName().equals("header")) {

					isNavigationSectionValid = false;
				}
			}

			if (!isNavigationSectionValid) {
				status = false;

				// public void addIssueUsingJsoupForCss(List<IssueDetails> issueList, Element
				// element, String snippetHtml,
				// String issueDescription, String level)

				String key = "Navigation section appears out of order, potentially breaking meaningful sequence";
				iGuidelineDataTransformer.addIssueUsingJsoupForCss(issueList, section, section.toString(),
						"Navigation section appears out of order, potentially breaking meaningful sequence.",
						getFix(key));
			}
		}

		// Calculate issue and success count
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Build and return the GuidelineResponse with the results
		return guidelineResponseBuilderService.buildGuidelineResponse(
				WcagGuidelineA.MEANINGFUL_SEQUENCE.getDescription(), level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	public GuidelineResponse sensoryCharacteristics(Document doc) {

		List<IssueDetails> issueList = new ArrayList<>();

		Elements instructionsElements = doc.select("p, li, div, span, a");
		for (Element element : instructionsElements) {

			String instructionText = element.text().toLowerCase();
			boolean reliesOnSensoryCharacteristics = false;

			if (instructionText.contains("red") || instructionText.contains("green") || instructionText.contains("blue")
					|| instructionText.contains("circle") || instructionText.contains("square")
					|| instructionText.contains("right") || instructionText.contains("left")
					|| instructionText.contains("top") || instructionText.contains("bottom")
					|| instructionText.contains("above") || instructionText.contains("below")) {

				if (!instructionText.contains("label") && !instructionText.contains("name")
						&& !instructionText.contains("next") && !instructionText.contains("button")) {

					reliesOnSensoryCharacteristics = true;
				}
			}

			if (reliesOnSensoryCharacteristics) {
				status = false;
				String key = "Instructions rely solely on sensory characteristics (e.g., color, shape, or location)";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(),
						"Instructions rely solely on sensory characteristics (e.g., color, shape, or location).",
						getFix(key));
			}
		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(
				WcagGuidelineA.SENSORY_CHARACTERISTICS.getDescription(), level, wcagVersion, issueList, false,
				successCount, issueCount);
	}

//	public GuidelineResponse useOfColor(Document doc) {
//
//		List<IssueDetails> issueList = new ArrayList<>();
//		int totalCheckedElements = 0;
//		int successfulElements = 0;
//
//		Elements visualElements = doc.select("p, li, div, span, a, button, input, table");
//		for (Element element : visualElements) {
//			totalCheckedElements++;
//
//			String style = element.attr("style").toLowerCase();
//			String color = getElementColor(element);
//			String backgroundColor = getElementBackgroundColor(element);
//
//			boolean reliesOnColorOnly = isColorUsedOnly(style, color, backgroundColor);
//
//			if (reliesOnColorOnly) {
//				status = false;
//				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(),
//						"Information is conveyed solely using color, which does not meet accessibility requirements.",
//						level);
//			} else {
//				successfulElements++;
//			}
//		}
//
//		int issueCount = issueList.size();
//		int successCount = successfulElements;
//
//		// Build and return the response
////		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
////				successCount, issueCount);
//
//		return (issueList != null && !issueList.isEmpty())
//				? guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.USE_OF_COLOR.getDescription(),
//						level, wcagVersion, issueList, false, successCount, issueCount)
//				: null;
//	}
//
//	@Override
//	public GuidelineResponse useOfColor(Document doc) {
//		List<IssueDetails> issueList = new ArrayList<>();
//
//		Elements visualElements = doc.select("p, li, div, span, a, button, input, table");
//
//		for (Element element : visualElements) {
//
//			String style = element.attr("style").toLowerCase();
//			String color = getElementColor(element);
//			String backgroundColor = getElementBackgroundColor(element);
//			boolean reliesOnColorOnly = isColorUsedOnly(style, color, backgroundColor);
//
//			// Check if the element is a hyperlink and needs additional checks
//			boolean isHyperlink = element.tagName().equals("a");
//			boolean isDistinguishable = isHyperlink && isHyperlinkDistinguishable(element);
//
//			if (reliesOnColorOnly || (isHyperlink && !isDistinguishable)) {
//				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(), isHyperlink
//						? "Hyperlink is not visually distinguishable from surrounding text, violating accessibility guidelines."
//						: "Information is conveyed solely using color, which does not meet accessibility requirements.",
//						level);
//			} else {
//
//			}
//		}
//
//		// Calculate issue and success count
//		int issueCount = issueList.size();
//		int totalChecked = getTotalCheckedElements(doc);
//		int successCount = totalChecked - issueCount;
//
//		// Update global counts
//		updateCounts(issueCount, successCount);
//
//		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.USE_OF_COLOR.getDescription(),
//				level, wcagVersion, issueList, status, successCount, issueCount);
//	}

	@Override
	public GuidelineResponse useOfColor(Document doc) {
		List<IssueDetails> issueList = new ArrayList<>();

		Elements visualElements = doc.select("p, li, div, span, a, button, input, table");

		for (Element element : visualElements) {
			String style = element.attr("style").toLowerCase();
			String color = getElementColor(element);
			String backgroundColor = getElementBackgroundColor(element);
			boolean reliesOnColorOnly = isColorUsedOnly(style, color, backgroundColor);

			// Check if the element is a hyperlink and needs additional checks
			boolean isHyperlink = element.tagName().equals("a");
			boolean isDistinguishable = isHyperlink && isHyperlinkDistinguishable(element);

			if (reliesOnColorOnly || (isHyperlink && !isDistinguishable)) {
				String Key = isHyperlink
						? "Hyperlink is not visually distinguishable from surrounding text, violating accessibility guidelines"
						: "Information is conveyed solely using color, which does not meet accessibility requirements";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(), isHyperlink
						? "Hyperlink is not visually distinguishable from surrounding text, violating accessibility guidelines."
						: "Information is conveyed solely using color, which does not meet accessibility requirements.",
						getFix(Key));
			}
		}

		// Fix totalChecked calculation
		int totalChecked = visualElements.size(); // Instead of getTotalCheckedElements(doc)
		int issueCount = issueList.size();
		int successCount = totalChecked - issueCount;

		// Prevent negative successCount
		// successCount = Math.max(successCount, 0);

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.USE_OF_COLOR.getDescription(),
				level, wcagVersion, issueList, status, successCount, totalChecked);
	}

	/**
	 * Checks if a hyperlink is distinguishable based on non-color visual cues.
	 */
	private boolean isHyperlinkDistinguishable(Element link) {
		String style = link.attr("style").toLowerCase();

		// Check for non-color visual cues: underline, bold, italic, different font
		// type/size
		boolean hasUnderline = style.contains("text-decoration: underline");
		boolean hasBold = style.contains("font-weight: bold");
		boolean hasItalic = style.contains("font-style: italic");
		boolean hasFontSizeChange = style.contains("font-size: larger") || style.contains("font-size: smaller");
		boolean hasFontFamilyChange = style.contains("font-family");

		// Check for hover/focus effect making it more noticeable
		boolean hasHoverEffect = link.attr("onmouseover").contains("text-decoration='underline'");

		return hasUnderline || hasBold || hasItalic || hasFontSizeChange || hasFontFamilyChange || hasHoverEffect;
	}

	private String getElementColor(Element element) {

		String style = element.attr("style").toLowerCase();
		if (style.contains("color:")) {
			return style.substring(style.indexOf("color:") + 6).split(";")[0].trim();
		}

		return "";
	}

	private String getElementBackgroundColor(Element element) {

		String style = element.attr("style").toLowerCase();
		if (style.contains("background-color:")) {
			return style.substring(style.indexOf("background-color:") + 17).split(";")[0].trim();
		}
		return "";
	}

	private boolean isColorUsedOnly(String style, String color, String backgroundColor) {

		return (color.isEmpty() && backgroundColor.isEmpty());
	}

	public GuidelineResponse evaluateAudioControl(Document doc) {
		final String guidelineNumber = "1.4.2 Audio Control";

		if (doc == null) {
			logger.error("Received null document. Cannot process WCAG guideline 1.4.2.");
			return guidelineResponseBuilderService.buildGuidelineResponse(guidelineNumber, level, wcagVersion, null,
					false, 0, 0);
		}

		List<IssueDetails> issuesList = new ArrayList<>();

		try {

			checkAudioForAutoPlay(doc, issuesList, level);

			checkForAudioControlMechanism(doc, issuesList, level);
		} catch (Exception e) {
			logger.error("Error processing the document for WCAG guideline 1.4.2", e);

		}

		int issueCount = issuesList.size();
		int totalCheckedElements = getTotalCheckedElementsForAudio(doc);
		int successCount = totalCheckedElements - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guidelineNumber, level, wcagVersion, issuesList,
				status, successCount, issueCount);
	}

	private void checkAudioForAutoPlay(Document doc, List<IssueDetails> issuesList, String accessibilityLevel) {
		try {
			Elements audios = doc.select("audio[autoplay]");
			for (Element audio : audios) {

				if (audio.attr("autoplay") != null) {
					String key = "Audio automatically plays for more than 3 seconds";
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, audio, audio.toString(), key, getFix(key));
				}
			}
		} catch (Exception e) {
			logger.error("Error checking audio for autoplay", e);
		}
	}

	private void checkForAudioControlMechanism(Document doc, List<IssueDetails> issuesList, String accessibilityLevel) {
		try {
			Elements audios = doc.select("audio");
			for (Element audio : audios) {
				boolean hasControl = audio.hasAttr("controls");
				boolean hasVolumeControl = audio.select("input[type='range'][aria-label='volume']").size() > 0;

				if (!hasControl && !hasVolumeControl) {
					String key = "No control mechanism to pause/stop or control audio volume";
					iGuidelineDataTransformer.addIssueUsingJsoup(issuesList, audio, audio.toString(), key, getFix(key));
				}
			}
		} catch (Exception e) {
			logger.error("Error checking for audio control mechanism", e);
		}
	}

	private int getTotalCheckedElementsForAudio(Document doc) {
		try {
			return doc.select("audio").size();
		} catch (Exception e) {
			logger.error("Error calculating total checked audio elements", e);
			return 0;
		}
	}

	public GuidelineResponse validateImagesOfText(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();

			driver.get(url);

			List<WebElement> imageElements = driver.findElements(By.tagName("img"));

			for (WebElement element : imageElements) {

				try {
					String altText = element.getAttribute("alt");
					String src = element.getAttribute("src");

					if (altText != null && !altText.isEmpty()) {
						if (isImageOfText(src)) {
							if (!isEssentialImageOfText(src)) {
								status = false;
								String key = "Image of text is used where it is not necessary or where text could be used instead";
								iGuidelineDataTransformer.addIssue(issueList, element, altText,
										"Image of text is used where it is not necessary or where text could be used instead.",
										getFix(key));
							}
						}
					}

				} catch (Exception e) {

				}
			}
		} catch (Exception e) {

		}

		// Calculate issue and success count
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;
		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.IMAGES_OF_TEXT.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private boolean isImageOfText(String src) {
		return src != null && (src.endsWith(".gif") || src.endsWith(".jpg") || src.endsWith(".png"));
	}

	private boolean isEssentialImageOfText(String src) {
		return src != null && src.contains("logo");
	}

	@Override
	public GuidelineResponse validateKeyboardAccessibility(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();

			driver.get(url);

			List<WebElement> interactiveElements = driver.findElements(
					By.cssSelector("a, button, input, textarea, select, [tabindex], [role='button'], [role='link']"));

			logger.info("Found {} interactive elements for keyboard accessibility check.", interactiveElements.size());

			for (WebElement element : interactiveElements) {

				try {
					if (!element.isDisplayed() || !element.isEnabled()) {
						logger.debug("Skipping non-visible or disabled element: {}", element.getTagName());
						continue;
					}

					boolean isKeyboardOperable = isKeyboardOperable(element);

					if (!isKeyboardOperable) {
						status = false;
						String key = "The element is not operable via the keyboard or lacks appropriate keyboard handling";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"The element is not operable via the keyboard or lacks appropriate keyboard handling.",
								getFix(key));
					} else {

					}
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {

		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.KEYBOARD.getDescription(), level,
				wcagVersion, issueList, status, successCount, issueCount);
	}

	private boolean isKeyboardOperable(WebElement element) {
		String tagName = element.getTagName();
		String role = element.getAttribute("role");
		String tabIndex = element.getAttribute("tabindex");

		return tagName.matches("input|button|a|textarea|select") || (role != null && role.matches("button|link"))
				|| (tabIndex != null && Integer.parseInt(tabIndex) >= 0);
	}

	@Override
	public GuidelineResponse validateNoKeyboardTrap(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();

			driver.get(url);

			List<WebElement> focusTrapElements = driver
					.findElements(By.cssSelector("div[role='dialog'], modal, .focus-trap"));

			;

			for (WebElement element : focusTrapElements) {

				try {
					if (!element.isDisplayed() || !element.isEnabled()) {
						logger.debug("Skipping non-visible or disabled element: {}", element.getTagName());
						continue;
					}

					boolean isFocusTrap = isFocusTrapped(element);

					if (isFocusTrap) {
						status = false;
						String key = "The element traps the keyboard focus and does not allow users to exit the component";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"The element traps the keyboard focus and does not allow users to exit the component.",
								getFix(key));
					}
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {

		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;
		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.NO_KEYBOARD_TRAP.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private boolean isFocusTrapped(WebElement element) {
		try {
			List<WebElement> focusableElements = element
					.findElements(By.cssSelector("a, button, input, textarea, select, [tabindex]"));

			if (focusableElements.size() < 2) {
				return false;
			}

			WebElement firstFocusable = focusableElements.get(0);
			WebElement lastFocusable = focusableElements.get(focusableElements.size() - 1);

			return firstFocusable.equals(lastFocusable);
		} catch (Exception e) {
			logger.error("Error checking if focus is trapped: {}", e.getMessage(), e);
			return false;
		}
	}

	@Override
	public GuidelineResponse validateTimingAdjustable(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();

			driver.get(url);

			List<WebElement> timedElements = driver.findElements(By
					.cssSelector("[data-timer], [role='alert'], [data-time-limit], [class*='timer'], [id*='timeout']"));

			logger.info("Found {} potential elements with timing constraints.", timedElements.size());

			for (WebElement element : timedElements) {

				try {
					if (!element.isDisplayed()) {
						logger.debug("Skipping non-visible element: {}", element.getTagName());
						continue;
					}

					boolean hasTimingControls = hasTimingAdjustmentControls(driver, element);

					if (!hasTimingControls) {
						status = false;
						String key = "This element imposes a time limit without providing mechanisms to turn off, extend, or adjust the time limit";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"This element imposes a time limit without providing mechanisms to turn off, extend, or adjust the time limit.",
								getFix(key));
					} else {

					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;
		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.TIMING_ADJUSTABLE.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private boolean hasTimingAdjustmentControls(WebDriver driver, WebElement element) {
		try {
			List<WebElement> controls = driver
					.findElements(By.cssSelector("button[aria-label*='pause'], button[aria-label*='extend'], "
							+ "button[aria-label*='stop'], [role='button'][data-extend], "
							+ "[role='alert'][data-extend]"));

			for (WebElement control : controls) {
				if (control.isDisplayed() && control.isEnabled()) {
					return true;
				}
			}

			WebElement settingsLink = driver
					.findElement(By.cssSelector("a[href*='settings'], button[aria-label*='settings']"));
			return settingsLink != null && settingsLink.isDisplayed();
		} catch (Exception e) {

			return false;
		}
	}

	@Override
	public GuidelineResponse validatePauseStopHide(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();

			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			List<WebElement> dynamicElements = driver.findElements(By.cssSelector(
					"[data-moving], [data-blink], [data-scroll], [role='marquee'], [class*='ticker'], [id*='moving'], "
							+ "[class*='animation'], [data-auto-update]"));

			for (WebElement element : dynamicElements) {

				try {
					if (!element.isDisplayed()) {

						continue;
					}

					boolean hasControlMechanisms = hasPauseStopHideControls(driver, element);

					if (!hasControlMechanisms) {
						status = false;
						String key = "This element has moving, blinking, or auto-updating content without providing mechanisms to pause, stop, or hide it";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"This element has moving, blinking, or auto-updating content without providing mechanisms to pause, stop, or hide it.",
								getFix(key));
					} else {

					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;
		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.PAUSE_STOP_HIDE.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private boolean hasPauseStopHideControls(WebDriver driver, WebElement element) {
		try {

			List<WebElement> controls = driver.findElements(By
					.cssSelector("button[aria-label*='pause'], button[aria-label*='stop'], button[aria-label*='hide'], "
							+ "button[aria-label*='control'], [role='button'][data-pause], [role='button'][data-stop], "
							+ "[role='button'][data-hide], [class*='control-button']"));

			for (WebElement control : controls) {
				if (control.isDisplayed() && control.isEnabled()) {
					return true;
				}
			}

			WebElement settingsLink = driver
					.findElement(By.cssSelector("a[href*='settings'], button[aria-label*='settings']"));
			return settingsLink != null && settingsLink.isDisplayed();
		} catch (Exception e) {

			return false;
		}
	}

	@Override
	public GuidelineResponse validateFlashingContent(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);

			if (isFlashingContentExceedsThreshold(driver)) {
				status = false;
				logger.error("Flashing content exceeds threshold on page.");
				String key = "Flashing content exceeds 3 flashes per second threshold";
				issueList.add(new IssueDetails("Flashing content exceeds 3 flashes per second threshold.", level,
						wcagVersion, wcagVersion, getFix(key)));
			} else {

			}
		} catch (Exception e) {
			status = false;

		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.THREE_FLASHES.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private boolean isFlashingContentExceedsThreshold(WebDriver driver) {
		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

		try {

			String flashCheckScript = "return Array.from(document.querySelectorAll('*')).filter(element => {"
					+ "  const style = window.getComputedStyle(element);"
					+ "  const animationDuration = style.getPropertyValue('animation-duration');"
					+ "  const animationIterationCount = style.getPropertyValue('animation-iteration-count');"
					+ "  // Detect animations that may cause flashing (duration < 1s and repeated indefinitely)"
					+ "  return (parseFloat(animationDuration) < 1 && animationIterationCount === 'infinite');"
					+ "}).map(element => element.tagName);";

			List<String> flashingElements = (List<String>) jsExecutor.executeScript(flashCheckScript);

			if (!flashingElements.isEmpty()) {

				return true;
			} else {
			}

			String videoFlashCheckScript = "return Array.from(document.querySelectorAll('video')).filter(video => {"
					+ "   const isFastPlayback = video.playbackRate > 3;" // Video playback rate exceeds threshold
					+ "   const hasFlashingContent = video && video.duration && video.duration > 0 && video.currentTime % 1 === 0;" // Placeholder
					+ "   return isFastPlayback || hasFlashingContent;" + "}).map(video => {"
					+ "   return { src: video.src, playbackRate: video.playbackRate, duration: video.duration };"
					+ "});";

			List<Map<String, Object>> fastVideos = (List<Map<String, Object>>) jsExecutor
					.executeScript(videoFlashCheckScript);

			if (!fastVideos.isEmpty()) {
				fastVideos.forEach(video -> {

				});
				return true;
			} else {
				logger.info("No flashing content detected in videos based on playback rate.");
			}

		} catch (JavascriptException e) {
			logger.error("JavaScript execution failed while checking flashing content: {}", e.getMessage(), e);
			return false;
		} catch (Exception e) {
			logger.error("Error during flashing content check: {}", e.getMessage(), e);
			return false;
		}

		return false;
	}
	
	@Override
	public GuidelineResponse validateBypassBlocks(Document doc, String url) {

		WebDriver driver = WebDriverFactory.getDriver(url); // Ensure WebDriver is initialized
		List<IssueDetails> issueList = new ArrayList<>();
		logger.info("...validateBypassBlocks...");

		// JavaScript-based validation using Selenium
		try {
			if (driver != null) {
				JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
				String bypassBlocksScript = "var skipLink = document.createElement('a');"
						+ "skipLink.setAttribute('href', '#main-content');"
						+ "skipLink.setAttribute('class', 'skip-link');"
						+ "skipLink.setAttribute('style', 'position:absolute;top:0;left:0;z-index:1000;background-color:#000;color:#fff;padding:10px;');"
						+ "skipLink.innerText = 'Skip to Main Content';"
						+ "document.body.insertBefore(skipLink, document.body.firstChild);"
						+ "return document.querySelector('.skip-link') ? 'Success' : 'Failure';";

				String result = (String) jsExecutor.executeScript(bypassBlocksScript);
				if (!"Success".equals(result)) {
					String key = "Skip link could not be added dynamically";
					iGuidelineDataTransformer.addIssue(issueList, doc.select("html").first(),
							"Skip link could not be added dynamically.", level, getFix(key));
				}
			} else {
				logger.warn("WebDriver is not initialized. Skipping JavaScript-based validation.");
			}
		} catch (JavascriptException e) {
			logger.error("JavaScript execution failed: " + e.getMessage());
		}

		// JSoup-based static validation
		Elements menus = doc.select("[class*='menu']");
		for (Element menu : menus) {
			Element parent = menu.closest("nav, header, aside");
			if (parent == null) {
				String key = "Menu must be placed within a landmark region";
				iGuidelineDataTransformer.addIssue(issueList, menu, menu.toString(), key, getFix(key));
			}
		}

		List<String> validSkipLinks = Arrays.asList("#main", "#content", "#main-content", "#primary", "#skiptarget",
				"#main-wrapper", "#maincontent");

		Elements interactiveElements = doc.select("a[href], button, input, select, textarea");
		if (!interactiveElements.isEmpty()) {
			Element firstElement = interactiveElements.first();
			if (firstElement.tagName().equals("a")) {
				String hrefValue = firstElement.attr("href").trim();
				if (!hrefValue.startsWith("#") || !validSkipLinks.contains(hrefValue)) {
					String key = "First interactive item must be a skip link to main content";
					iGuidelineDataTransformer.addIssue(issueList, doc.select("html").first(), hrefValue, key,
							getFix(key));
				}
			}
		} else {
			String key = "First interactive item must be a skip link to main content";
			iGuidelineDataTransformer.addIssue(issueList, doc.select("html").first(), url, key, getFix(key));
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		return guidelineResponseBuilderService.buildGuidelineResponse(WcagGuidelineA.BYPASS_BLOCKS.getDescription(),
				level, wcagVersion, issueList, status, successCount, issueCount);
	}

	@Override
	public GuidelineResponse evaluatePageTitle(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);

			String pageTitle = driver.getTitle();

			if (pageTitle == null || pageTitle.trim().isEmpty()) {
				status = false;
				addIssue(issueList, doc, "No title found for the page.", pageTitle);
			} else {

				if (pageTitle.equals("Untitled") || pageTitle.toLowerCase().contains("index")
						|| pageTitle.toLowerCase().contains("home")) {
					status = false;
					addIssue(issueList, doc,
							"Title is not descriptive. Consider updating the title to reflect the content or purpose of the page.",
							pageTitle);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.PAGE_TITLED.getDescription(), level, wcagVersion, issueList,
				status, successCount, issueCount);
	}

	private void addIssue(List<IssueDetails> issueList, Element link, String issueMessage, String href) {
		iGuidelineDataTransformer.transform(issueMessage, issueMessage, issueMessage, issueList, 0, 0);
	}

	private GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
			List<IssueDetails> issues, boolean status, int successCount, int issueCount) {
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issues, status,
				successCount, issueCount);
	}

	@Override
	public GuidelineResponse evaluateFocusOrder(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);

			Elements focusableElements = doc.select("a, button, input, select, textarea, [tabindex]");
			for (Element element : focusableElements) {

				String cssSelector = generateValidCssSelector((WebElement) element);
				if (cssSelector == null || cssSelector.isEmpty()) {

					continue;
				}

				try {
					WebElement seleniumElement = driver.findElement(By.cssSelector(cssSelector));
					int tabindex = getTabindex(seleniumElement);

					if (tabindex != Integer.MAX_VALUE) {

						boolean isTabOrderCorrect = checkTabOrder(seleniumElement, tabindex, driver);

						if (!isTabOrderCorrect) {
							status = false;
							addIssue(issueList, element, tabindex);
						}
					}
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {

		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.FOCUS_ORDER.getDescription(), level, wcagVersion, issueList,
				status, successCount, issueCount);
	}

	private String generateValidCssSelector(WebElement field) {
		try {
			String cssSelector = ((Element) field).cssSelector();
			return (cssSelector != null && !cssSelector.trim().isEmpty()) ? cssSelector : null;
		} catch (Exception e) {

			return null;
		}
	}

	private int getTabindex(WebElement element) {
		try {
			String tabindexStr = element.getAttribute("tabindex");
			return (tabindexStr != null) ? Integer.parseInt(tabindexStr) : Integer.MAX_VALUE;
		} catch (NumberFormatException e) {

			return Integer.MAX_VALUE;
		}
	}

	private boolean checkTabOrder(WebElement element, int tabindex, WebDriver driver) {

		List<WebElement> focusableElements = driver
				.findElements(By.cssSelector("a, button, input, select, textarea, [tabindex]"));

		int currentPosition = focusableElements.indexOf(element);

		if (currentPosition == -1) {

			return false;
		}

		for (int i = 0; i < focusableElements.size(); i++) {
			WebElement focusableElement = focusableElements.get(i);
			int currentTabindex = getTabindex(focusableElement);

			if (currentTabindex == -1) {
				currentTabindex = 0;
			}

			if (i == currentPosition) {
				continue;
			}

			if (i > currentPosition) {
				int nextTabindex = getTabindex(focusableElements.get(i));

				if (nextTabindex == -1) {
					nextTabindex = 0;
				}

				if (nextTabindex < currentTabindex) {

					return false;
				}
			}
		}

		return true;
	}

	private void addIssue(List<IssueDetails> issueList, Element element, int tabindex) {
//		TODO TITLE -->Tabindex value is incorrect for an element.
		String key = "Tabindex value is incorrect for an element";
		String issueMessage = "The tabindex value of %d is incorrect for element: %s".formatted(tabindex,
				element.tagName());
		iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(), issueMessage, getFix(key));
	}

	@Override
	public GuidelineResponse evaluateLinkPurpose(Document doc, String url) {
		boolean status = true; // Ensure status is initialized
		List<IssueDetails> issueList = new ArrayList<>();

		if (doc == null) {
			logger.error("Document is null, skipping evaluation.");
			return buildGuidelineResponse(WcagGuidelineA.LINK_PURPOSE.getDescription(), level, wcagVersion, null, false,
					0, 0);
		}

		try {
			Elements links = doc.select("a[href]");
			int totalChecked = links.size();
			logger.info("Total links found: {}", totalChecked);

			for (Element link : links) {
				String linkText = link.text().trim();
				String href = link.attr("href");

				if (linkText.isEmpty() || isGenericLinkText(linkText)) {
					status = false;// TODO TITLE -->Link text is empty or too generic
					addIssue(issueList, link, "Link text is empty or generic ('Click here').", href);
					logger.warn("Issue added: Link text is empty or generic [{}]", href);
					status = false;
				} else if (!hasProgrammaticContext(link)) {
					addIssue(issueList, link, "Link lacks sufficient programmatic context.", href);
					logger.warn("Issue added: Link lacks sufficient context [{}]", href);
					status = false;
				}
			}

		} catch (Exception e) {
			logger.error("Error while evaluating link purpose: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);
		logger.info("Evaluation completed: {} issues found, {} passed.", issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.LINK_PURPOSE.getDescription(), level, wcagVersion, issueList,
				status, successCount, issueCount);
	}

	/**
	 * Checks if the link text is generic and lacks meaningful information.
	 */
	private boolean isGenericLinkText(String linkText) {
		List<String> genericTexts = Arrays.asList("click here", "read more", "learn more", "details", "more");
		return genericTexts.stream().anyMatch(text -> linkText.equalsIgnoreCase(text));
	}

	/**
	 * Checks if the link has sufficient programmatic context.
	 */
	private boolean hasProgrammaticContext(Element link) {

		Element parent = link.parent();
		return parent != null && (!parent.text().isEmpty() && !isGenericLinkText(parent.text()));
	}

	public GuidelineResponse validateLanguageOfPage(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);

			List<WebElement> htmlElements = driver.findElements(By.cssSelector("html"));

			for (WebElement htmlElement : htmlElements) {

				try {
					String langAttribute = htmlElement.getAttribute("lang");
					if (langAttribute == null || langAttribute.trim().isEmpty()) {
						status = false;
						String key = "The <html> element is missing the lang attribute";
						iGuidelineDataTransformer.addIssue(issueList, htmlElement, "",
								"The <html> element is missing the lang attribute.", getFix(key));
					} else if (!isValidLanguageTag(langAttribute)) {
						status = false;
						String key = "The lang attribute on the <html> element contains an invalid language tag";
						iGuidelineDataTransformer.addIssue(issueList, htmlElement, langAttribute,
								"The lang attribute on the <html> element contains an invalid language tag.",
								getFix(key));
					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.LANGUAGE_OF_PAGE.getDescription(), level, wcagVersion, issueList,
				status, successCount, issueCount);
	}

	/**
	 * Validates whether the given language tag conforms to BCP 47 standards.
	 *
	 * @param langTag The language tag to validate.
	 * @return True if the language tag is valid; false otherwise.
	 */
	private boolean isValidLanguageTag(String langTag) {

		return langTag.matches("^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$");
	}

	@Override
	public GuidelineResponse validateOnFocus(Document doc, String url) {
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true; // Default status as true

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Find all focusable elements
			List<WebElement> focusableElements = driver.findElements(By.xpath(
					"//*[self::a or self::button or self::input or self::textarea or self::select or @tabindex]"));

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2)); // Replaces Thread.sleep()

			for (WebElement element : focusableElements) {
				Actions actions = new Actions(driver);
				actions.moveToElement(element).perform();

				String initialUrl = driver.getCurrentUrl();
				String initialTitle = driver.getTitle();

				element.click();
				wait.until(ExpectedConditions.or(ExpectedConditions.not(ExpectedConditions.urlToBe(initialUrl)),
						ExpectedConditions.not(ExpectedConditions.titleIs(initialTitle))));

				String currentUrl = driver.getCurrentUrl();
				String currentTitle = driver.getTitle();

				if (!initialUrl.equals(currentUrl) || !initialTitle.equals(currentTitle)) {
					String key = "Focus on this element triggers a context change (e.g., navigation or modal)";
					iGuidelineDataTransformer.addIssue(issueList, element, currentTitle,
							"Focus on this element triggers a context change (e.g., navigation or modal).",
							getFix(key));
					status = false;
				}
			}
		} catch (WebDriverException e) {
			logger.error("WebDriver error: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.ON_FOCUS.getDescription(), level, wcagVersion, issueList, status,
				successCount, issueCount);
	}

	public GuidelineResponse validateOnInput(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);

			List<WebElement> interactiveElements = driver.findElements(By
					.xpath("//*[self::select or self::input or self::textarea or @onchange or @onclick or @oninput]"));

			for (WebElement element : interactiveElements) {

				try {

					String initialUrl = driver.getCurrentUrl();
					String initialTitle = driver.getTitle();

					if (element.getTagName().equals("select")) {
						Select select = new Select(element);
						List<WebElement> options = select.getOptions();
						if (options.size() > 1) {
							select.selectByIndex(1);
							Thread.sleep(500);
						}
					} else if (element.getTagName().equals("input") || element.getTagName().equals("textarea")) {
						element.sendKeys("test input");
						Thread.sleep(500);
					}

					String currentUrl = driver.getCurrentUrl();
					String currentTitle = driver.getTitle();

					if (!initialUrl.equals(currentUrl) || !initialTitle.equals(currentTitle)) {
						String key = "Input on this element triggers a context change (e.g., navigation or modal) without warning";
						iGuidelineDataTransformer.addIssue(issueList, element, currentTitle,
								"Input on this element triggers a context change (e.g., navigation or modal) without warning.",
								getFix(key));
						status = false;
					}
				} catch (Exception e) {
					logger.error("Error validating input behavior: {}", e.getMessage());
				}
			}
		} catch (WebDriverException e) {
			logger.error("WebDriver encountered an issue: {}", e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected error during validation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);
		return buildGuidelineResponse(WcagGuidelineA.ON_INPUT.getDescription(), level, wcagVersion, issueList, status,
				successCount, issueCount);
	}

	public GuidelineResponse validateErrorIdentification(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {

			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			List<WebElement> inputElements = driver.findElements(By.cssSelector("input, select, textarea"));

			for (WebElement element : inputElements) {

				try {

					String errorText = getErrorMessageForField(element);
					if (errorText != null && !errorText.isEmpty()) {
						status = false;
						String key = "Input error detected. The field contains invalid data";
						iGuidelineDataTransformer.addIssue(issueList, element, errorText,
								"Input error detected. The field contains invalid data.", getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing input element: {}", e.getMessage(), e);
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.ERROR_IDENTIFICATION.getDescription(), level, wcagVersion,
				issueList, status, successCount, issueCount);
	}

	private String getErrorMessageForField(WebElement element) {

		String errorMessage = null;
		if (element.getAttribute("aria-invalid") != null && element.getAttribute("aria-invalid").equals("true")) {
			errorMessage = "This field has an error.";
		} else {
			List<WebElement> errorMessages = element.findElements(By.cssSelector(".error-message"));
			if (!errorMessages.isEmpty()) {
				errorMessage = errorMessages.get(0).getText();
			}
		}
		return errorMessage;
	}

	public GuidelineResponse validateLabelsOrInstructions(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Find all form input elements (input, select, textarea)
			List<WebElement> inputElements = driver.findElements(By.cssSelector("input, select, textarea"));

			for (WebElement element : inputElements) {

				try {
					// Check if the element has a label or instructions
					boolean hasLabelOrInstructions = hasLabelOrInstructionsForField(element);
					if (!hasLabelOrInstructions) {
						status = false;
						String key = "This form element is missing a label or instructions";
						String errorMessage = "Field is missing a label or instruction.";
						iGuidelineDataTransformer.addIssue(issueList, element, errorMessage,
								"This form element is missing a label or instructions.", getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing input element: {}", e.getMessage(), e);
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.LABELS_OR_INSTRUCTIONS.getDescription(), level, wcagVersion,
				issueList, status, successCount, issueCount);
	}

	private boolean hasLabelOrInstructionsForField(WebElement element) {
		// Check for a label element associated with the input
		WebElement label = getLabelForField(element);
		if (label != null && !label.getText().isEmpty()) {
			return true;
		}

		// Check for aria-describedby or aria-label attributes
		String ariaLabel = element.getAttribute("aria-label");
		String ariaDescribedBy = element.getAttribute("aria-describedby");

		if ((ariaLabel != null && !ariaLabel.isEmpty()) || (ariaDescribedBy != null && !ariaDescribedBy.isEmpty())) {
			return true;
		}

		// Check if the element has inline instructions or placeholder text
		String placeholder = element.getAttribute("placeholder");
		if (placeholder != null && !placeholder.isEmpty()) {
			return true;
		}

		// If none of the checks pass, return false
		return false;
	}

	private WebElement getLabelForField(WebElement element) {
		// Try to find a label element associated with the form control
		List<WebElement> labels = element.findElements(By.xpath("ancestor::label"));
		if (!labels.isEmpty()) {
			return labels.get(0);
		}
		return null;
	}

//	public GuidelineResponse validateNameRoleValue(Document doc, String url) {
//
//		WebDriver driver = null;
//		List<IssueDetails> issueList = new ArrayList<>();
//
//		try {
//			driver = WebDriverFactory.getDriver();
//			driver.get(url);
//			logger.info("Navigated to URL: {}", url);
//
//			// Find all interactive elements (e.g., form elements, links, and components
//			// generated by scripts)
//			List<WebElement> interactiveElements = driver
//					.findElements(By.cssSelector("input, select, textarea, button, a, [role]"));
//
//			for (WebElement element : interactiveElements) {
//
//				try {
//					// Check if the element has a programmatically determined name, role, and value
//					boolean hasNameRoleValue = hasValidNameRoleValue(element);
//					if (!hasNameRoleValue) {
//						status = false;
//						String errorMessage = "Element is missing valid name, role, or value.";
//						iGuidelineDataTransformer.addIssue(issueList, element, errorMessage,
//								"This element is missing a valid programmatically determined name, role, or value.",
//								level);
//					} else {
//
//					}
//				} catch (Exception e) {
//					logger.error("Error processing interactive element: {}", e.getMessage(), e);
//				}
//			}
//
//		} catch (Exception e) {
//			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
//		}
//
//		int issueCount = issueList.size();
//		int totalChecked = getTotalCheckedElements(doc);
//		int successCount = totalChecked - issueCount;
//
//		// Update global counts
//		updateCounts(issueCount, successCount);
//
//		return buildGuidelineResponse(WcagGuidelineA.NAME_ROLE_VALUE.getDescription(), level, wcagVersion, issueList,
//				status, successCount, issueCount);
//	}

	@Override
	public GuidelineResponse validateNameRoleValue(Document doc, String url) {
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Find all interactive elements (e.g., form elements, links, and ARIA
			// components)
			List<WebElement> interactiveElements = driver
					.findElements(By.cssSelector("input, select, textarea, button, a, [role]"));

			for (WebElement element : interactiveElements) {
				try {
					boolean hasNameRoleValue = hasValidNameRoleValue(element);
					if (!hasNameRoleValue) {
						status = false;
						String key = "This element is missing a valid programmatically determined name, role, or value";
						String errorMessage = "Element is missing valid name, role, or value.";
						iGuidelineDataTransformer.addIssue(issueList, element, errorMessage,
								"This element is missing a valid programmatically determined name, role, or value.",
								getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing interactive element: {}", e.getMessage(), e);
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();

		int totalChecked = getTotalCheckedElements(doc);

		int successCount = totalChecked - issueCount;

		// Prevent negative successCount
		// successCount = Math.max(successCount, 0);

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.NAME_ROLE_VALUE.getDescription(), level, wcagVersion, issueList,
				status, successCount, issueCount);
	}

	private boolean hasValidNameRoleValue(WebElement element) {
		// Check if the element has a programmatically determined name
		String name = getElementName(element);
		String role = getElementRole(element);
		String value = getElementValue(element);

		return (name != null && !name.isEmpty()) && (role != null && !role.isEmpty()) && (value != null);
	}

	private String getElementName(WebElement element) {
		// Check for name using ARIA attributes, title, or form labels
		String name = element.getAttribute("aria-label");
		if (name == null || name.isEmpty()) {
			name = element.getAttribute("aria-labelledby");
		}
		if (name == null || name.isEmpty()) {
			name = element.getAttribute("title");
		}
		if (name == null || name.isEmpty()) {
			name = element.getText(); // Fall back to visible text
		}
		return name;
	}

	private String getElementRole(WebElement element) {
		// Retrieve role from ARIA or HTML attributes
		String role = element.getAttribute("role");
		return (role != null && !role.isEmpty()) ? role : "default";
	}

	private String getElementValue(WebElement element) {
		// Check if the element is user-interactive (e.g., checkbox, radio button, input
		// field)
		String value = null;
		if (element instanceof WebElement) {
			value = element.getAttribute("value");
		}
		return value;
	}

	@Override
	public GuidelineResponse validateParsing(Document doc, String url) {

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Retrieve the document source code
			String pageSource = driver.getPageSource();

			// Check for invalid or missing start/end tags, element nesting issues, or
			// duplicate attributes
			if (!isValidHTML(pageSource)) {
				status = false;
				String key = "HTML contains invalid or incorrectly nested tags, or duplicate attributes";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, doc, wcagVersion, // FIXME SNIPPET missing
						"HTML contains invalid or incorrectly nested tags, or duplicate attributes.", getFix(key));
			}
		} catch (Exception e) {
			logger.error("Error processing page: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(WcagGuidelineA.PARSING.getDescription(), level, wcagVersion, issueList, status,
				successCount, issueCount);
	}

	private boolean isValidHTML(String pageSource) {

		try {

			Document doc = Jsoup.parse(pageSource);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private String getFix(String key) {
		try {
			return Optional.ofNullable(fixes.get(key)).map(IssueFixDetails::getFix).orElse("");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
