package com.brahos.accessibilitychecker.helper.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.helper.service.IGuidelineDataTransformer;
import com.brahos.accessibilitychecker.model.GuidelineData;
import com.brahos.accessibilitychecker.model.IssueDetails;

@Service
public class IGuidelineDataTransformerImpl implements IGuidelineDataTransformer {

	@Override
	public void addIssueUsingJsoupForCss(List<IssueDetails> issueList, Element element, String snippetHtml,
			String issueDescription, String level) {
		if (element == null || issueDescription == null || issueDescription.isEmpty()) {
			throw new IllegalArgumentException("Element or issue description cannot be null or empty");
		}

		IssueDetails issue = new IssueDetails();
		issue.setTitle(issueDescription);
		issue.setTagName(element.tagName());
		issue.setSnippet(snippetHtml);
		
		String cssSelector = generateCssSelectorJsoup(element);
		
		issue.setSelector(cssSelector); // Store the CSS selector
		issue.setFixes(level);

		issueList.add(issue);
	}

	private String generateCssSelectorJsoup(Element element) {
		if (element == null)
			return "";

		StringBuilder selector = new StringBuilder();
		Element current = element;

		while (current != null && !current.tagName().equals("html")) {
			String tag = current.tagName();
			String id = current.id();
			String classes = current.className().trim();

			if (!id.isEmpty()) {
				selector.insert(0, tag + "#" + id);
				break;
			}

			if (!classes.isEmpty()) {
				selector.insert(0, tag + "." + classes.replace(" ", "."));
			} else {
				int index = getElementIndex(current);
				selector.insert(0, tag + ":nth-of-type(" + index + ")");
			}

			current = current.parent();
			if (current != null && !current.tagName().equals("html")) {
				selector.insert(0, " > ");
			}
		}

		return selector.toString();
	}

	
	
	private int getElementIndex(Element element) {
		if (element == null || element.parent() == null) {
			return 1;
		}

		int index = 1;
		for (Element sibling : element.parent().children()) {
			if (sibling.tagName().equals(element.tagName())) {
				if (sibling == element) {
					return index;
				}
				index++;
			}
		}
		return index;
	}

	public void addIssueUsingJsoup(List<IssueDetails> issueList, Element element, String elementHtmlSnippet,
			String issueDescription, String fix) {
		// Validate input
		if (element == null || issueDescription == null || issueDescription.isEmpty()) {
			throw new IllegalArgumentException("Element or issue description cannot be null or empty");
		}

		// Create a new IssueDetails object
		IssueDetails issue = new IssueDetails();

		// Set the title (description of the issue)
		issue.setTitle(issueDescription);

		// Set the tag name of the element causing the issue (e.g., img, video, input,
		// etc.)
		issue.setTagName(element.tagName());

		// Set the snippet of the element (HTML of the element, for reference)
		issue.setSnippet(elementHtmlSnippet);

		// Set the CSS selector for the element (to help locate it in the document)
		String cssSelector = generateCssSelector(element); // Use a custom method for better accuracy
		issue.setSelector(cssSelector);

		// Set the suggested fix level (e.g., "A", "AA", "AAA")
		issue.setFixes(fix);

		// Add the issue to the list
		issueList.add(issue);
	}

