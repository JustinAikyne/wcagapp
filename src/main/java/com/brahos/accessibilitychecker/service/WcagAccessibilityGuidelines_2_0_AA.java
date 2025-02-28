package com.brahos.accessibilitychecker.service;

import org.jsoup.nodes.Document;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

public interface WcagAccessibilityGuidelines_2_0_AA {

	public GuidelineResponse captionsLive(Document doc);

	public GuidelineResponse audioDescriptionPrerecorded(Document doc);

	public GuidelineResponse evaluateTextContrast(Document doc, String url);

	public GuidelineResponse resizeText(Document doc, String url);

	public GuidelineResponse checkImagesOfText(Document doc, String url);

	public GuidelineResponse checkMultipleWays(Document doc, String url);

	public GuidelineResponse validateHeadingsAndLabels(Document doc, String url);

	public GuidelineResponse validateFocusVisible(Document doc, String url);

	public GuidelineResponse validateLanguageOfParts(Document doc, String url);

	public GuidelineResponse validateConsistentNavigation(Document doc, String url);

	public GuidelineResponse validateConsistentIdentification(Document doc, String url);

	GuidelineResponse validateErrorSuggestion(Document doc, String url);
	
	public GuidelineResponse validateErrorPreventionForCriticalActions(Document doc, String url);

	

}
