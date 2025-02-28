package com.brahos.accessibilitychecker.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
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
import com.brahos.accessibilitychecker.service.WcagAccessibilityGuidelines_2_0_AA;
import com.brahos.accessibilitychecker.utility.WebDriverFactory;

import ch.qos.logback.classic.Logger;
import jakarta.annotation.PostConstruct;

@Service
public class WcagAccessibilityGuidelinesImpl_2_0_AA implements WcagAccessibilityGuidelines_2_0_AA {

	private static final Logger logger = (Logger) LoggerFactory
			.getLogger(AccessibilityGuidelinesServiceImpl_2_0_A.class);

	private HelperGuidelinesExecutorService helperGuidelinesExecutorService;

	private GuidelineResponseBuilderService guidelineResponseBuilderService;

	private IGuidelineDataTransformer iGuidelineDataTransformer;

	private String level = "AA";

	private String wcagVersion = "2.0";

	private boolean status = true;

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

	private static final Pattern UNIT_PATTERN = Pattern.compile("(-?\\d*\\.?\\d+)(px|em|%|rem|deg|rad|grad)?");
	
	@Autowired
	private AccessibilityIssueFixesRepository issueFixesRepository;

	private Map<String, IssueFixDetails> fixes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	@PostConstruct
	public void init() {
		fixes = getAllFixesDataByVersion("2.0", "AA");
	}