	/**
	 * Helper method to generate a more reliable CSS selector for the Element. This
	 * can be customized for your needs, e.g., adding ID or class or other
	 * attributes.
	 */
	private String generateCssSelector(Element element) {
		StringBuilder selector = new StringBuilder(element.tagName());

		// If the element has an ID, include it in the selector (most specific)
		String id = element.id();
		if (id != null && !id.isEmpty()) {
			selector.append("#").append(id);
		}

		// If the element has a class, include it (less specific than ID but still
		// useful)
		String className = element.className();
		if (className != null && !className.isEmpty()) {
			selector.append(".").append(className.replace(" ", "."));
		}

		// Optionally, you can add more attributes or other logic here for uniqueness

		return selector.toString();
	}

//
//	@Override
//	public GuidelineData transform(String guideline, String level, String wcagVersion, List<IssueDetails> issueList,
//			int guidelineIssueCount, int guidelineSuccessCount, int totalIssueCount, int totalSuccessCount) {
//
//		GuidelineData guidelineData = new GuidelineData();
//		guidelineData.setGuideline(guideline);
//		guidelineData.setLevel(level);
//		guidelineData.setWcagVersion(wcagVersion);
//		guidelineData.setIssueDetails(new ArrayList<>(issueList));
//		guidelineData.setGuidelineIssueCount(guidelineIssueCount);
//		guidelineData.setGuidelineSuccessCount(guidelineSuccessCount);
//
//		guidelineData.setGuidelineIssueCount(totalIssueCount);
//		guidelineData.setGuidelineSuccessCount(totalSuccessCount);
//
//		return guidelineData;
//	}
//
	@Override
	public GuidelineData transform(String guideline, String level, String wcagVersion, List<IssueDetails> issueList,
			int issueCount, int successCount) {
		GuidelineData guidelineData = new GuidelineData();
		guidelineData.setGuideline(guideline);
		guidelineData.setLevel(level);
		guidelineData.setWcagVersion(wcagVersion);
		guidelineData.setIssueDetails(new ArrayList<>(issueList));
		guidelineData.setIssueCount(issueCount);
		guidelineData.setSuccessCount(successCount);

		return guidelineData;
	}

	public void addIssue(List<IssueDetails> issueList, WebElement element, String elementHtmlSnippet,
			String issueDescription, String fix) {
		if (element == null || issueDescription == null || issueDescription.isEmpty()) {
			throw new IllegalArgumentException("Element or issue description cannot be null or empty");
		}

// Create a new IssueDetails object
		IssueDetails issue = new IssueDetails();

// Set the title (description of the issue)
		issue.setTitle(issueDescription);

// Set the tag name of the element causing the issue (e.g., img, video, input, etc.)
		String tagName = element.getTagName();
		issue.setTagName(tagName);

// Set the snippet of the element (HTML of the element, for reference)
		issue.setSnippet(elementHtmlSnippet);

// Get the CSS selector using a more reliable strategy
		String cssSelector = generateCssSelector(element);

// Set the CSS selector for the element
		issue.setSelector(cssSelector);

// Set the suggested fix level (e.g., "A", "AA", "AAA")
		issue.setFixes(fix); // Assuming "level" corresponds to the fix level

// Add the issue to the list
		issueList.add(issue);
	}

	/**
	 * Helper method to generate a unique CSS selector for a WebElement. This method
	 * can be expanded to generate more accurate selectors.
	 */
	private String generateCssSelector(WebElement element) {
// Try to build a more specific selector (you can add custom logic here)
		StringBuilder selector = new StringBuilder(element.getTagName());

		if (element.getAttribute("id") != null && !element.getAttribute("id").isEmpty()) {
			selector.append("#").append(element.getAttribute("id"));
		} else if (element.getAttribute("class") != null && !element.getAttribute("class").isEmpty()) {
			selector.append(".").append(element.getAttribute("class").replace(" ", "."));
		}

		return selector.toString();
	}

//	@Override
//	public void addIssue(List<IssueDetails> issueList, WebElement link, String linkText, String issueDescription,
//			String suggestion, String level) {
//		// Validate inputs
//		if (link == null || issueDescription == null || issueDescription.isEmpty()) {
//			throw new IllegalArgumentException("WebElement or issue description cannot be null or empty");
//		}
//
//		// Create a new IssueDetails object
//		IssueDetails issue = new IssueDetails();
//
//		// Set the title (description of the issue)
//		issue.setTitle(issueDescription);
//
//		// Set the tag name of the element causing the issue (e.g., "a" for links)
//		issue.setTagName(link.getTagName());
//
//		// Set the snippet of the element (HTML of the element, for reference)
//		issue.setSnippet(linkText != null && !linkText.isEmpty() ? linkText : link.getAttribute("outerHTML"));
//
//		// Get the CSS selector using a more reliable strategy
//		String cssSelector = generateCssSelector(link);
//
//		// Set the CSS selector for the element
//		issue.setSelector(cssSelector);
//
//		// Set the suggested fix level (e.g., "A", "AA", "AAA")
//		issue.setFixes(level);
//
//		// Add the issue to the list
//		issueList.add(issue);
//	}

