package com.brahos.accessibilitychecker.service;

import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

public interface WcagAccessibilityGuidelines_2_0_AAA {

	public GuidelineResponse signLanguagePrerecorded(Document doc);

	public GuidelineResponse extendedAudioDescriptionPrerecorded(Document doc);

	public GuidelineResponse mediaAlternativePrerecorded(Document doc);

	public GuidelineResponse audioOnlyLive(Document doc);

	public GuidelineResponse validateContrastEnhancedLevelAAA(Document doc, String url);

	public GuidelineResponse validateLowOrNoBackgroundAudio(Document doc, String url);

	public GuidelineResponse validateVisualPresentation(Document doc, String url);

	public	GuidelineResponse validateKeyboardAccessibilityNoException(Document doc, String url);

	public GuidelineResponse validateNoTiming(Document doc, String url);

	public GuidelineResponse validateInterruptions(Document doc, String url);

	public GuidelineResponse validateReauthentication(Document doc, String url);

	public	GuidelineResponse validateFlashingContentAAA(Document doc, String url);

	public GuidelineResponse validateLocation(Document doc, String url);

	public GuidelineResponse validateLinkPurpose(Document doc, String url);

	public GuidelineResponse validateSectionHeadings(Document doc, String url);

	public GuidelineResponse validateUnusualWords(Document doc, String url);

	public GuidelineResponse validateAbbreviations(Document doc, String url);

	public GuidelineResponse validateReadingLevel(Document doc, String url);

	public GuidelineResponse validatePronunciation(Document doc, String url);

	public GuidelineResponse validateChangeOnRequest(Document doc, String url);

	public GuidelineResponse validateContextSensitiveHelp(Document doc, String url);

	public GuidelineResponse validateAllErrorPrevention(Document doc, String url);

	GuidelineResponse evaluateImagesOfText(Document doc, String url);



}
