package com.brahos.accessibilitychecker.helper.service;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.brahos.accessibilitychecker.model.GuidelineData;
import com.brahos.accessibilitychecker.model.IssueDetails;

public interface IGuidelineDataTransformer {

	public void addIssueUsingJsoup(List<IssueDetails> issueList, Element img, String tempImg, String issueDescription,
			String level);

	// ------------------

	public void addIssueUsingJsoupForCss(List<IssueDetails> issueList, Element img, String tempImg, String issueDescription,
			String level);

	// ------------------

	public void addIssue(List<IssueDetails> issueList, WebElement webElement, String tempImg, String issueDescription,
			String level);

	public void addIssueSelenium(List<IssueDetails> issueList, List<WebElement> landmarks, String tempImg,
			String issueDescription, String level, WebElement webElement, WebDriver driver);

	GuidelineData transform(String guideline, String level, String wcagVersion, List<IssueDetails> issueList,
			int issueCount, int successCount);

//	GuidelineData transform(String guideline, String level, String wcagVersion, List<IssueDetails> issueList,
//			int guidelineIssueCount, int guidelineSuccessCount, int totalIssueCount, int totalSuccessCount);
//	

//	public void addIssue(List<IssueDetails> issueList, WebElement link, String linkText, String string, String string2,
//			String level);

	public GuidelineData transformDataOfIssueNull(String guideline, String level, String wcagVersion,
			List<IssueDetails> issueList, int guidelineIssueCount, int guidelineSuccessCount, int totalIssueCount,
			int totalSuccessCount);

	public void addIssue(List<IssueDetails> issueList, Element field, String string, String issueDescription,
			String level);

}