	@Override
	public GuidelineData transformDataOfIssueNull(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, int guidelineIssueCount, int guidelineSuccessCount, int totalIssueCount,
			int totalSuccessCount) {

		GuidelineData guidelineData = new GuidelineData();
		guidelineData.setGuideline(guideline);

//		guidelineData.setGuidelineIssueCount(guidelineIssueCount);
		// guidelineData.setGuidelineSuccessCount(guidelineSuccessCount);

		return guidelineData;
	}

//	@Override
//	public void addIssue(List<IssueDetails> issueList, WebElement webElement, String tempImg, String issueDescription,
//			String level, WebDriver driver) {
//		if (webElement == null || issueDescription == null || issueDescription.isEmpty()) {
//			throw new IllegalArgumentException("Element or issue description cannot be null or empty");
//		}
//
//// Create a new IssueDetails object
//		IssueDetails issue = new IssueDetails();
//
//// Set the title (description of the issue)
//		issue.setTitle(issueDescription);
//
//// Set the tag name of the element causing the issue (e.g., img, video, input, etc.)
//		String tagName = webElement.getTagName();
//		issue.setTagName(tagName);
//
//// Set the snippet of the webElement (HTML of the element, for reference)
//		String elementHtmlSnippet = webElement.getAttribute("outerHTML");
//		issue.setSnippet(elementHtmlSnippet);
//
//// Get the CSS selector using a more reliable strategy
////		String cssSelector = generateCssSelector(element);
//
//		String cssSelector = getCssSelector(driver, webElement);
//
//		String stripTrailing = cssSelector.stripTrailing();
//		
//		
//		
//
//		System.out.println("cssSelector.intern();.,,,,,,,,,,,,,,,,,,,,,,,,,,,,"+cssSelector.intern());
//		
//		System.out.println("stripTrailing.,,,,,,,,,,,,,,,,,,,,,,,,,,,,"+stripTrailing);
//		
//// Set the CSS selector for the element
//		issue.setSelector(cssSelector);
//
//// Set the suggested fix level (e.g., "A", "AA", "AAA")
//		issue.setFixes(level);
//
//// Add the issue to the list
//		issueList.add(issue);
//	}

	@Override
	public void addIssueSelenium(List<IssueDetails> issueList, List<WebElement> landmarks, String tempImg,
			String issueDescription, String level, WebElement webElement, WebDriver driver) {
		if (webElement == null || issueDescription == null || issueDescription.isEmpty()) {
			throw new IllegalArgumentException("Element or issue description cannot be null or empty");
		}

		// Create a new IssueDetails object
		IssueDetails issue = new IssueDetails();

		// Set the title (description of the issue)
		issue.setTitle(issueDescription);

		// Set the tag name of the element causing the issue (e.g., img, video, input,
		// etc.)
		String tagName = webElement.getTagName();
		issue.setTagName(tagName);

		// Set the snippet of the webElement (HTML of the element, for reference)
		String elementHtmlSnippet = webElement.getAttribute("outerHTML");
		issue.setSnippet(elementHtmlSnippet);

		// Get the CSS selector using a more reliable strategy
		String cssSelector = generateCssSelector(webElement);

		// Set the CSS selector for the element
		issue.setSelector(cssSelector);

		// Set the suggested fix level (e.g., "A", "AA", "AAA")
		issue.setFixes(level);

		// Add the issue to the list
		issueList.add(issue);
	}

