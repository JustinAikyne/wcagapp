package com.brahos.accessibilitychecker.service;

import org.jsoup.nodes.Document;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

public interface WcagAccessibilityGuidelines_2_0_A {

	public GuidelineResponse evaluateNonTextContent(Document doc);

	public GuidelineResponse audioVideoOnlyContent(Document document);

	public GuidelineResponse captionsPrerecorded(Document document);

	public GuidelineResponse audioDescriptionOrMediaAlternative(Document doc);

	public GuidelineResponse useOfColor(Document doc);

	public GuidelineResponse evaluateAudioControl(Document doc);

	public GuidelineResponse infoAndRelationships(Document doc,String url);

	public GuidelineResponse meaningfulSequence(Document doc);

	public GuidelineResponse sensoryCharacteristics(Document doc);

	public GuidelineResponse validateKeyboardAccessibility(Document doc, String url);

	GuidelineResponse validateNoKeyboardTrap(Document doc, String url);

	GuidelineResponse validateTimingAdjustable(Document doc, String url);

	GuidelineResponse validatePauseStopHide(Document doc, String url);

	GuidelineResponse validateFlashingContent(Document doc, String url);

	GuidelineResponse evaluatePageTitle(Document doc, String url);

	GuidelineResponse validateBypassBlocks(Document doc, String url);

	GuidelineResponse evaluateFocusOrder(Document doc, String url);

	GuidelineResponse evaluateLinkPurpose(Document doc, String url);

	public GuidelineResponse validateLanguageOfPage(Document doc, String url);

	public GuidelineResponse validateOnFocus(Document doc, String url);

	public GuidelineResponse validateOnInput(Document doc, String url);

	public GuidelineResponse validateErrorIdentification(Document doc, String url);
	
	public GuidelineResponse validateLabelsOrInstructions(Document doc, String url);
	
	public GuidelineResponse validateNameRoleValue(Document doc, String url);

	GuidelineResponse validateParsing(Document doc, String url);
	
	
	

}
