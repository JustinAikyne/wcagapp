package com.brahos.accessibilitychecker.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.helper.service.IGuidelineDataTransformer;
import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.model.IssueDetails;
import com.brahos.accessibilitychecker.model.IssueFixDetails;
import com.brahos.accessibilitychecker.model.WcagFixesData;
import com.brahos.accessibilitychecker.repository.AccessibilityIssueFixesRepository;
import com.brahos.accessibilitychecker.service.GuidelineResponseBuilderService;
import com.brahos.accessibilitychecker.service.HelperGuidelinesExecutorService;
import com.brahos.accessibilitychecker.service.WcagAccessibilityGuidelines_2_0_AAA;
import com.brahos.accessibilitychecker.utility.AccessibilityValidator;
import com.brahos.accessibilitychecker.utility.ReadabilityScore;
import com.brahos.accessibilitychecker.utility.WebDriverFactory;

import ch.qos.logback.classic.Logger;
import jakarta.annotation.PostConstruct;

@Service
public class WcagAccessibilityGuidelinesImpl_2_0_AAA implements WcagAccessibilityGuidelines_2_0_AAA {

	private static Logger logger = (Logger) LoggerFactory.getLogger(AccessibilityGuidelinesServiceImpl_2_0_A.class);

	private HelperGuidelinesExecutorService helperGuidelinesExecutorService;

	private GuidelineResponseBuilderService guidelineResponseBuilderService;

	private IGuidelineDataTransformer iGuidelineDataTransformer;

	private WebDriverFactory webDriverFactory;

	private String level = "AAA";

	private String wcagVersion = "2.0";

	private boolean status = true;

	@Autowired
	private AccessibilityValidator accessibilityValidator;

	private static final Pattern UNIT_PATTERN = Pattern.compile("(-?\\d*\\.?\\d+)(px|em|%|rem|deg|rad|grad)?");

	@Autowired
	private AccessibilityIssueFixesRepository issueFixesRepository;

	private Map<String, IssueFixDetails> fixes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	@PostConstruct
	public void init() {
		fixes = getAllFixesDataByVersion("2.0", "AAA");
	}