	public Map<String, IssueFixDetails> getAllFixesDataByVersion(String wcagVersion, String level) {
		Optional<WcagFixesData> fixesData = issueFixesRepository.findByWcagVersionAndLevel(wcagVersion, level);
		return fixesData.map(data -> {
			Map<String, IssueFixDetails> caseInsensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			caseInsensitiveMap.putAll(data.getData());
			return caseInsensitiveMap;
		}).orElse(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
	}

	public WcagAccessibilityGuidelinesImpl_2_0_AA(HelperGuidelinesExecutorService helperGuidelinesExecutorService,
			GuidelineResponseBuilderService guidelineResponseBuilderService,
			IGuidelineDataTransformer iGuidelineDataTransformer) {
		this.helperGuidelinesExecutorService = helperGuidelinesExecutorService;
		this.guidelineResponseBuilderService = guidelineResponseBuilderService;
		this.iGuidelineDataTransformer = iGuidelineDataTransformer;

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

	private int getTotalCheckedElements(WebDriver driver) {
		if (driver == null) {
			return 0; // Prevent NullPointerException
		}

		try {
			// Find all relevant elements that need to be checked for accessibility
			List<WebElement> elements = driver
					.findElements(By.cssSelector("img, svg, picture, figure, video, audio, source, track, "
							+ "input, textarea, select, button, label, "
							+ "[role=img], [role=button], [role=checkbox], [role=link]"));

			return elements.size(); // Return total count of matching elements
		} catch (Exception e) {
			logger.error("Error counting checked elements: {}", e.getMessage(), e);
			return 0;
		}
	}

	public GuidelineResponse captionsLive(Document doc) {
		String guideline = "1.2.4 Captions (Live)";

		List<IssueDetails> issueList = new ArrayList<>();

		Elements liveVideos = doc.select("video[controls][live]");
		for (Element video : liveVideos) {

			boolean hasCaptions = !video.select("track[kind=captions]").isEmpty();

			if (!hasCaptions) {
				String key ="Missing captions for live video";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, video, video.toString(),
						key, getFix(key));
			} else {
				status = false;
			}
		}

		Elements liveAudios = doc.select("audio[controls][live]");
		for (Element audio : liveAudios) {

			boolean hasCaptions = !audio.select("track[kind=captions]").isEmpty();

			if (!hasCaptions) {
				String key ="Missing captions for live audio";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, audio, audio.toString(),
						key, getFix(key));
			}
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	public GuidelineResponse audioDescriptionPrerecorded(Document doc) {
		String guideline = "1.2.5 Audio Description (Prerecorded)";

		List<IssueDetails> issueList = new ArrayList<>();

		Elements videos = doc.select("video");
		for (Element video : videos) {

			boolean hasAudioDescription = !video.select("track[kind=descriptions]").isEmpty(); // Check for audio
																								// description

			if (!hasAudioDescription) {
				String key ="Missing audio description for prerecorded video";
				iGuidelineDataTransformer.addIssueUsingJsoup(issueList, video, video.toString(),
						key, getFix(key));
			} else {

				status = false;
			}
		}

		int issueCount = issueList.size();
		int totalChecked = getTotalCheckedElements(doc);
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);

	}

	@Override
	public GuidelineResponse evaluateTextContrast(Document doc, String url) {
		String guideline = "1.4.3 Contrast (Minimum)";
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		if (doc == null) {
			logger.error("Document is null. Cannot evaluate text contrast.");
			return buildGuidelineResponse(guideline, level, wcagVersion, null, false, 0, 0);
		}

		boolean status = true; // Default to true, turns false if issues are found
		int totalChecked = 0; // Count elements actually found by Selenium

		try {
			driver = WebDriverFactory.getDriver(url);

			Elements textElements = doc.select("div, p, span, a, li, h1, h2, h3, h4, h5, h6");
			for (Element element : textElements) {
				String cssSelector = generateValidCssSelector(element);
				if (cssSelector == null || cssSelector.isEmpty()) {
					logger.debug("Skipping element due to invalid CSS Selector: {}", element.tagName());
					continue;
				}

				try {
					List<WebElement> seleniumElements = driver.findElements(By.cssSelector(cssSelector));
					if (seleniumElements.isEmpty()) {
						continue; // Skip if not found in Selenium
					}

					totalChecked++; // Count only successfully found elements

					WebElement seleniumElement = seleniumElements.get(0); // Take the first match
					String textColor = seleniumElement.getCssValue("color");
					String backgroundColor = seleniumElement.getCssValue("background-color");

					if (isValidColor(textColor) && isValidColor(backgroundColor)) {
						double contrastRatio = calculateContrastRatio(textColor, backgroundColor);
						boolean isLargeText = isLargeText(seleniumElement);
						double requiredRatio = isLargeText ? 3.0 : 4.5;

						if (contrastRatio < requiredRatio) {
							status = false;
							addIssue(issueList, element, contrastRatio, requiredRatio, isLargeText);
						}
					}
				} catch (Exception e) {
					logger.error("Error processing element with CSS Selector {}: {}", cssSelector, e.getMessage());
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = totalChecked - issueCount;

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private String generateValidCssSelector(Element element) {
		try {
			String cssSelector = element.cssSelector();
			return (cssSelector != null && !cssSelector.trim().isEmpty()) ? cssSelector : null;
		} catch (Exception e) {
			logger.warn("Failed to generate CSS selector for element: {}", element, e);
			return null;
		}
	}

	private boolean isValidColor(String color) {
		return color != null && !color.trim().isEmpty() && !color.equals("transparent");
	}

	private void addIssue(List<IssueDetails> issueList, Element element, double contrastRatio, double requiredRatio,
			boolean isLargeText) {
		//TODO TITLE -->Contrast ratio does not meet the minimum WCAG requirement
		String key = "Contrast ratio does not meet the minimum WCAG requirement";
		String issueMessage = "Contrast ratio %.2f:1 does not meet the required %.1f:1 for %s text."
				.formatted(contrastRatio, requiredRatio, isLargeText ? "large" : "normal");
		iGuidelineDataTransformer.addIssueUsingJsoup(issueList, element, element.toString(), issueMessage, getFix(key));
	}

	private GuidelineResponse buildGuidelineResponse(String guideline, String level, String wcagVersion,
			List<IssueDetails> issues, boolean status, int successCount, int issueCount) {
		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issues, status,
				successCount, issueCount);
	}

	private double calculateContrastRatio(String foreground, String background) {
		int[] foregroundRgb = parseColor(foreground);
		int[] backgroundRgb = parseColor(background);

		double luminance1 = calculateRelativeLuminance(foregroundRgb);
		double luminance2 = calculateRelativeLuminance(backgroundRgb);

		double lighter = Math.max(luminance1, luminance2);
		double darker = Math.min(luminance1, luminance2);
		return (lighter + 0.05) / (darker + 0.05);
	}

	private double calculateRelativeLuminance(int[] rgb) {
		double[] linearRgb = new double[3];
		for (int i = 0; i < 3; i++) {
			double value = rgb[i] / 255.0;
			linearRgb[i] = value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * linearRgb[0] + 0.7152 * linearRgb[1] + 0.0722 * linearRgb[2];
	}

	private int[] parseColor(String color) {
		try {
			if (color.startsWith("rgb")) {
				return parseRgbColor(color);
			} else if (color.startsWith("#")) {
				return parseHexColor(color);
			}
		} catch (Exception e) {
			logger.error("Error parsing color: {}", color, e);
		}
		return new int[] { 255, 255, 255 }; // default to white if parsing fails
	}

	private int[] parseRgbColor(String rgb) {
		String[] components = rgb.replaceAll("[rgba()\\s]", "").split(",");
		return new int[] { Integer.parseInt(components[0].trim()), Integer.parseInt(components[1].trim()),
				Integer.parseInt(components[2].trim()) };
	}

	private int[] parseHexColor(String hex) {
		try {
			int r = Integer.parseInt(hex.substring(1, 3), 16);
			int g = Integer.parseInt(hex.substring(3, 5), 16);
			int b = Integer.parseInt(hex.substring(5, 7), 16);
			return new int[] { r, g, b };
		} catch (Exception e) {
			logger.error("Error parsing hex color: {}", hex, e);
			return new int[] { 255, 255, 255 }; // default to white if parsing fails
		}
	}

	private boolean isLargeText(WebElement element) {
		try {
			String fontSize = element.getCssValue("font-size");
			String fontWeight = element.getCssValue("font-weight");

			if (fontSize != null && fontWeight != null) {
				double size = Double.parseDouble(fontSize.replace("px", "").trim());
				boolean isBold = Integer.parseInt(fontWeight) >= 700;
				return size >= 18.5 || (isBold && size >= 14.0);
			}
		} catch (Exception e) {
			logger.error("Unable to parse font size or weight for element: {}", element, e);
		}
		return false;
	}

//	public GuidelineResponse resizeText(Document doc, String url) {
//		String guideline = "1.4.4 Resize Text";
//
//		WebDriver driver = null;
//		List<IssueDetails> issueList = new ArrayList<>();
//
//		try {
//			driver = WebDriverFactory.getDriver();
//			 WebDriver driver = WebDriverFactory.getDriver(url); 
//
//			logger.info("Navigated to URL: {}", url);
//
//			List<WebElement> textElements = driver
//					.findElements(By.cssSelector("p, h1, h2, h3, h4, h5, h6, span, a, li, button, input, textarea"));
//
//			for (WebElement seleniumElement : textElements) {
//
//				try {
//					String fontSize = seleniumElement.getCssValue("font-size");
//					boolean isFontSizeRescalable = isFontSizeRescalable(fontSize);
//
//					if (!isFontSizeRescalable) {
//						status = false;
//						iGuidelineDataTransformer.addIssue(issueList, seleniumElement, seleniumElement.getText(),
//								"Text cannot be resized or scaled properly, violating WCAG Resize Text criterion.",
//								level);
//					} else {
//
//					}
//				} catch (Exception e) {
//					logger.error("Error processing element: {}", seleniumElement, e);
//				}
//			}
//		} catch (Exception e) {
//			logger.error("Error during WebDriver operation: {}", e.getMessage(), e);
//			status = false;
//		}
//
//		int issueCount = issueList.size();
//		int totalChecked = getTotalCheckedElements(doc);
//		int successCount = totalChecked - issueCount;
//
//		// Update global counts
//		updateCounts(issueCount, successCount);
//
//		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
//	}

	@Override
	public GuidelineResponse resizeText(Document doc, String url) {
		String guideline = "1.4.4 Resize Text";
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true; // Default status

		try {

			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			// Use Selenium to find all text-based elements
			List<WebElement> textElements = driver
					.findElements(By.cssSelector("p, h1, h2, h3, h4, h5, h6, span, a, li, button, input, textarea"));

			int totalChecked = textElements.size(); // Ensure totalChecked is correctly set

			for (WebElement seleniumElement : textElements) {
				try {
					String fontSize = seleniumElement.getCssValue("font-size");
					boolean isFontSizeRescalable = isFontSizeRescalable(fontSize);

					if (!isFontSizeRescalable) {
						status = false;
						String key = "Text cannot be resized or scaled properly, violating WCAG Resize Text criterion";
						iGuidelineDataTransformer.addIssue(issueList, seleniumElement, seleniumElement.getText(),
								"Text cannot be resized or scaled properly, violating WCAG Resize Text criterion.",
								getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing element: {}", seleniumElement, e);
				}
			}

			int issueCount = issueList.size();
			int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values

			// Update global counts
			updateCounts(issueCount, successCount);

			return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);

		} catch (Exception e) {
			logger.error("Error during WebDriver operation: {}", e.getMessage(), e);
			return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
		}
	}

	private boolean isFontSizeRescalable(String fontSize) {
		if (fontSize == null || fontSize.isEmpty()) {
			return false;
		}
		return fontSize.matches(".*(em|rem|%|vw|vh).*");
	}

	@Override
	public GuidelineResponse checkImagesOfText(Document doc, String url) {
		String guideline = "1.4.5 Images of Text";

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true; // Default status as true
		int totalChecked = 0; // Initialize totalChecked properly

		try {

			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("img")));

			List<WebElement> imageElements = driver.findElements(By.tagName("img"));
			totalChecked = imageElements.size();

			if (imageElements.isEmpty()) {
				logger.info("No image elements found on the page: {}", url);
				return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList,
						true, 0, 0);
			}

			for (WebElement image : imageElements) {
				try {
					String altText = image.getAttribute("alt");
					boolean isTextImage = isImageContainingText(altText, image);

					if (isTextImage) {
						status = false; // Set status to false if issue found
						String imageSrc = image.getAttribute("src");
						String key = "Image contains text, violating WCAG Images of Text guideline";
						String issueMessage = "Image contains text, violating WCAG Images of Text guideline.";

						logger.warn("Non-compliant image found: {}", imageSrc);
						iGuidelineDataTransformer.addIssue(issueList, image, altText, issueMessage, getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing image element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver operation for URL {}: {}", url, e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(totalChecked - issueCount, 0); // Ensure non-negative count

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				totalChecked, successCount);
	}

	private boolean isImageContainingText(String altText, WebElement image) {
		if (altText != null && !altText.isEmpty() && altText.toLowerCase().contains("text")) {
			return true;
		}

		// Additional check for image file names containing "text"
		String srcAttribute = image.getAttribute("src");
		if (srcAttribute != null && srcAttribute.toLowerCase().contains("text")) {
			return true;
		}

		// More attributes can be analyzed if necessary
		return false;
	}

	public GuidelineResponse checkMultipleWays(Document doc, String url) {
		String guideline = "2.4.5 Multiple Ways";

		int totalChecked = 0;

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {

			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			// Check for search functionality
			boolean hasSearch = false;
			boolean hasSiteMap = false;
			boolean hasTableOfContents = false;

			List<WebElement> searchElements = driver
					.findElements(By.cssSelector("input[type='search'], form[action*='search']"));

			totalChecked = searchElements.size();
			if (!searchElements.isEmpty()) {
				hasSearch = true;
				logger.info("Search functionality detected.");
			} else {
				String issueDescription = "No search functionality detected on the page.";
				iGuidelineDataTransformer.transform(guideline, level, wcagVersion, issueList, totalChecked,
						totalChecked);
			}

			// Check for site map links
			List<WebElement> siteMapLinks = driver
					.findElements(By.cssSelector("a[href*='sitemap'], a[title*='sitemap']"));
			if (!siteMapLinks.isEmpty()) {
				hasSiteMap = true;
				logger.info("Site map detected.");
			} else {
				String issueDescription = "No site map detected on the page.";
				iGuidelineDataTransformer.transform(guideline, level, wcagVersion, issueList, totalChecked,
						totalChecked);
			}

			// Check for table of contents links
			List<WebElement> tocLinks = driver
					.findElements(By.cssSelector("a[href*='table-of-contents'], a[title*='table of contents']"));
			if (!tocLinks.isEmpty()) {
				hasTableOfContents = true;
				logger.info("Table of Contents detected.");
			} else {
				String issueDescription = "No Table of Contents detected on the page.";
				iGuidelineDataTransformer.transform(guideline, level, wcagVersion, issueList, totalChecked,
						totalChecked);
			}

			// Determine compliance based on available methods
			if ((hasSearch ? 1 : 0) + (hasSiteMap ? 1 : 0) + (hasTableOfContents ? 1 : 0) < 2) {
				status = false;
				String issueDescription = "Less than two navigation methods available for locating content.";
				iGuidelineDataTransformer.transform(guideline, level, wcagVersion, issueList, totalChecked,
						totalChecked);
			}

		} catch (Exception e) {
			logger.error("Error during WebDriver operation for URL {}: {}", url, e.getMessage());
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return guidelineResponseBuilderService.buildGuidelineResponse(guideline, level, wcagVersion, issueList, status,
				successCount, issueCount);
	}

//	@Override
//	public GuidelineResponse validateHeadingsAndLabels(Document doc, String url) {
//		String guideline = "2.4.6 Headings and Labels"; // WCAG guideline being validated
//		int totalCheckedElements = 0; // Counter for total elements checked
//		int successfulElements = 0; // Counter for elements that pass the validation
//		boolean status = true; // Tracks overall validation status
//		WebDriver driver = null;
//		List<IssueDetails> issueList = new ArrayList<>(); // List to store detected issues
//
//		try {
//
//			driver = WebDriverFactory.getDriver(url); // Navigate to the provided URL
//			logger.info("Navigated to URL: {}", url);
//
//			// Validate headings (h1-h6)
//			List<WebElement> headingElements = driver.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));
//
//			int totalChecked = headingElements.size();
//			for (WebElement element : headingElements) {
//				totalCheckedElements++;
//				try {
//					String text = element.getText().trim(); // Extract and trim heading text
//					if (text.isEmpty() || !isDescriptive(text)) { // Check if the heading is descriptive
//						status = false;
//						iGuidelineDataTransformer.addIssue(issueList, element, text,
//								"Heading is not descriptive or clear.", "AA"); // Log issue if invalid
//					} else {
//						successfulElements++; // Increment successful count if valid
//					}
//				} catch (Exception e) {
//					logger.error("Error processing heading element: {}", e.getMessage(), e);
//				}
//			}
//
//			// Validate labels (label elements)
//			List<WebElement> labelElements = driver.findElements(By.cssSelector("label"));
//			for (WebElement element : labelElements) {
//				totalCheckedElements++;
//				try {
//					String text = element.getText().trim(); // Extract and trim label text
//					if (text.isEmpty() || !isDescriptive(text)) { // Check if the label is descriptive
//						status = false;
//						iGuidelineDataTransformer.addIssue(issueList, element, text,
//								"Label is not descriptive or clear.", "AA"); // Log issue if invalid
//					} else {
//						successfulElements++; // Increment successful count if valid
//					}
//				} catch (Exception e) {
//					logger.error("Error processing label element: {}", e.getMessage(), e);
//				}
//			}
//		} catch (Exception e) {
//			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
//			status = false;
//		}
//
//		int issueCount = issueList.size();
//		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values
//
//		// Update global counts
//		updateCounts(issueCount, successCount);
//
//		// Build and return the guideline response object
//		return buildGuidelineResponse(guideline, "AA", "WCAG 2.1", issueList, status, successfulElements,
//				totalCheckedElements);
//	}

	@Override
	public GuidelineResponse validateHeadingsAndLabels(Document doc, String url) {
		String guideline = "2.4.6 Headings and Labels";

		int totalCheckedElements = 0;

		boolean status = true;
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			// Validate headings (h1-h6)
			List<WebElement> headingElements = driver.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));
			List<WebElement> labelElements = driver.findElements(By.cssSelector("label"));

			totalCheckedElements += headingElements.size() + labelElements.size(); // Corrected total count

			for (WebElement element : headingElements) {
				try {
					String text = element.getText();
					if (text == null || text.isEmpty() || !isDescriptive(text)) {
						status = false;
						String key = "Heading is not descriptive or clear";
						iGuidelineDataTransformer.addIssue(issueList, element, text,
								"Heading is not descriptive or clear.", getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing heading element: {}", e.getMessage(), e);
				}
			}

			// Validate labels (label elements)
			for (WebElement element : labelElements) {
				try {
					String text = element.getText();
					if (text == null || text.isEmpty() || !isDescriptive(text)) {
						status = false;
						String key = "Label is not descriptive or clear";
						iGuidelineDataTransformer.addIssue(issueList, element, text,
								"Label is not descriptive or clear.", getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing label element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = totalCheckedElements - issueCount; // Ensuring correct success count
		status = issueList.isEmpty(); // Set status based on issue presence

		// Update global counts
		updateCounts(issueCount, successCount);

		// Build and return the guideline response object
		return buildGuidelineResponse(guideline, "AA", "WCAG 2.1", issueList, status, successCount, issueCount);
	}

	// Helper method to check if the text is descriptive enough
	private boolean isDescriptive(String text) {
		return text.length() > 3 && !text.trim().matches(".*\\d.*"); // Ensures text is meaningful and not just numbers
	}

	public GuidelineResponse validateFocusVisible(Document doc, String url) {
		String guideline = "2.4.7 Focus Visible";

		int totalCheckedElements = 0;
		int successfulElements = 0;
		boolean status = true; // Ensure status is initialized

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {

			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			// Identify all focusable elements
			List<WebElement> focusableElements = driver.findElements(
					By.cssSelector("a, button, input, select, textarea, [tabindex]:not([tabindex='-1'])"));

			totalCheckedElements = focusableElements.size();

			for (WebElement element : focusableElements) {
				try {
					// Check if the focus indicator is visible when the element is focused
					if (!isFocusVisible(driver, element)) {
						status = false;
						String key = "Element does not show a visible focus indicator when focused";
						iGuidelineDataTransformer.addIssue(issueList, element, element.getTagName(),
								"Element does not show a visible focus indicator when focused.", getFix(key));
					} else {
						successfulElements++;
					}
				} catch (Exception e) {
					logger.error("Error processing focusable element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();
		int successCount = Math.max(totalCheckedElements - issueCount, 0); // Use totalCheckedElements

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successfulElements,
				totalCheckedElements);

	}

	// Helper method to check if a focus indicator is visible
	private boolean isFocusVisible(WebDriver driver, WebElement element) {
		// Simulate focusing the element and check if the outline or border changes
		JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

		// Focus the element
		jsExecutor.executeScript("arguments[0].focus();", element);

		// Check if the outline or border of the element is visible (can be customized)
		String outline = element.getCssValue("outline");
		String border = element.getCssValue("border");

		// A valid visible focus should have a non-transparent outline or border
		return !outline.equals("none") || !border.equals("none");
	}

	public GuidelineResponse validateLanguageOfParts(Document doc, String url) {
		String guideline = "3.1.2 Language of Parts";

		int successCount = 0;
		boolean status = true; // Initialize status properly
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();

		try {
			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			// Select all elements that could contain language changes (e.g., spans, divs,
			// etc.)
			List<WebElement> elements = driver.findElements(By.cssSelector("*[lang]"));

			for (WebElement element : elements) {
				try {
					String langAttribute = element.getAttribute("lang");

					if (langAttribute == null || langAttribute.trim().isEmpty()) {
						status = false;
						String key = "Element has a 'lang' attribute but it is empty or not specified";
						iGuidelineDataTransformer.addIssue(issueList, element, "",
								"Element has a 'lang' attribute but it is empty or not specified.", getFix(key));
					} else if (!isValidLanguageTag(langAttribute)) {
						status = false;
						String key = "The 'lang' attribute contains an invalid language tag";
						iGuidelineDataTransformer.addIssue(issueList, element, langAttribute,
								"The 'lang' attribute contains an invalid language tag.", getFix(key));
					} else {
						successCount++; // Count valid language elements
					}
				} catch (Exception e) {
					logger.error("Error processing element with 'lang' attribute: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
		}

		int issueCount = issueList.size();

// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
	}

	/**
	 * Validates whether the given language tag conforms to BCP 47 standards.
	 *
	 * @param langTag The language tag to validate.
	 * @return True if the language tag is valid; false otherwise.
	 */
	private boolean isValidLanguageTag(String langTag) {
		// Basic validation for BCP 47 language tags
		return langTag.matches("^[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?$");
	}

	public GuidelineResponse validateConsistentNavigation(Document doc, String url) {

		WebDriver driver = null;
		String guideline = "3.2.3 Consistent Navigation";
		boolean status = true; // Default validation status

		driver = WebDriverFactory.getDriver(url);

		List<IssueDetails> issueList = new ArrayList<>();
		Map<String, org.openqa.selenium.Point> initialPositions = new HashMap<>();

		int totalCheckedSections = 0;
		int successfulElements = 0; // Track successfully validated elements

		driver = WebDriverFactory.getDriver(url);

		if (driver == null) {
			logger.error("WebDriver could not be initialized.");
			return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
		}

		try {

			logger.info("Navigated to URL: {}", url);

			// ✅ Identify all navigation elements
			List<WebElement> navigationElements = driver
					.findElements(By.xpath("//nav | //*[contains(@class, 'navigation')]")).stream()
					.filter(WebElement::isDisplayed).collect(Collectors.toList());

			int totalChecked = navigationElements.size();

			// ✅ Store initial positions of navigation elements
			for (WebElement element : navigationElements) {
				String uniqueIdentifier = createUniqueIdentifier(element, totalCheckedSections);
				initialPositions.put(uniqueIdentifier, element.getLocation());
				logger.info("Stored element [{}] at position {}", uniqueIdentifier, element.getLocation());
			}

			// ✅ Click on the first navigable link
			List<WebElement> links = driver.findElements(By.xpath("//a[@href and not(contains(@href, '#'))]"));
			if (links.isEmpty()) {
				logger.warn("No navigable links found on the page.");
				return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
			}

			WebElement firstLink = links.get(0);
			new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(firstLink))
					.click();
			new WebDriverWait(driver, Duration.ofSeconds(10))
					.until(ExpectedConditions.not(ExpectedConditions.urlToBe(url))); // Wait for navigation

			// ✅ Check new navigation element positions
			List<WebElement> newNavigationElements = driver
					.findElements(By.xpath("//nav | //*[contains(@class, 'navigation')]"));

			for (WebElement element : newNavigationElements) {
				String uniqueIdentifier = createUniqueIdentifier(element, totalCheckedSections);
				if (initialPositions.containsKey(uniqueIdentifier)) {
					org.openqa.selenium.Point initialPosition = initialPositions.get(uniqueIdentifier);
					org.openqa.selenium.Point currentPosition = element.getLocation();

					if (!initialPosition.equals(currentPosition)) {
						String key="Navigation element position has changed across pages";
						iGuidelineDataTransformer.addIssue(issueList, element,driver.getTitle(),//FIXME Snippet missing
								"Navigation element position has changed across pages.", getFix(key));
						status = false;
					} else {
						successfulElements++; // Count element as successfully validated
					}
				}
			}
		} catch (WebDriverException e) {
			logger.error("WebDriver encountered an issue: {}", e.getMessage(), e);
			return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
		} catch (Exception e) {
			logger.error("Unexpected error during validation: {}", e.getMessage(), e);
			return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
		}

		int issueCount = issueList.size();
		int successCount = successfulElements; // ✅ Track success explicitly

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private String createUniqueIdentifier(WebElement element, int totalCheckedSections) {

		String elementId = element.getAttribute("id");
		if (elementId == null || elementId.isEmpty()) {
			elementId = element.getTagName() + "_" + totalCheckedSections;
		}
		return elementId + "_" + totalCheckedSections;
	}

	@Override
	public GuidelineResponse validateConsistentIdentification(Document doc, String url) {
		String guideline = "3.2.4 Consistent Identification";
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true; // Default validation status
		int totalChecked = 0; // Track total checked elements across pages

		try {
			WebDriverFactory.getDriver(url);
			if (driver == null) {
				logger.error("WebDriver could not be initialized.");
				return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
			}

			Map<String, String> previousPageIdentifiers = new HashMap<>();
			List<String> urls = extractUrls(doc);

			for (String currentUrl : urls) {
				driver.get(currentUrl);
				logger.info("Navigated to URL: {}", currentUrl);

				List<WebElement> components = driver.findElements(By.xpath("//button | //a | //img[@alt]")).stream()
						.filter(WebElement::isDisplayed).collect(Collectors.toList());

				totalChecked += components.size(); // Accumulate across pages

				for (WebElement component : components) {
					String label = getLabel(component);
					if (label == null || label.trim().isEmpty()) {
						continue; // Skip elements without valid labels
					}

					if (previousPageIdentifiers.containsKey(label)) {
						String previousUrl = previousPageIdentifiers.get(label);
						if (!previousUrl.equals(currentUrl)) {
							String key = "Inconsistent labeling for component";
							iGuidelineDataTransformer.addIssue(issueList, component,driver.getTitle(),//FIXME Snippet missing
									"Inconsistent labeling for component.", getFix(key));
							status = false;
						}
					} else {
						previousPageIdentifiers.put(label, currentUrl);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error during validation: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
	}

	private String getLabel(WebElement component) {
		String label = "";

		if (component.getTagName().equalsIgnoreCase("button")) {
			label = component.getText();
		} else if (component.getTagName().equalsIgnoreCase("a")) {
			label = component.getText();
		} else if (component.getTagName().equalsIgnoreCase("img")) {

			label = component.getAttribute("alt");
		}

		return label.trim();
	}

	private List<String> extractUrls(Document doc) {
		List<String> urls = new ArrayList<>();

		Elements links = doc.select("a[href]");

		for (Element link : links) {
			String href = link.attr("href");

			if (!href.startsWith("http") && !href.startsWith("https")) {

			} else {
				urls.add(href);
			}
		}

		return urls;
	}

	@Override
	public GuidelineResponse validateErrorSuggestion(Document doc, String url) {
		String guideline = "3.3.3 Error Suggestion";
		String level = "AA"; // WCAG level
		String wcagVersion = "WCAG 2.1";

		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true;

		try {
			driver = WebDriverFactory.getDriver(url);
			logger.info("Navigated to URL: {}", url);

			List<String> urls = extractUrls(doc);

			for (String currentUrl : urls) {
				driver.get(currentUrl);
				logger.info("Navigated to URL: {}", currentUrl);

				List<WebElement> inputFields = driver.findElements(By.xpath("//input | //textarea | //select")).stream()
						.filter(WebElement::isDisplayed).collect(Collectors.toList());

				for (WebElement field : inputFields) {
					String fieldType = field.getAttribute("type");

					if (fieldType != null
							&& (fieldType.equals("text") || fieldType.equals("email") || fieldType.equals("number"))) {
						field.clear();
						field.sendKeys("invalid input");
					} else if (field.getTagName().equals("select")) {
						Select select = new Select(field);
						select.selectByIndex(0); 
					}

					WebElement submitButton = driver
							.findElements(By.xpath("//button[@type='submit'] | //input[@type='submit']")).stream()
							.findFirst().orElse(null);

					if (submitButton != null) {
						submitButton.click();
					}

					// Check for error messages and suggestions
					List<WebElement> errorMessages = driver
							.findElements(By.xpath("//*[contains(@class, 'error') or contains(@id, 'error')]"));

					if (errorMessages.isEmpty()) {
						String key = "No error suggestion provided for invalid input";
						iGuidelineDataTransformer.addIssue(issueList, field, driver.getTitle(), //FIXME Snippet missing
								"No error suggestion provided for invalid input.", getFix(key));
						status = false;
					} else {
						boolean hasSuggestion = errorMessages.stream()
								.anyMatch(error -> error.getText().toLowerCase().contains("suggest"));

						if (hasSuggestion) {
							// fieldsWithSuggestions++;
						} else {
							String key = "Error message does not provide suggestions for correction";
							iGuidelineDataTransformer.addIssue(issueList, field,driver.getTitle(), //FIXME Snippet missing
									"Error message does not provide suggestions for correction.", getFix(key));
							status = false;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error during validation: {}", e.getMessage(), e);
			status = false;
		}

		int totalChecked = getTotalCheckedElements(doc);
		int issueCount = issueList.size();
		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
	}

	public GuidelineResponse validateErrorPreventionForCriticalActions(Document doc, String url) {
		String guideline = "Error Prevention (Legal, Financial, Data)";
		WebDriver driver = null;
		List<IssueDetails> issueList = new ArrayList<>();
		boolean status = true; // Default validation status
		int totalChecked = 0; // Track total checked forms

		try {
			driver = WebDriverFactory.getDriver(url);
			if (driver == null) {
				logger.error("WebDriver could not be initialized.");
				return buildGuidelineResponse(guideline, level, wcagVersion, issueList, false, 0, 0);
			}

//			 WebDriver driver = WebDriverFactory.getDriver(url); 
			logger.info("Navigated to URL: {}", url);

			// Select all forms related to critical actions
			List<WebElement> criticalForms = driver.findElements(By.cssSelector("form[data-critical='true']"));
			totalChecked = criticalForms.size();

			for (WebElement form : criticalForms) {
				boolean hasErrorPrevention = false;

				try {
					// ✅ Check for a confirmation step
					if (hasReviewOrConfirmationMechanism(form)) {
						logger.info("Form has a review or confirmation mechanism.");
						hasErrorPrevention = true;
					}

					// ✅ Check for input validation
					if (!hasErrorPrevention && hasInputValidation(form)) {
						logger.info("Form has input validation.");
						hasErrorPrevention = true;
					}

					// ✅ Check if the submission is reversible
					if (!hasErrorPrevention && isSubmissionReversible(form)) {
						logger.info("Form submission is reversible.");
						hasErrorPrevention = true;
					}

					if (!hasErrorPrevention) {
						status = false;
						String key = "The form does not implement required error prevention mechanisms (reversible, checked, or confirmed)";
						iGuidelineDataTransformer.addIssue(issueList, form, "",
								"The form does not implement required error prevention mechanisms (reversible, checked, or confirmed).",
								getFix(key));
					}
				} catch (Exception e) {
					logger.error("Error processing critical form element: {}", e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during WebDriver initialization or navigation: {}", e.getMessage(), e);
			status = false;
		}

		int issueCount = issueList.size();
		int successCount = Math.max(totalChecked - issueCount, 0); // Prevent negative values

		// Update global counts
		updateCounts(issueCount, successCount);

		return buildGuidelineResponse(guideline, level, wcagVersion, issueList, status, successCount, issueCount);
	}

	/**
	 * Checks if the form has a review or confirmation mechanism.
	 *
	 * @param form The form element to check.
	 * @return True if the form has a review or confirmation mechanism; false
	 *         otherwise.
	 */
	private boolean hasReviewOrConfirmationMechanism(WebElement form) {
		List<WebElement> reviewIndicators = form
				.findElements(By.xpath(".//*[contains(text(), 'Review') or contains(text(), 'Confirm')]"));
		return !reviewIndicators.isEmpty();
	}

	/**
	 * Checks if the form provides input validation.
	 *
	 * @param form The form element to check.
	 * @return True if input validation is present; false otherwise.
	 */
	private boolean hasInputValidation(WebElement form) {
		List<WebElement> inputs = form.findElements(By.tagName("input"));
		for (WebElement input : inputs) {
			if (input.getAttribute("required") != null || input.getAttribute("pattern") != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the form submission is reversible.
	 *
	 * @param form The form element to check.
	 * @return True if the form submission can be reversed; false otherwise.
	 */
	private boolean isSubmissionReversible(WebElement form) {
		List<WebElement> undoButtons = form
				.findElements(By.xpath(".//button[contains(text(), 'Undo') or contains(text(), 'Cancel')]"));
		return !undoButtons.isEmpty();
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