	public static String getCssSelector(WebDriver driver, WebElement element) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		return (String) js.executeScript("function getCSSSelector(element) { "

				+ "    var path = []; " + "    while (element.nodeType === Node.ELEMENT_NODE) { "
				+ "        var selector = element.nodeName.toLowerCase(); " + "        if (element.id) { "
				+ "            selector += '#' + element.id; " + "        } else if (element.className) { "
				+ "            selector += '.' + element.className.split(' ').join('.'); " + "        } "
				+ "        path.unshift(selector); " + "        element = element.parentNode; " + "    } "
				+ "    return path.join(' > '); " + "} " + "return getCSSSelector(arguments[0]);", element);
	}

	@Override
	public void addIssue(List<IssueDetails> issueList, Element element, String title, String issueDescription,
			String fix) {
		if (element == null || issueDescription == null || issueDescription.isEmpty()) {
			throw new IllegalArgumentException("Element or issue description cannot be null or empty");
		}

// Create a new IssueDetails object
		IssueDetails issue = new IssueDetails();

// Set the title (description of the issue)
		issue.setTitle(issueDescription);

// Set the tag name of the element causing the issue (e.g., img, video, input, etc.)
		String tagName = element.tagName();
		issue.setTagName(tagName);

// Set the snippet of the element (HTML of the element, for reference)
		issue.setSnippet(element.html());

// Get the CSS selector using a more reliable strategy
		String cssSelector = generateCssSelector(element);

// Set the CSS selector for the element
		issue.setSelector(cssSelector);

// Set the suggested fix level (e.g., "A", "AA", "AAA")
		issue.setFixes(fix); // Assuming "level" corresponds to the fix level

// Add the issue to the list
		issueList.add(issue);
	}

//	public static String getCssSelector(WebDriver driver, WebElement element) {
//
////		WebElement link = driver.findElement(By.cssSelector(""));
////		link.click();
////		
////		
////		System.out.println("link................................."+link);
//
//		JavascriptExecutor js = (JavascriptExecutor) driver;
//		return (String) js.executeScript("function getCSSSelector(element) { " 
//				+ "    var path = [];"
//				+ "    while (element.nodeType === Node.ELEMENT_NODE) {"
//				+ "        var selector = element.nodeName.toLowerCase();" 
//				+ "        if (element.id) {"
//				+ "            selector += '#' + element.id;" 
//				+ "        } else if (element.className) {"
//				+ "            selector += '.' + element.className.split(' ').join('.');" 
//				+ "        }"
//				+ "        path.unshift(selector);" 
//				+ "        element = element.parentNode;" 
//				+ "    }"
//				+ "    return path.join(' > ');" + "}" 
//				+ "return getCSSSelector(arguments[0]);", element);
//	}

//	public String getCssSelector(WebDriver driver, WebElement element) {
//		JavascriptExecutor js = (JavascriptExecutor) driver;
//		return (String) js.executeScript("function getCssSelector(el) {" + "   if (!(el instanceof Element)) return;"
//				+ "   let path = [];" + "   while (el.nodeType === Node.ELEMENT_NODE) {"
//				+ "       let selector = el.nodeName.toLowerCase();" + "       if (el.id) {"
//				+ "           selector += '#' + el.id;" + "           path.unshift(selector);" + "           break;"
//				+ "       } else {" + "           let sibling = el, nth = 1;"
//				+ "           while (sibling = sibling.previousElementSibling) {"
//				+ "               if (sibling.nodeName.toLowerCase() == selector) nth++;" + "           }"
//				+ "           selector += ':nth-of-type(' + nth + ')';" + "       }" + "       path.unshift(selector);"
//				+ "       el = el.parentNode;" + "   }" + "   return path.join(' > ');"
//				+ "} return getCssSelector(arguments[0]);", element);
//	}
}