	public Map<String, IssueFixDetails> getAllFixesDataByVersion(String wcagVersion, String level) {
		Optional<WcagFixesData> fixesData = issueFixesRepository.findByWcagVersionAndLevel(wcagVersion, level);
		return fixesData.map(data -> {
			Map<String, IssueFixDetails> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			caseInsensitiveMap.putAll(data.getData());
			return caseInsensitiveMap;
		}).orElse(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
	}

	public WcagAccessibilityGuidelinesImpl_2_0_AAA(HelperGuidelinesExecutorService helperGuidelinesExecutorService,
			GuidelineResponseBuilderService guidelineResponseBuilderService,
			IGuidelineDataTransformer iGuidelineDataTransformer, WebDriverFactory webDriverFactory) {
		this.helperGuidelinesExecutorService = helperGuidelinesExecutorService;
		this.guidelineResponseBuilderService = guidelineResponseBuilderService;
		this.iGuidelineDataTransformer = iGuidelineDataTransformer;
		this.webDriverFactory = webDriverFactory;
	}

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

	public List<WebElement> getGlobalSeleniumData(String url) {
		WebDriver driver = null;

		//
//		// Create a WebDriver instance using the factory
		driver = WebDriverFactory.getDriver();

		driver.get(url);

		System.out.println("Fetching Selenium data...");

		// Fetch the data from the server using a CSS selector
		return driver.findElements(By.cssSelector(
				"p, h1, h2, h3, h4, h5, h6, span, section, div, a, li, button, input, textarea, audio, select, "
						+ "[tabindex], [role='button'], [role='link'], [role='menuitem'], [data-time-limit], "
						+ "[class*='countdown'], [id*='timer'], [class*='timer'], [role='timer'], meta[http-equiv='refresh']"));

	}

//	public List<WebElement> getWebDriver(String url, WebDriver driver, By locator) {
//
//		try {
//			// Initialize WebDriver using the factory
//			
//
//			// Find and return elements based on the provided locator
//			return driver.findElements(locator);
//
//		} catch (Exception e) {
//			// Log or handle the exception as needed
//			System.err.println("Error occurred: " + e.getMessage());
//			return null;
//		}
//	}

	/**
	 * Checks if prerecorded video content in the document includes a sign language
	 * track. WCAG 1.2.6 requires sign language interpretation for all prerecorded
	 * audio content.
	 *
	 * @param doc    The Jsoup Document object representing the HTML page.
	 * @param issues A list to collect issues found during the check.
	 */

	public GuidelineResponse signLanguagePrerecorded(Document doc) {
		String guideline = "1.2.6 Sign Language (Prerecorded)";

		List<IssueDetails> issueList = new ArrayList<>();

		Elements videos = doc.select("video");
		for (Element video : videos) {

			boolean hasSignLanguage = !video.select("track[kind=signLanguage]").isEmpty();

			if (!hasSignLanguage) {
				String key = "Missing sign language alternative";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, video, video.toString(), key, getFix(key));
			}
		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Return the GuidelineResponse with updated counts
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	public GuidelineResponse extendedAudioDescriptionPrerecorded(Document doc) {
		String guideline = "1.2.7 Extended Audio Description (Prerecorded)";

		List<IssueDetails> issueList = new ArrayList<>();

		Elements videos = doc.select("video");
		for (Element video : videos) {

			boolean hasExtendedAudioDescription = !video.select("track[kind=descriptions]").isEmpty();

			if (!hasExtendedAudioDescription) {
				String key = "Missing extended audio description";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, video, video.toString(), key, getFix(key));
			}
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Return the GuidelineResponse with updated counts
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	public GuidelineResponse mediaAlternativePrerecorded(Document doc) {
		String guideline = "1.2.8 Media Alternative (Prerecorded)";

		List<IssueDetails> issueList = new ArrayList<>();

		Elements videos = doc.select("video");
		for (Element video : videos) {

			boolean hasMediaAlternative = video.select("track[kind=alternative]").size() > 0;

			if (!hasMediaAlternative) {
				String key = "Missing media alternative (text alternative) for prerecorded content";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, video, video.toString(), key, getFix(key));
			}
		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Build and return the GuidelineResponse with the results
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	public GuidelineResponse audioOnlyLive(Document doc) {
		String guideline = "1.2.9 Audio-only (Live)";

		List<IssueDetails> issueList = new ArrayList<>();

		Elements audioElements = doc.select("audio");
		for (Element audio : audioElements) {

			boolean isLive = audio.hasAttr("data-live") && audio.attr("data-live").equalsIgnoreCase("true");

			if (isLive) {

				boolean hasCaptionsIframe = doc.select("iframe[title~=(?i)live\\s*captions]").size() > 0;
				boolean hasTranscriptLink = doc.select("a[href~=(?i)transcript]").size() > 0;

				if (!hasCaptionsIframe && !hasTranscriptLink) {
					status = false;
					String key = "Missing text alternative (live captions or transcript) for live audio-only content";
					iGuidelineDataTransformer.addIssueUsingJsoup(issueList, audio, audio.toString(),
							"Missing text alternative (live captions or transcript) for live audio-only content.",
							getFix(key));
				}
			}
		}
		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		// Build and return the GuidelineResponse with the results
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

//	@Override
//	public GuidelineResponse validateContrastEnhancedLevelAAA(Document doc, String url) {
//		logger.info("validateContrastEnhancedLevelAAA");
//		String guideline = "1.4.6 Contrast (Enhanced)";
//
//		int totalCheckedElements = 0;
//		int successfulElements = 0;
//
//		WebDriver driver = null;
//		List<IssueDetails> issueList = new ArrayList<>();
//
//		try {
//
//			driver = WebDriverFactory.getDriver(url);
//
////			// Fetch elements with potential text content
//			List<WebElement> textElements = driver
//					.findElements(By.cssSelector("p, h1, h2, h3, h4, h5, h6, span, a, li, button, input, textarea"));
//
//			   int totalChecked = textElements.size(); 
//			
//			for (WebElement element : textElements) {
//				totalCheckedElements++;
//				try {
//					String color = element.getCssValue("color");
//					String backgroundColor = element.getCssValue("background-color");
//
//					if (color != null && backgroundColor != null) {
//						double contrastRatio = calculateContrastRatioAAA(color, backgroundColor);
//						boolean isLargeTextAAA = isLargeTextAAA(element);
//
//						double requiredContrastRatio = isLargeTextAAA ? 4.5 : 7.0;
//						if (contrastRatio < requiredContrastRatio) {
//							status = false;
//							iGuidelineDataTransformer
//									.addIssue(
//											issueList, element, element.getText(), "Insufficient contrast ratio: "
//													+ contrastRatio + " (required: " + requiredContrastRatio + ").",
//											level);
//						} else {
//							successfulElements++;
//						}
//					}
//				} catch (Exception e) {
//					logger.error("Error processing element: {}", e.getMessage());
//				}
//			}
//
//		} catch (Exception e) {
//			logger.error("Error initializing WebDriver or processing URL: {}", e.getMessage());
//			status = false;
//		}
//
//
//
//     
//
//        int issueCount = issueList.size();
//		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values
//
//		// Update global counts
//		updateCounts(issueCount, successCount);
//
//		
//		
//		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, true,
//				successCount, totalCheckedElements);
//	}

	@Override
	public GuidelineResponse validateContrastEnhancedLevelAAA(Document doc, String url) {
		logger.info("validateContrastEnhancedLevelAAA");
		String guideline = "1.4.6 Contrast (Enhanced)";
		String level = "AAA"; // WCAG level
		String wcagVersion = "WCAG 2.1";

		int successfulElements = 0;
		boolean status = true; // Ensure it's initialized

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			// Fetch elements with potential text content
			List<WebElement> textElements = driver
					.findElements(By.cssSelector("p, h1, h2, h3, h4, h5, h6, span, a, li, button, input, textarea"));

			for (WebElement element : textElements) {

				try {
					String color = element.getCssValue("color");
					String backgroundColor = element.getCssValue("background-color");

					if (color == null || backgroundColor == null || color.isEmpty() || backgroundColor.isEmpty()) {
						continue;
					}

					double contrastRatio = calculateContrastRatioAAA(color, backgroundColor);
					boolean isLargeTextAAA = isLargeTextAAA(element);

					double requiredContrastRatio = isLargeTextAAA ? 4.5 : 7.0;
					if (contrastRatio < requiredContrastRatio) {
						status = false;
							String key ="Insufficient contrast ratio";
							iGuidelineDataTransformer
									.addIssue(
											issueList, element, element.getText(), "Insufficient contrast ratio: "
													+ contrastRatio + " (required: " + requiredContrastRatio + ").",
											getFix(key));//TODO TITLE--> Insufficient contrast ratio
						} else {
							successfulElements++;
						}
				} catch (Exception e) {
					logger.error("Error processing element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error initializing WebDriver or processing URL: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0);

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);
	}

	private double calculateContrastRatioAAA(String color, String backgroundColor) {
		double luminance1 = getLuminance(color);
		double luminance2 = getLuminance(backgroundColor);
		double lighter = Math.max(luminance1, luminance2);
		double darker = Math.min(luminance1, luminance2);
		return (lighter + 0.05) / (darker + 0.05);
	}

	private double getLuminance(String rgbColor) {
		String[] rgbValues = rgbColor.replace("rgb(", "").replace(")", "").split(",");
		double r = normalizeColorValue(Integer.parseInt(rgbValues[0].trim()));
		double g = normalizeColorValue(Integer.parseInt(rgbValues[1].trim()));
		double b = normalizeColorValue(Integer.parseInt(rgbValues[2].trim()));
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private double normalizeColorValue(int value) {
		double color = value / 255.0;
		return color <= 0.03928 ? color / 12.92 : Math.pow((color + 0.055) / 1.055, 2.4);
	}

	private boolean isLargeTextAAA(WebElement element) {
		String fontSize = element.getCssValue("font-size").replace("px", "");
		String fontWeight = element.getCssValue("font-weight");
		double size = Double.parseDouble(fontSize);
		boolean isBold = Integer.parseInt(fontWeight) >= 700;

		return size >= 18 || (isBold && size >= 14);
	}

	public GuidelineResponse validateLowOrNoBackgroundAudio(Document doc, String url) {

		logger.info("validateLowOrNoBackgroundAudio");
		String guideline = "1.4.7 Low or No Background Audio";

		int totalCheckedElements = 0;
		int successfulElements = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			List<WebElement> audioElements = driver.findElements(By.cssSelector("audio"));

			for (WebElement audioElement : audioElements) {
				totalCheckedElements++;

				try {
					String audioSrc = getResolvedAudioSource(audioElement, url);
					if (audioSrc == null) {
						continue; // Skip processing if source is invalid
					}

					if (!validateAudioCompliance(audioSrc)) {
						status = false;
						String key ="The audio does not meet the requirements of WCAG SC 1.4.7";
						iGuidelineDataTransformer.addIssue(issueList, audioElement, "Non-compliant audio element.",
								"The audio does not meet the requirements of WCAG SC 1.4.7.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing audio element: {}", e.getMessage());
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver operation: {}", e.getMessage());
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private String getResolvedAudioSource(WebElement audioElement, String baseUrl) {
		try {
			String audioSrc = audioElement.getAttribute("src");
			if (audioSrc == null || audioSrc.isEmpty()) {
				logger.warn("Audio element has no source: {}", audioElement);
				return null;
			}
			return resolveUrl(baseUrl, audioSrc);
		} catch (Exception e) {
			logger.error("Error resolving audio source: {}", e.getMessage());
			return null;
		}
	}

	private boolean validateAudioCompliance(String audioSrc) {
		try {
			logger.info("Validating audio compliance for source: {}", audioSrc);

			// Example checks: Add more sophisticated logic as needed
			if (!hasAudioControls(audioSrc)) {
				logger.warn("Audio lacks controls to stop background noise.");
				return false;
			}
			if (containsBackgroundNoise(audioSrc)) {
				logger.warn("Audio contains background noise or ambient sounds.");
				return false;
			}
			if (getAudioDuration(audioSrc) <= 10) {
				logger.warn("Audio duration is too short to meet compliance.");
				return false;
			}

			return true;
		} catch (Exception e) {
			logger.error("Error validating audio compliance: {}", e.getMessage());
			return false;
		}
	}

	private boolean analyzeAudioFile(String audioSrc) {
		try {
			if (!hasAudioControls(audioSrc)) {
				logger.warn("Audio element lacks controls (can't turn off background audio).");
				return false;
			}

			if (containsBackgroundNoise(audioSrc)) {
				logger.warn("Audio element contains background music or ambient sound.");
				return false;
			}

			long duration = getAudioDuration(audioSrc);
			if (duration <= 10) {
				logger.warn("Audio element duration is too short to be compliant.");
				return false;
			}

			return true;
		} catch (Exception e) {
			logger.error("Error processing audio file: {}", e.getMessage());
			return false;
		}
	}

	private boolean hasAudioControls(String audioSrc) {
		return audioSrc.contains("controls");
	}

	private boolean containsBackgroundNoise(String audioSrc) {
		return audioSrc.contains("background music") || audioSrc.contains("ambient sound");
	}

	private long getAudioDuration(String audioSrc) {
		try {
			ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", audioSrc);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			StringBuilder output = new StringBuilder();

//			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//				String line;
//				while ((line = reader.readLine()) != null) {
//					output.append(line).append("\n");
//				}
//			}

			String outputString = output.toString();
			String regex = "Duration: (\\d+):(\\d+):(\\d+\\.\\d+)";
			Pattern pattern = Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(outputString);

			if (matcher.find()) {
				int hours = Integer.parseInt(matcher.group(1));
				int minutes = Integer.parseInt(matcher.group(2));
				double seconds = Double.parseDouble(matcher.group(3));
				return (long) ((hours * 3600) + (minutes * 60) + seconds);
			}
		} catch (IOException e) {
			logger.error("Error getting audio duration: {}", e.getMessage());
		}

		return -1;
	}

	private String resolveUrl(String baseUrl, String relativeUrl) {

		try {
			URI baseUri = new URI(baseUrl);
			URI resolvedUri = baseUri.resolve(relativeUrl);
			return resolvedUri.toURL().toString();
		} catch (URISyntaxException | MalformedURLException e) {
			logger.error("Error resolving URL: {}", e.getMessage());
			return relativeUrl;
		}
	}

	private GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, int successCount, int totalCheckedElements) {

		boolean isCompliant = issueList.isEmpty();
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList,
				isCompliant, successCount, totalCheckedElements);
	}

	public GuidelineResponse validateVisualPresentation(Document doc, String url) {
		logger.info("validateVisualPresentation");
		String guideline = "1.4.8 Visual Presentation";

		int totalCheckedElements = 0;
		int successfulElements = 0;

		WebDriver driver = null;

		List<IssueDetails> issueList = new ArrayList<>();

		try {

			driver = WebDriverFactory.getDriver(url);

			// driver = accessibilityValidator.initDriver(url);

			// Fetch text elements using AccessibilityValidator
//			List<WebElement> textElements = validator.getVisualPresentationElements();
//			totalCheckedElements = textElements.size();

//			List<WebElement> textElements = driver.findElements(By.cssSelector(
//				    "p, h1, h2, h3, h4, h5, h6, span, section, div, a, li, button, input, textarea, " +
//				    "audio, select, [tabindex], [role='button'], [role='link'], [role='menuitem'], " +
//				    "[data-time-limit], [class*='countdown'], [id*='timer'], [class*='timer'], " +
//				    "[role='timer'], meta[http-equiv='refresh']"
//				));
//			

			List<WebElement> textElements = driver.findElements(By.cssSelector("p, h1, h2, h3, h4, h5, h6, span, div"));

			int totalChecked = textElements.size();

			for (WebElement element : textElements) {
				totalCheckedElements++;
				try {
					String color = element.getCssValue("color");
					String backgroundColor = element.getCssValue("background-color");
					if (!validateColors(color, backgroundColor)) {
						status = false;
						String key = "Foreground and background color combination is not user-adjustable";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"Foreground and background color combination is not user-adjustable.", getFix(key));
					}

					String text = element.getText();
					if (text != null && !validateLineWidth(text)) {
						status = false;
						String key = "Line width exceeds 80 characters or 40 CJK glyphs";
						iGuidelineDataTransformer.addIssue(issueList, element, text,
								"Line width exceeds 80 characters or 40 CJK glyphs.", getFix(key));
					}

					String textAlign = element.getCssValue("text-align");
					if ("justify".equalsIgnoreCase(textAlign)) {
						status = false;
						String key = "Text is fully justified, causing readability issues";
						iGuidelineDataTransformer.addIssue(issueList, element, text,
								"Text is fully justified, causing readability issues.", getFix(key));
					}

					if (!validateLineAndParagraphSpacing(element)) {
						status = false;
						String key ="Line spacing or paragraph spacing does not meet Level AAA requirements";
						iGuidelineDataTransformer.addIssue(issueList, element, text,
								"Line spacing or paragraph spacing does not meet Level AAA requirements.", getFix(key));
					}

					if (!validateTextResizing(driver, element)) {
						status = false;
						String key = "Text resizing up to 200% causes horizontal scrolling";
						iGuidelineDataTransformer.addIssue(issueList, element, text,
								"Text resizing up to 200% causes horizontal scrolling.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing text element: {}", e.getMessage());
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage());
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successfulElements, issueCount);
	}

	private boolean validateColors(String color, String backgroundColor) {
		if (color == null || backgroundColor == null) {
			return false;
		}

		try {
			int[] textColor = parseCssColor(color);
			int[] bgColor = parseCssColor(backgroundColor);

			double contrastRatio = calculateContrastRatio(textColor, bgColor);

			return contrastRatio >= 7.0;
		} catch (Exception e) {
			logger.error("Error validating colors: {}", e.getMessage());
			return false;
		}
	}

	private int[] parseCssColor(String color) {
		if (color.startsWith("rgb")) {

			String[] rgbValues = color.replace("rgb(", "").replace("rgba(", "").replace(")", "").split(",");
			return new int[] { Integer.parseInt(rgbValues[0].trim()), Integer.parseInt(rgbValues[1].trim()),
					Integer.parseInt(rgbValues[2].trim()) };
		} else if (color.startsWith("#")) {

			int r = Integer.parseInt(color.substring(1, 3), 16);
			int g = Integer.parseInt(color.substring(3, 5), 16);
			int b = Integer.parseInt(color.substring(5, 7), 16);
			return new int[] { r, g, b };
		}
		throw new IllegalArgumentException("Unsupported color format: " + color);
	}

	private double calculateContrastRatio(int[] color1, int[] color2) {
		// Calculate relative luminance for both colors
		double luminance1 = calculateRelativeLuminance(color1);
		double luminance2 = calculateRelativeLuminance(color2);

		// Ensure luminance1 is always the brighter color
		if (luminance1 < luminance2) {
			double temp = luminance1;
			luminance1 = luminance2;
			luminance2 = temp;
		}

		// Calculate and return the contrast ratio
		return (luminance1 + 0.05) / (luminance2 + 0.05);
	}

	private double calculateRelativeLuminance(int[] color) {
		// Convert RGB to linear values
		double r = adjustColor(color[0]);
		double g = adjustColor(color[1]);
		double b = adjustColor(color[2]);

		// Calculate the relative luminance using the formula
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private double adjustColor(int colorValue) {
		// Normalize the color value to a 0-1 range
		double value = colorValue / 255.0;

		// Apply the gamma correction to the color value
		if (value <= 0.03928) {
			return value / 12.92;
		} else {
			return Math.pow((value + 0.055) / 1.055, 2.4);
		}
	}

	private boolean validateLineWidth(String text) {
		if (text == null)
			return true;
		int maxLineWidth = 80;

		for (String line : text.split("\n")) {
			if (line.length() > maxLineWidth) {
				return false;
			}
		}
		return true;
	}

	private boolean validateLineAndParagraphSpacing(WebElement element) {
		String lineHeight = element.getCssValue("line-height");
		String marginBottom = element.getCssValue("margin-bottom");

		double lineHeightValue = parseCssUnit(lineHeight);
		double marginBottomValue = parseCssUnit(marginBottom);
		return lineHeightValue >= 1.5 && marginBottomValue >= 2.5 * lineHeightValue;
	}

	private static double parseCssUnit(String cssValue) {

		if (cssValue == null || cssValue.trim().isEmpty()) {
			logger.error("CSS value is null or empty");
			return 0.0;
		}

		try {
			if ("normal".equalsIgnoreCase(cssValue)) {
				return 400.0;
			}

			Matcher matcher = UNIT_PATTERN.matcher(cssValue.trim());

			if (matcher.matches()) {

				double value = Double.parseDouble(matcher.group(1));

				String unit = matcher.group(2);
				if (unit == null || unit.isEmpty()) {

					return value;
				}

				switch (unit.toLowerCase()) {
				case "px":
				case "em":
				case "rem":
				case "%":

					return value;
				default:
					logger.error("Unsupported CSS unit: {}", unit);
					return 0.0;
				}
			} else {

				logger.error("Unable to parse CSS unit: {}", cssValue);
				return 0.0;
			}

		} catch (Exception e) {

			logger.error("Error parsing CSS value: {}", cssValue, e);
			return 0.0;
		}
	}

	private boolean validateTextResizing(WebDriver driver, WebElement element) {
		try {

			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("document.body.style.zoom='200%';");
			Dimension viewportSize = driver.manage().window().getSize();
			int scrollWidth = Integer
					.parseInt(js.executeScript("return document.documentElement.scrollWidth;").toString());
			js.executeScript("document.body.style.zoom='100%';");
			return scrollWidth <= viewportSize.getWidth();
		} catch (Exception e) {
			logger.error("Error resizing text: {}", e.getMessage());
			return false;
		}
	}

	@Override
	public GuidelineResponse validateKeyboardAccessibilityNoException(Document doc, String url) {
		logger.info("validateKeyboardAccessibilityNoException");
		String guideline = "2.1.3 Keyboard (No Exception) (Level AAA)";

		int totalCheckedElements = 0;
		int successfulElements = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			// Identify all actionable elements
			List<WebElement> actionableElements = driver.findElements(By.cssSelector(
					"a, button, input, textarea, select, [tabindex], [role='button'], [role='link'], [role='menuitem']"));

			logger.info("Found {} actionable elements to verify keyboard accessibility.", actionableElements.size());

			for (WebElement element : actionableElements) {
				totalCheckedElements++;

				try {
					if (!element.isDisplayed() || !element.isEnabled()) {
						logger.debug("Skipping non-visible or disabled element: {}", element.getTagName());
						continue;
					}

					// Check if the element is operable using the keyboard
					boolean isKeyboardAccessible = isOperableByKeyboard(driver, element);

					if (!isKeyboardAccessible) {
						status = false;
						String key = "The element is not operable using the keyboard interface";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"The element is not operable using the keyboard interface.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing element {}: {}");
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage());
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private boolean isOperableByKeyboard(WebDriver driver, WebElement element) {
		try {
			// Save initial focus
			// List<WebElement> initialFocusedElement =
			// accessibilityValidator.getGlobalSeleniumData();

			WebElement initialFocusedElement = driver.switchTo().activeElement();

			// Attempt to focus the element
			element.sendKeys(Keys.TAB);
			WebElement focusedElement = driver.switchTo().activeElement();

			// Check if the element received focus
			boolean focusReceived = element.equals(focusedElement);

			if (!focusReceived) {
				return false;
			}

			// Test activation (if applicable)
			String tagName = element.getTagName();
			if ("button".equalsIgnoreCase(tagName) || "a".equalsIgnoreCase(tagName)) {
				element.sendKeys(Keys.ENTER);

			}

			return true;
		} catch (Exception e) {
			logger.error("Error verifying keyboard operability: {}", e.getMessage(), e);
			return false;
		}
	}

//	private boolean isOperableByKeyboard(WebDriver driver, WebElement element) {
//		try {
//			// Save the currently focused element
//			WebElement initialFocusedElement = driver.switchTo().activeElement();
//
//			// Use JavaScript to focus the element
//			((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element);
//
//			// Verify if the element received focus
//			WebElement focusedElement = driver.switchTo().activeElement();
//			boolean focusReceived = element.equals(focusedElement);
//
//			if (!focusReceived) {
//				logger.warn("Element did not receive focus: {}", element);
//				return false;
//			}
//
//			// Simulate activation (if applicable)
//			String tagName = element.getTagName().toLowerCase();
//			if ("button".equals(tagName) || "a".equals(tagName)) {
//				try {
//					element.sendKeys(Keys.ENTER);
//					logger.info("Activated element using ENTER key: {}", element);
//				} catch (Exception e) {
//					logger.error("Error activating element: {}", e.getMessage(), e);
//					return false;
//				}
//			}
//
//			// Restore initial focus
//			((JavascriptExecutor) driver).executeScript("arguments[0].focus();", initialFocusedElement);
//
//			return true;
//		} catch (Exception e) {
//			logger.error("Error verifying keyboard operability: {}", e.getMessage(), e);
//			return false;
//		}
//	}

	public GuidelineResponse validateNoTiming(Document doc, String url) {
		String guideline = "2.2.3 No Timing (Level AAA)";

		int successfulElements = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			// List<WebElement> timedElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			// Identify elements that might have timing-related constraints
			List<WebElement> timedElements = driver.findElements(By.cssSelector(
					"[data-time-limit], [class*='countdown'], [id*='timer'], [class*='timer'], [role='timer']"));

			logger.info("Found {} potential elements with timing-related constraints.", timedElements.size());

			for (WebElement element : timedElements) {

				try {
					if (!element.isDisplayed()) {
						logger.debug("Skipping non-visible element: {}", element.getTagName());
						continue;
					}

					// Check if the element has mechanisms to disable timing
					boolean hasTimingControl = hasDisableTimingControls(driver, element);

					if (!hasTimingControl) {
						status = false;
						String key ="This element enforces timing-related constraints without providing mechanisms to disable or extend the timing";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getText(),
								"This element enforces timing-related constraints without providing mechanisms to disable or extend the timing.",
								getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing element {}: {}", element.getTagName(), e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private boolean hasDisableTimingControls(WebDriver driver, WebElement element) {
		try {
			// Locate any controls to pause, extend, or disable timing-related constraints
			List<WebElement> controls = driver.findElements(By.cssSelector(
					"[aria-controls='" + element.getAttribute("id") + "'], [data-extend-timer], [data-disable-timer]"));

			logger.debug("Found {} timing control mechanisms for element: {}", controls.size(), element.getTagName());
			return !controls.isEmpty();
		} catch (Exception e) {
			logger.error("Error checking timing controls for element {}: {}", element.getTagName(), e.getMessage(), e);
			return false;
		}
	}

	public GuidelineResponse validateInterruptions(Document doc, String url) {
		String guideline = "2.2.4 Interruptions (Level AAA)";

		int successfulElements = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// List<WebElement> metaElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			// Identify meta elements with http-equiv="refresh"
			List<WebElement> metaElements = driver.findElements(By.cssSelector("meta[http-equiv='refresh']"));

			logger.info("Found {} meta refresh elements.", metaElements.size());

			for (WebElement element : metaElements) {

				try {
					String contentAttribute = element.getAttribute("content");
					logger.debug("Processing meta element with content: {}", contentAttribute);

					if (contentAttribute == null || contentAttribute.isEmpty()) {
						logger.warn("Meta element has no content attribute.");
						continue;
					}

					String[] parts = contentAttribute.split(";");
					if (parts.length > 0) {
						int refreshInterval = Integer.parseInt(parts[0].trim());

						if (refreshInterval < 1 || refreshInterval > 72000) {
							status = false;
							String key = "Meta refresh interval is set to an invalid value or does not allow sufficient time for users to interact";
							iGuidelineDataTransformer.addIssue(issueList, element, element.getAttribute("outerHTML"),
									"Meta refresh interval is set to an invalid value or does not allow sufficient time for users to interact.",
									getFix(key));
						}
					}

					// Check for mechanisms to disable or adjust the refresh timing
					boolean hasDisableOption = hasRefreshDisableMechanisms(driver);
					if (!hasDisableOption) {
						status = false;
						String key = "No mechanism provided to disable or adjust meta refresh timing";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getAttribute("outerHTML"),
								"No mechanism provided to disable or adjust meta refresh timing.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing meta element: {}", e.getMessage(), e);
				}
			}

			// List<WebElement> alertElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> alertElements = driver
					.findElements(By.cssSelector("[role='alert'], [class*='notification'], [id*='popup']"));
			logger.info("Found {} alert/notification elements.", alertElements.size());

			for (WebElement alert : alertElements) {

				try {
					if (!alert.isDisplayed()) {
						logger.debug("Skipping non-visible alert/notification element: {}", alert.getTagName());
						continue;
					}

					boolean isEssential = isEmergencyAlert(alert);
					if (!isEssential) {
						status = false;
						String key ="Non-essential interruptions (e.g., notifications or alerts) cannot be postponed or disabled";
						iGuidelineDataTransformer.addIssue(issueList, alert, alert.getText(),
								"Non-essential interruptions (e.g., notifications or alerts) cannot be postponed or disabled.",
								getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing alert/notification element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private boolean hasRefreshDisableMechanisms(WebDriver driver) {
		try {

			// List<WebElement> controls= accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> controls = driver.findElements(
					By.cssSelector("[data-disable-refresh], [class*='pause-refresh'], [id*='disable-refresh']"));

			logger.debug("Found {} refresh disable mechanisms.", controls.size());
			return !controls.isEmpty();
		} catch (Exception e) {
			logger.error("Error checking for refresh disable mechanisms: {}", e.getMessage(), e);
			return false;
		}
	}

	private boolean isEmergencyAlert(WebElement element) {
		try {
			String alertText = element.getText().toLowerCase();
			return alertText.contains("emergency") || alertText.contains("danger") || alertText.contains("safety")
					|| alertText.contains("health") || alertText.contains("data loss");
		} catch (Exception e) {
			logger.error("Error determining if alert is an emergency: {}", e.getMessage(), e);
			return false;
		}
	}

	@Override
	public GuidelineResponse validateReauthentication(Document doc, String url) {
		String guideline = "2.2.5 Re-authenticating (Level AAA)";

		int issueCount = 0;
		int successCount = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			logger.info("Simulating session expiration and verifying re-authentication handling...");

			try {
				triggerSessionTimeout(driver);

				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // Wait for elements to load

				WebElement form = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("checkoutForm")));
				fillOutForm(driver, form);
				form.submit();

				WebElement reauthPrompt = wait
						.until(ExpectedConditions.visibilityOfElementLocated(By.id("reauthenticatePrompt")));
				if (reauthPrompt.isDisplayed()) {
					logger.info("Re-authentication prompt displayed.");

					reauthenticateUser(driver);

					WebElement restoredForm = wait
							.until(ExpectedConditions.visibilityOfElementLocated(By.id("checkoutForm")));
					if (verifyFormDataRestored(restoredForm)) {
						logger.info("User data successfully preserved after re-authentication.");
						issueCount++;

					} else {
						successCount++;
						status = false;
						String key = "User data was not preserved after re-authentication";
						iGuidelineDataTransformer.addIssue(issueList, restoredForm, "",
								"User data was not preserved after re-authentication.", getFix(key));
					}
				} else {
					status = false;
					String key ="No re-authentication prompt provided after session timeout";
					iGuidelineDataTransformer.addIssue(issueList, form, "",
							"No re-authentication prompt provided after session timeout.", getFix(key));
				}
			} catch (NoSuchElementException e) {
				status = false;
				logger.error("Error finding necessary elements: {}", e.getMessage(), e);
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, issueCount, successCount);
	}

	/**
	 * Helper method to simulate session timeout.
	 */
	private void triggerSessionTimeout(WebDriver driver) {

		driver.manage().deleteAllCookies();
		logger.info("Session timeout simulated by clearing cookies.");
	}

	/**
	 * Helper method to fill out a form for testing.
	 */
	private void fillOutForm(WebDriver driver, WebElement form) {

		WebElement inputField = form.findElement(By.name("creditCardNumber"));
		inputField.sendKeys("4111111111111111");
	}

	/**
	 * Helper method to perform re-authentication.
	 */
	private void reauthenticateUser(WebDriver driver) {
		WebElement usernameField = driver.findElement(By.name("username"));
		WebElement passwordField = driver.findElement(By.name("password"));
		WebElement loginButton = driver.findElement(By.name("login"));

		usernameField.sendKeys("testUser");
		passwordField.sendKeys("testPassword");
		loginButton.click();
		logger.info("Re-authentication completed.");
	}

	/**
	 * Helper method to verify if form data is restored after re-authentication.
	 */
	private boolean verifyFormDataRestored(WebElement form) {
		WebElement inputField = form.findElement(By.name("creditCardNumber"));
		String restoredData = inputField.getAttribute("value");
		return "4111111111111111".equals(restoredData);
	}

//	@Override
//	public GuidelineResponse validateFlashingContentAAA(Document doc, String url) {
//		String guideline = "2.3.2 Three Flashes (Level AAA)";
//
//		int issuecount = 0;
//		int successCount = 0;
//
//		WebDriver driver = null;
//		List<IssueDetails> issueList = new ArrayList<>();
//
//		try {
//			// Create WebDriver instance
//			driver = webDriverFactory.createWebDriver();
//			driver.get(url);
//			logger.info("Navigated to URL: {}", url);
//
//			
//
//			try {
//
//				List<WebElement> flashingElements = accessibilityValidator.getGlobalSeleniumData(url);
//
//				// List<WebElement> flashingElements = detectFlashingContent(driver);
//
//				if (!flashingElements.isEmpty()) {
//					
//					issuecount++;
//					status = false;
//					for (WebElement element : flashingElements) {
//						String elementDetails = "Tag: %s, ID: %s, Class: %s".formatted(element.getTagName(),
//								element.getAttribute("id"), element.getAttribute("class"));
//						logger.warn("Flashing content detected: {}", elementDetails);
//						iGuidelineDataTransformer.addIssue(issueList, element, "",
//								"Flashing content exceeds threshold of three flashes per second.", level);
//					}
//				} else {
//					successCount++;
//					logger.info("No flashing content detected that violates the guideline.");
//				}
//
//			} catch (JavascriptException e) {
//				status = false;
//				logger.error("JavaScript execution failed while checking flashing content: {}", e.getMessage(), e);
//			} catch (Exception e) {
//				status = false;
//				logger.error("Unexpected error during flashing content check: {}", e.getMessage(), e);
//			}
//
//		} catch (Exception e) {
//			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
//			status = false;
//		} 
//
//		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount,
//				issuecount);
//	}

	@Override
	public GuidelineResponse validateFlashingContentAAA(Document doc, String url) {
		String guideline = "2.3.2 Three Flashes (Level AAA)";
		int issueCount = 0;
		int successCount = 0;
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			try {
				// Detect flashing elements using JavaScript
				List<WebElement> flashingElements = detectFlashingContent(driver);

				if (!flashingElements.isEmpty()) {
					for (WebElement element : flashingElements) {
						String elementDetails = String.format("Tag: %s, ID: %s, Class: %s", element.getTagName(),
								element.getAttribute("id"), element.getAttribute("class"));
						logger.warn("Flashing content detected: {}", elementDetails);

						// Adding issue details
						String key ="Flashing content exceeds threshold of three flashes per second";
						iGuidelineDataTransformer.addIssue(issueList, element, "",
								"Flashing content exceeds threshold of three flashes per second.", getFix(key));

						issueCount++; // Increment issue count
					}
				} else {
					successCount++; // No flashing content found
					logger.info("No flashing content detected that violates the guideline.");
				}
			} catch (JavascriptException e) {
				logger.error("JavaScript execution failed while checking flashing content: {}", e.getMessage(), e);
			} catch (Exception e) {
				logger.error("Unexpected error during flashing content check: {}", e.getMessage(), e);
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		// Return the result, including counts
		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private List<WebElement> detectFlashingContent(WebDriver driver) {
		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

		// JavaScript snippet to detect flashing elements
		String flashDetectionScript = """
				    return Array.from(document.querySelectorAll('*')).filter(element => {
				        const style = window.getComputedStyle(element);
				        const animationDuration = parseFloat(style.getPropertyValue('animation-duration')) || 0;
				        const iterationCount = style.getPropertyValue('animation-iteration-count');
				        return animationDuration < 1 && (iterationCount === 'infinite' || parseFloat(iterationCount) > 3);
				    });
				""";

		@SuppressWarnings("unchecked")
		List<WebElement> flashingElements = (List<WebElement>) jsExecutor.executeScript(flashDetectionScript);

		logger.info("Flashing content detection completed. Found {} elements.", flashingElements.size());
		return flashingElements;
	}

	public GuidelineResponse validateLocation(Document doc, String url) {
		String guideline = "2.4.8 Location";

		int successfulElements = 0;
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true;

		try {
			driver = WebDriverFactory.getDriver(url);
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Locate elements for breadcrumbs, sitemaps, and navigation bars
			List<WebElement> breadcrumbElements = driver.findElements(By.cssSelector(".breadcrumb, .breadcrumbs, nav"));
			List<WebElement> siteMapLinks = driver.findElements(By.cssSelector("a[href*='sitemap']"));
			List<WebElement> navBars = driver.findElements(By.cssSelector("nav"));

			// Check breadcrumbs
			for (WebElement breadcrumb : breadcrumbElements) {

				try {
					if (breadcrumb.getText().isEmpty()) {
						status = false;
						String key ="Breadcrumb trail or navigation indicator is missing";
						iGuidelineDataTransformer.addIssue(issueList, breadcrumb,
								"Breadcrumb trail is empty or not present.",
								"Breadcrumb trail or navigation indicator is missing.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing breadcrumb element: {}", e.getMessage(), e);
				}
			}

			// Check site map links
			for (WebElement sitemap : siteMapLinks) {

				try {
					if (!sitemap.isDisplayed()) {
						status = false;
						String key = "Sitemap link is not visible or accessible";
						iGuidelineDataTransformer.addIssue(issueList, sitemap, sitemap.getText(),
								"Sitemap link is not visible or accessible.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing sitemap element: {}", e.getMessage(), e);
				}
			}

			// Check navigation bars
			for (WebElement nav : navBars) {

				try {
					String navText = nav.getText();
					if (navText == null || navText.isEmpty()) {
						status = false;
						String key ="Navigation bar is missing or does not show the current location";
						iGuidelineDataTransformer.addIssue(issueList, nav,
								"Navigation bar does not indicate the current location.",
								"Navigation bar is missing or does not show the current location.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing navigation bar element: {}", e.getMessage(), e);
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	public GuidelineResponse validateLinkPurpose(Document doc, String url) {
		String guideline = "2.4.9 Link Purpose (Link Only)";

		int successfulLinks = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Find all links (<a> tags) on the page

			// List<WebElement> links = accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> links = driver.findElements(By.tagName("a"));

			for (WebElement link : links) {

				try {
					// Get link text and href
					String linkText = link.getText().trim();
					String linkHref = link.getAttribute("href");

					// Check if the link text is descriptive and non-empty
					if (linkText.isEmpty()) {
						status = false;//TODO TITLE -->Link text is empty or non-descriptive.
						String key = "Link text is empty or non-descriptive";
						iGuidelineDataTransformer.addIssueUsingJsoup(issueList, doc, linkHref, url, getFix(key));
					} else if (isNonDescriptiveText(linkText)) {
						status = false;
						iGuidelineDataTransformer.addIssue(issueList, link, linkHref, url, guideline);
					} else {
						// If the link text is sufficient
						successfulLinks++;
					}

					// Check if aria-label or aria-labelledby is provided for links with
					// insufficient text
					String ariaLabel = link.getAttribute("aria-label");
					String ariaLabelledBy = link.getAttribute("aria-labelledby");

					if (linkText.isEmpty() && (ariaLabel == null || ariaLabel.isEmpty())
							&& (ariaLabelledBy == null || ariaLabelledBy.isEmpty())) {
						status = false;//TODO TITLE -->Link lacks an accessible name (aria-label or aria-labelledby).
						String key = "Link lacks an accessible name (aria-label or aria-labelledby)";
						iGuidelineDataTransformer.addIssue(issueList, link, ariaLabelledBy, url, getFix(key));
					}

				} catch (Exception e) {
					logger.error("Error processing link: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulLinks, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	/**
	 * Helper function to identify non-descriptive link text.
	 */
	private boolean isNonDescriptiveText(String linkText) {
		// Define a list of common non-descriptive phrases
		List<String> nonDescriptiveTexts = Arrays.asList("click here", "read more", "learn more", "here", "more");
		return nonDescriptiveTexts.stream().anyMatch(linkText::equalsIgnoreCase);
	}

	public GuidelineResponse validateSectionHeadings(Document doc, String url) {
		String guideline = "2.4.10 Section Headings";

		int successfulElements = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Find all sections or div elements representing content sections

			List<WebElement> sectionElements = driver.findElements(By.cssSelector("section, div"));

			for (WebElement section : sectionElements) {

				try {
					String sectionText = section.getText();
					boolean hasHeading = hasValidHeading(section);

					if (!hasHeading) {
						status = false;
						String key = "Section is missing a heading or the heading is not descriptive";
						iGuidelineDataTransformer.addIssue(issueList, section, sectionText,
								"Section is missing a heading or the heading is not descriptive.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing section element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulElements, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private boolean hasValidHeading(WebElement section) {
		List<WebElement> headingElements = section.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));
		for (WebElement heading : headingElements) {
			if (!heading.getText().isEmpty()) {
				return true; // Valid heading exists
			}
		}
		return false; // No valid heading found
	}

	public GuidelineResponse validateUnusualWords(Document doc, String url) {
		String guideline = "3.1.3 Unusual Words";

		int successfulWords = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			// Fetch all text content in the document

			// List<WebElement> textElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> textElements = driver.findElements(By.xpath("//*[not(self::script or self::style)]"));

			for (WebElement element : textElements) {
				String textContent = element.getText();
				if (textContent != null && !textContent.isEmpty()) {
					// Extract words from text content
					List<String> words = extractWords(textContent);
					for (String word : words) {

						if (isUnusualWord(word)) {
							if (!hasDefinition(word, doc)) {
								status = false;//TODO TITLE -->Unusual word found without a provided definition or link to a glossary
								String key = "Unusual word found without a provided definition or link to a glossary";
								iGuidelineDataTransformer.addIssue(issueList, element, word,
										"Unusual word found without a provided definition or link to a glossary: "
												+ word,
										getFix(key));
							} else {
								successfulWords++;
							}
						} else {
							successfulWords++;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulWords, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	/**
	 * Extracts individual words from text content for processing.
	 *
	 * @param text The input text content.
	 * @return A list of words.
	 */
	private List<String> extractWords(String text) {
		return Arrays.asList(text.split("\\s+"));
	}

	/**
	 * Determines if a word is considered "unusual" or technical jargon.
	 *
	 * @param word The word to check.
	 * @return True if the word is unusual; false otherwise.
	 */
	private boolean isUnusualWord(String word) {
		// Define or load a list of common words to filter against
		List<String> commonWords = Arrays.asList("the", "and", "a", "in", "on", "of", "to", "with", "is", "it", "this");
		return !commonWords.contains(word.toLowerCase()) && !isStandardWord(word);
	}

	/**
	 * Determines if a word is standard (exists in a predefined dictionary or
	 * glossary).
	 *
	 * @param word The word to check.
	 * @return True if the word is standard; false otherwise.
	 */
	private boolean isStandardWord(String word) {
		// Add logic to check against a dictionary or API for standard words
		return false; // Placeholder logic
	}

	/**
	 * Checks if a word has a definition or is linked to a glossary.
	 *
	 * @param word The word to check.
	 * @param doc  The document to search in.
	 * @return True if a definition exists; false otherwise.
	 */
	private boolean hasDefinition(String word, Document doc) {
		// Search for a definition linked or provided in the document
		Elements definitions = doc.select("dfn, a[href*='glossary'], dl > dt");
		for (Element definition : definitions) {
			if (definition.text().equalsIgnoreCase(word)) {
				return true;
			}
		}
		return false;
	}

	public GuidelineResponse validateAbbreviations(Document doc, String url) {
		String guideline = "3.1.4 Abbreviations";

		int successfulAbbreviations = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			// Fetch all elements containing abbreviations

			// List<WebElement> abbreviationElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> abbreviationElements = driver.findElements(By.xpath("//abbr | //acronym"));

			for (WebElement abbrElement : abbreviationElements) {

				String abbreviation = abbrElement.getText();
				String expansion = abbrElement.getAttribute("title");

				if (abbreviation == null || abbreviation.isEmpty()) {
					String key ="Empty abbreviation tag found";
					iGuidelineDataTransformer.addIssue(issueList, abbrElement, abbreviation,
							"Empty abbreviation tag found.", getFix(key));
					status = false;
				} else if (expansion == null || expansion.isEmpty()) {
					String key ="Abbreviation is missing an expanded form via the 'title' attribute";
					iGuidelineDataTransformer.addIssue(issueList, abbrElement, abbreviation, "Abbreviation '"
							+ abbreviation + "' is missing an expanded form via the 'title' attribute.", getFix(key));
					status = false;//TODO TITLE -->Abbreviation is missing an expanded form via the 'title' attribute.
				} else {
					successfulAbbreviations++;
				}
			}

			// Check for abbreviations in text without <abbr> or <acronym>

			// List<WebElement> textElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> textElements = driver.findElements(By.xpath("//*[not(self::script or self::style)]"));
			for (WebElement element : textElements) {
				String textContent = element.getText();
				if (textContent != null && !textContent.isEmpty()) {
					List<String> abbreviations = extractPotentialAbbreviations(textContent);
					for (String potentialAbbr : abbreviations) {

						if (!isDefinedAbbreviation(potentialAbbr, doc)) {
							String key ="Potential abbreviation found without a corresponding definition or 'abbr' tag";
							iGuidelineDataTransformer
									.addIssue(issueList, element, potentialAbbr,
											"Potential abbreviation '" + potentialAbbr
													+ "' found without a corresponding definition or 'abbr' tag.",
											getFix(key));//TODO TITLE -->Potential abbreviation found without a corresponding definition or 'abbr' tag.
							status = false;
						} else {
							successfulAbbreviations++;
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulAbbreviations, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	/**
	 * Extracts potential abbreviations from text based on pattern matching.
	 *
	 * @param text The input text content.
	 * @return A list of potential abbreviations.
	 */
	private List<String> extractPotentialAbbreviations(String text) {
		// Match patterns for potential abbreviations (e.g., capitalized acronyms or
		// initialisms)
		Pattern abbreviationPattern = Pattern.compile("\\b[A-Z]{2,}\\b");
		Matcher matcher = abbreviationPattern.matcher(text);
		List<String> abbreviations = new ArrayList<>();
		while (matcher.find()) {
			abbreviations.add(matcher.group());
		}
		return abbreviations;
	}

	/**
	 * Checks if a potential abbreviation is already defined in the document.
	 *
	 * @param abbreviation The abbreviation to check.
	 * @param doc          The document to search in.
	 * @return True if the abbreviation is defined; false otherwise.
	 */
	private boolean isDefinedAbbreviation(String abbreviation, Document doc) {
		Elements abbrElements = doc.select("abbr[title], acronym[title]");
		for (Element abbrElement : abbrElements) {
			if (abbrElement.text().equals(abbreviation)) {
				return true;
			}
		}

		// Optionally, check a predefined glossary for the abbreviation
		return isDefinedInGlossary(abbreviation);
	}

	/**
	 * Checks a predefined glossary for the expanded form of an abbreviation.
	 *
	 * @param abbreviation The abbreviation to check.
	 * @return True if the abbreviation is in the glossary; false otherwise.
	 */
	private boolean isDefinedInGlossary(String abbreviation) {
		// Placeholder for external glossary integration or API call
		// Replace with actual logic to check glossary
		return false;
	}

	public GuidelineResponse validateReadingLevel(Document doc, String url) {
		String guideline = "3.1.5 Reading Level";

		int successfulSections = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			// Fetch text content excluding scripts, styles, and hidden elements

			// List<WebElement> textElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> textElements = driver.findElements(By.xpath(
					"//*[not(self::script or self::style or self::noscript) and string-length(normalize-space()) > 0 and not(ancestor::*[@aria-hidden='true'])]"));

			for (WebElement element : textElements) {
				String textContent = element.getText().trim();

				if (!textContent.isEmpty()) {

					// Measure readability score
					ReadabilityScore readabilityScore = analyzeReadability(textContent);

					logger.info("Analyzed text: {} | Grade Level: {}", textContent, readabilityScore.getGradeLevel());

					// Check if the readability is below the threshold for lower secondary education
					// (Grade 9 or equivalent)
					if (readabilityScore.getGradeLevel() > 9) {
						String key = "Text content requires a reading level higher than lower secondary education (Grade 9)";
						iGuidelineDataTransformer.addIssue(issueList, element, textContent,
								"Text content requires a reading level higher than lower secondary education (Grade 9).",
								getFix(key));
						status = false;
					} else {
						successfulSections++;
					}
				}
			}

			// Check for supplemental content
			if (!checkForSupplementalContent(doc)) {
				String key = "Page lacks supplemental content for complex text requiring advanced reading ability";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, doc,
						"Page lacks supplemental content for complex text requiring advanced reading ability.", level,
						wcagVersion);
				status = false;
			}

		} catch (WebDriverException e) {
			logger.error("WebDriver encountered an issue: {}", e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected error during validation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulSections, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	/**
	 * Analyzes the readability of a given text using Flesch-Kincaid Grade Level.
	 *
	 * @param text The input text.
	 * @return A ReadabilityScore object containing grade level and other metrics.
	 */
	private ReadabilityScore analyzeReadability(String text) {
		int sentenceCount = countSentences(text);
		int wordCount = countWords(text);
		int syllableCount = countSyllablesInText(text);

		if (sentenceCount == 0 || wordCount == 0) {
			return new ReadabilityScore(0);
		}

		// Calculate Flesch-Kincaid Grade Level
		double fkGradeLevel = 0.39 * (wordCount / (double) sentenceCount) + 11.8 * (syllableCount / (double) wordCount)
				- 15.59;
		return new ReadabilityScore(fkGradeLevel);
	}

	/**
	 * Counts the number of sentences in a text.
	 *
	 * @param text The input text.
	 * @return The number of sentences.
	 */
	private int countSentences(String text) {
		return text.split("[.!?]").length;
	}

	/**
	 * Counts the number of words in a text.
	 *
	 * @param text The input text.
	 * @return The number of words.
	 */
	private int countWords(String text) {
		return text.split("\\s+").length;
	}

	/**
	 * Counts the total syllables in a text.
	 *
	 * @param text The input text.
	 * @return The total number of syllables.
	 */
	private int countSyllablesInText(String text) {
		int totalSyllables = 0;
		for (String word : text.split("\\s+")) {
			totalSyllables += countSyllables(word);
		}
		return totalSyllables;
	}

	/**
	 * Counts syllables in a word using improved heuristics.
	 *
	 * @param word The input word.
	 * @return The number of syllables.
	 */
	private int countSyllables(String word) {
		String lowerCaseWord = word.toLowerCase();
		int syllables = lowerCaseWord.replaceAll("e$", "") // Remove trailing 'e'
				.replaceAll("[aeiouy]{2,}", "a") // Replace consecutive vowels with single 'a'
				.replaceAll("[^aeiouy]", "") // Remove non-vowels
				.length();
		return Math.max(1, syllables);
	}

	/**
	 * Checks for the presence of supplemental content for complex text.
	 *
	 * @param doc The document to analyze.
	 * @return True if supplemental content is present; false otherwise.
	 */
	private boolean checkForSupplementalContent(Document doc) {
		// Check for plain language summaries, audio versions, visual illustrations, or
		// ARIA roles
		Elements plainLanguageSections = doc.select(".plain-language-summary, .easy-to-read");
		Elements audioSections = doc.select("audio, .audio-version");
		Elements visualIllustrations = doc.select("img, .illustration");
		Elements ariaRoles = doc.select("[role='doc-summary'], [role='figure']");

		return !plainLanguageSections.isEmpty() || !audioSections.isEmpty() || !visualIllustrations.isEmpty()
				|| !ariaRoles.isEmpty();
	}

	public GuidelineResponse validatePronunciation(Document doc, String url) {
		String guideline = "3.1.6 Pronunciation";

		int successfulSections = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			// List<WebElement> textElements =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> textElements = driver.findElements(By.xpath(
					"//*[not(self::script or self::style or self::noscript) and string-length(normalize-space()) > 0 and not(ancestor::*[@aria-hidden='true'])]"));

			for (WebElement element : textElements) {
				String textContent = element.getText().trim();

				if (!textContent.isEmpty()) {

					List<WebElement> rubyElements = element.findElements(By.xpath(".//ruby"));
					List<WebElement> audioLinks = element
							.findElements(By.xpath(".//a[contains(@href, '.mp3') or contains(@href, '.wav')]"));

					if (rubyElements.isEmpty() && audioLinks.isEmpty()) {
						String key = "Ambiguous words without pronunciation mechanisms detected";
						iGuidelineDataTransformer.addIssue(issueList, element, textContent,
								"Ambiguous words without pronunciation mechanisms detected.", getFix(key));
						status = false;
					} else {
						successfulSections++;
					}
				}
			}

			if (!checkForGlossaryOrPronunciationSupport(doc)) {
				String key = "No glossary or supplemental pronunciation support found for ambiguous words";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, doc,wcagVersion,//FIXME Snippet missing
						"No glossary or supplemental pronunciation support found for ambiguous words.", getFix(key));
				status = false;
			}

		} catch (WebDriverException e) {
			logger.error("WebDriver encountered an issue: {}", e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Unexpected error during validation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulSections, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private boolean checkForGlossaryOrPronunciationSupport(Document doc) {

		Elements glossaryElements = doc.select("a[href*='glossary']");
		return !glossaryElements.isEmpty();

	}

//	public GuidelineResponse validateChangeOnRequest(Document doc, String url) {
//		String guideline = "3.2.5 Change on Request";
//
//		int issueCount = 0;
//		int successCount = 0;
//
//		WebDriver driver = null;
//		List<IssueDetails> issueList = new ArrayList<>();
//
//		try {
//			driver = webDriverFactory.createWebDriver();
//			driver.get(url);
//			logger.info("Navigated to URL: {}", url);
//
//			List<WebElement> metaRefreshElements = accessibilityValidator.getGlobalSeleniumData(url);
//
//			// List<WebElement> metaRefreshElements =
//			// driver.findElements(By.xpath("//meta[@http-equiv='refresh']"));
//			if (!metaRefreshElements.isEmpty()) {
//				issueCount++;
//				for (WebElement meta : metaRefreshElements) {
//					String content = meta.getAttribute("content");
//					if (content != null && !content.trim().isEmpty()) {
//						iGuidelineDataTransformer.addIssue(issueList, meta,
//								"Page contains meta refresh that may cause automatic redirection.", level, wcagVersion);
//						status = false;
//					}
//				}
//			}
//
//			List<WebElement> linksOpeningNewWindows = driver.findElements(By.xpath("//a[@target='_blank']"));
//			if (!linksOpeningNewWindows.isEmpty()) {
//				issueCount++;
//				for (WebElement link : linksOpeningNewWindows) {
//					String linkText = link.getText();
//					if (linkText != null && !linkText.trim().isEmpty()) {
//						iGuidelineDataTransformer.addIssue(issueList, link,
//								"Link opens a new window automatically. Ensure it is user-initiated.", level,
//								wcagVersion, linkText);
//						status = false;
//					}
//				}
//			}
//
//			if (!checkForMechanismToTurnOffAutomaticChanges(doc)) {
//				iGuidelineDataTransformer.addIssue(issueList, doc,
//						"No mechanism to turn off automatic content changes or context changes.", level, wcagVersion);
//				status = false;
//			}
//
//		} catch (WebDriverException e) {
//			logger.error("WebDriver encountered an issue: {}", e.getMessage(), e);
//		} catch (Exception e) {
//			logger.error("Unexpected error during validation: {}", e.getMessage(), e);
//		}
//
//		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, issueCount, successCount);
//	}

	public GuidelineResponse validateChangeOnRequest(Document doc, String url) {
		String guideline = "3.2.5 Change on Request";
		int issueCount = 0;
		int successCount = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true;

		try {
			driver = WebDriverFactory.getDriver();
			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// Check for meta refresh elements
			List<WebElement> metaRefreshElements = driver.findElements(By.xpath("//meta[@http-equiv='refresh']"));
			for (WebElement meta : metaRefreshElements) {
				String content = meta.getAttribute("content");
				if (content != null && !content.trim().isEmpty()) {
					String key ="Page contains meta refresh that may cause automatic redirection";
					iGuidelineDataTransformer.addIssue(issueList, meta,wcagVersion,//FIXME Snippet missing
							"Page contains meta refresh that may cause automatic redirection.", level);
					issueCount++;
					status = false;
				}
			}

			// Check for links opening new windows
			List<WebElement> linksOpeningNewWindows = driver.findElements(By.xpath("//a[@target='_blank']"));
			for (WebElement link : linksOpeningNewWindows) {
				String linkText = link.getText();
				if (linkText != null && !linkText.trim().isEmpty()) {
					String key = "Links open new windows without user request";
					iGuidelineDataTransformer.addIssue(issueList, link, linkText, url, getFix(key));
					issueCount++;//TODO TITLE -->Links open new windows without user request.
					status = false;
				}
			}

			// Check for a mechanism to turn off automatic changes
			if (!checkForMechanismToTurnOffAutomaticChanges(doc)) {
				String key = "No mechanism to turn off automatic content changes or context changes";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, doc,wcagVersion,// FIXME Snippet missing
						"No mechanism to turn off automatic content changes or context changes.", getFix(key));
				issueCount++;
				status = false;
			}

		} catch (WebDriverException e) {
			logger.error("WebDriver encountered an issue: {}", e.getMessage(), e);
		}

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	private boolean checkForMechanismToTurnOffAutomaticChanges(Document doc) {
		for (Element button : doc.select("button, a, input[type='button']")) {
			String buttonText = button.text().toLowerCase();
			if (buttonText.contains("disable") || buttonText.contains("stop")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public GuidelineResponse validateContextSensitiveHelp(Document doc, String url) {
		String guideline = "3.3.5 Help";

		int fieldsWithHelp = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);

			logger.info("Navigated to URL: {}", url);

			// List<WebElement> inputFields =
			// accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> inputFields = driver.findElements(By.xpath("//input | //textarea | //select"));

			for (WebElement field : inputFields) {
				if (!field.isDisplayed()) {
					continue;
				}

				try {
					String fieldId = field.getAttribute("id");
					List<WebElement> helpLinks = driver.findElements(By.xpath(
							"//label[@for='%s']//following-sibling::a[contains(@class, 'help') or contains(text(), 'help')]"
									.formatted(fieldId)));

					if (!helpLinks.isEmpty()) {
						fieldsWithHelp++;
					} else {
						String key = "No context-sensitive help provided for this field";
						iGuidelineDataTransformer.addIssue(issueList, field, driver.getTitle(),//FIXME Snippet missing
								"No context-sensitive help provided for this field.", getFix(key));
						status = false;
					}
				} catch (Exception e) {
					logger.error("Error processing field: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(fieldsWithHelp, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	@Override
	public GuidelineResponse validateAllErrorPrevention(Document doc, String url) {
		String guideline = "3.3.6 Error Prevention (All)";

		int successfulForms = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver();

			driver.get(url);
			logger.info("Navigated to URL: {}", url);

			// List<WebElement> forms = accessibilityValidator.getGlobalSeleniumData(url);

			List<WebElement> forms = driver.findElements(By.tagName("form"));

			for (WebElement form : forms) {

				boolean isValid = true;

				try {
					// Check if submission is reversible
					List<WebElement> undoButtons = form
							.findElements(By.xpath(".//button[contains(text(), 'Undo') or contains(@class, 'undo')]"));
					if (undoButtons.isEmpty()) {
						isValid = false;
						String key = "No mechanism to reverse submissions";
						iGuidelineDataTransformer.addIssue(issueList, form, wcagVersion,//FIXME Snippet missing
								"No mechanism to reverse submissions.",
								getFix(key));
					}

					// Check if data is validated for errors
					List<WebElement> errorMessages = form
							.findElements(By.xpath(".//*[contains(@class, 'error') or contains(text(), 'error')]"));
					if (errorMessages.isEmpty()) {
						isValid = false;
						String key = "No error validation mechanism detected";
						iGuidelineDataTransformer.addIssue(issueList, form, wcagVersion,//FIXME Snippet missing
								"No error validation mechanism detected.",
								getFix(key));
					}

					// Check if form provides confirmation mechanism
					List<WebElement> confirmButtons = form.findElements(
							By.xpath(".//button[contains(text(), 'Confirm') or contains(@class, 'confirm')]"));
					if (confirmButtons.isEmpty()) {
						isValid = false;
						String key = "No mechanism for confirming submissions before finalizing";
						iGuidelineDataTransformer.addIssue(issueList, form, wcagVersion,//FIXME Snippet missing
								"No mechanism for confirming submissions before finalizing.", getFix(key));
					}

					if (isValid) {
						successfulForms++;
					} else {
						status = false;
					}

				} catch (Exception e) {
					logger.error("Error processing form: {}", e.getMessage(), e);
					status = false;
				}
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(successfulForms, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);
		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, successCount, issueCount);
	}

	@Override
	public GuidelineResponse evaluateImagesOfText(Document doc, String url) {
		logger.info("evaluateImagesOfText for URL: {}", url);

		WebDriver driver = WebDriverFactory.getDriver(url);
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		String guideline = "1.4.9 Images of Text (No Exception)";
		String level = "AAA";
		String wcagVersion = "WCAG 2.1";
		boolean status = true;

		List<IssueDetails> issueList = new ArrayList<>();

		try {
			// Check all images for text violations
			checkImageElements(doc, driver, wait, issueList);
		}

		catch (Exception e) {
			logger.error("Error processing images of text: {}", e.getMessage(), e);
			status = false;
		}
		int totalCheckedElements = calculateTotalCheckedElements(driver);
		int issueCount = issueList.size();
		int successCount = totalCheckedElements - issueCount;

		// Log results
		logResults(totalCheckedElements, successCount, issueCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	private void checkImageElements(Document doc, WebDriver driver, WebDriverWait wait, List<IssueDetails> issueList) {
		List<WebElement> images = driver.findElements(By.tagName("img"));

		for (WebElement img : images) {
			try {
				wait.until(ExpectedConditions.visibilityOf(img));

				String altText = img.getAttribute("alt");
				String src = img.getAttribute("src");

				if (src == null || src.isEmpty()) {
					continue; // Skip images without a source
				}

				// Check if the alt text contains meaningful text
				if (altText != null && containsText(altText)) {
					String key = "Image contains text in alt attribute";
					issueList.add(new IssueDetails(key, altText, src, src, getFix(key)));
				}

				if (src != null && isImageContainingText(src)) {
					String key="Image may contain text";
					issueList.add(new IssueDetails("Image may contain text and does not allow for text customization.",
							"Possible text in image", src, src, getFix(key)));
				}

			} catch (TimeoutException e) {
				logger.warn("Timeout while waiting for image visibility: {}", e.getMessage());
			}
		}
	}

	private boolean containsText(String text) {
		return text.matches(".*[a-zA-Z0-9].*"); // Checks if the text contains any letters or numbers
	}

	private boolean isImageContainingText(String imageUrl) {
		// Placeholder: Implement OCR to detect text in images
		return false;
	}

	private int calculateTotalCheckedElements(WebDriver driver) {
		return driver.findElements(By.tagName("img")).size();
	}

	private void logResults(int totalChecked, int success, int issues) {
		logger.info("Total Checked Images: {}", totalChecked);
		logger.info("Successful Images (No Issues): {}", success);
		logger.info("Images with Issues: {}", issues);
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
