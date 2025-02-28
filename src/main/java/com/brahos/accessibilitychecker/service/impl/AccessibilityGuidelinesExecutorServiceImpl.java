package com.brahos.accessibilitychecker.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.nodes.Document;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.exception.AccessibilityServiceException;
import com.brahos.accessibilitychecker.model.GuidelineHandler;
import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.brahos.accessibilitychecker.service.GuidelineExecutorService;
import com.brahos.accessibilitychecker.service.GuidelineResponseBuilderService;
import com.brahos.accessibilitychecker.service.WcagAccessibilityGuidelines_2_0_AA;
import com.brahos.accessibilitychecker.service.WcagAccessibilityGuidelines_2_0_AAA;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.qos.logback.classic.Logger;

@Service
public class AccessibilityGuidelinesExecutorServiceImpl implements GuidelineExecutorService {

	private final ExecutorService executorService = Executors.newFixedThreadPool(10);
	private static final Logger logger = (Logger) LoggerFactory
			.getLogger(AccessibilityGuidelinesExecutorServiceImpl.class);

	private final AccessibilityGuidelinesServiceImpl_2_0_A accessibilityGuidelinesServiceImplA;
	private final WcagAccessibilityGuidelines_2_0_AA wcagAccessibilityGuidelinesAA;
	private final WcagAccessibilityGuidelines_2_0_AAA wcagAccessibilityGuidelinesAAA;
	private final GuidelineResponseBuilderService guidelineResponseBuilderService;

	public AccessibilityGuidelinesExecutorServiceImpl(
			AccessibilityGuidelinesServiceImpl_2_0_A accessibilityGuidelinesServiceImplA,
			WcagAccessibilityGuidelines_2_0_AA wcagAccessibilityGuidelinesAA,
			WcagAccessibilityGuidelines_2_0_AAA wcagAccessibilityGuidelinesAAA,
			GuidelineResponseBuilderService guidelineResponseBuilderService) {
		this.accessibilityGuidelinesServiceImplA = accessibilityGuidelinesServiceImplA;
		this.wcagAccessibilityGuidelinesAA = wcagAccessibilityGuidelinesAA;
		this.wcagAccessibilityGuidelinesAAA = wcagAccessibilityGuidelinesAAA;
		this.guidelineResponseBuilderService = guidelineResponseBuilderService;
	}

	private List<GuidelineHandler> getGuidelineHandlersForLevelA(Document document, String url) {
		return List.of(
				new GuidelineHandler("Non-text content",
						(doc, u) -> accessibilityGuidelinesServiceImplA.evaluateNonTextContent(doc), false),

				new GuidelineHandler("Audio-video content",
						(doc, u) -> accessibilityGuidelinesServiceImplA.audioVideoOnlyContent(doc), false),

				new GuidelineHandler("Captions Prerecorded",
						(doc, u) -> accessibilityGuidelinesServiceImplA.captionsPrerecorded(doc), false),

				new GuidelineHandler("Audio Description or Media Alternative",
						(doc, u) -> accessibilityGuidelinesServiceImplA.audioDescriptionOrMediaAlternative(doc), false),

				new GuidelineHandler("Info and Relationships",
						(doc, u) -> accessibilityGuidelinesServiceImplA.infoAndRelationships(doc, u), true),

				new GuidelineHandler("Meaningful Sequence",
						(doc, u) -> accessibilityGuidelinesServiceImplA.meaningfulSequence(doc), false),

				new GuidelineHandler("Sensory Characteristics",
						(doc, u) -> accessibilityGuidelinesServiceImplA.sensoryCharacteristics(doc), false),

				new GuidelineHandler("Use of Color", (doc, u) -> accessibilityGuidelinesServiceImplA.useOfColor(doc),
						false),

				new GuidelineHandler("Evaluate Audio Control",
						(doc, u) -> accessibilityGuidelinesServiceImplA.evaluateAudioControl(doc), false),

				new GuidelineHandler("validateKeyboardAccessibility",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateKeyboardAccessibility(doc, u), true),

				new GuidelineHandler("validateNoKeyboardTrap",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateNoKeyboardTrap(doc, u), true),

				new GuidelineHandler("validateTimingAdjustable",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateTimingAdjustable(doc, u), true),

				new GuidelineHandler("validatePauseStopHide",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validatePauseStopHide(doc, u), true),

				new GuidelineHandler("validateFlashingContent",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateFlashingContent(doc, u), true),

				new GuidelineHandler("validateBypassBlocks",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateBypassBlocks(doc, u), true),

				new GuidelineHandler("evaluatePageTitle",
						(doc, u) -> accessibilityGuidelinesServiceImplA.evaluatePageTitle(doc, u), true),

				new GuidelineHandler("evaluateFocusOrder",
						(doc, u) -> accessibilityGuidelinesServiceImplA.evaluateFocusOrder(doc, u), true),

				new GuidelineHandler("evaluateLinkPurpose",
						(doc, u) -> accessibilityGuidelinesServiceImplA.evaluateLinkPurpose(doc, u), true),

				new GuidelineHandler("validateLanguageOfPage",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateLanguageOfPage(doc, u), true),

				new GuidelineHandler("validateOnFocus",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateOnFocus(document, url), true),

				new GuidelineHandler("validateOnInput",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateOnInput(document, url), true),

				new GuidelineHandler("validateErrorIdentification",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateErrorIdentification(document, url),
						true),

				new GuidelineHandler("validateLabelsOrInstructions",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateLabelsOrInstructions(document, url),
						true),

				new GuidelineHandler("validateParsing",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateParsing(document, url), true),

				new GuidelineHandler("validateNameRoleValue",
						(doc, u) -> accessibilityGuidelinesServiceImplA.validateNameRoleValue(document, url), true)

		);
	}

	private List<GuidelineHandler> getGuidelineHandlersForLevelAA(Document document, String url) {
		return List.of(
				new GuidelineHandler("Captions Live", (doc, u) -> wcagAccessibilityGuidelinesAA.captionsLive(doc),
						false),
				new GuidelineHandler("Audio Description Prerecorded",
						(doc, u) -> wcagAccessibilityGuidelinesAA.audioDescriptionPrerecorded(doc), false),
				new GuidelineHandler("evaluateTextContrast",
						(doc, u) -> wcagAccessibilityGuidelinesAA.evaluateTextContrast(doc, u), true),
				new GuidelineHandler("Resize Text", (doc, u) -> wcagAccessibilityGuidelinesAA.resizeText(doc, u), true),
				new GuidelineHandler("checkImagesOfText",
						(doc, u) -> wcagAccessibilityGuidelinesAA.checkImagesOfText(doc, u), true),
				new GuidelineHandler("checkMultipleWays",
						(doc, u) -> wcagAccessibilityGuidelinesAA.checkMultipleWays(doc, u), true),
				new GuidelineHandler("validateHeadingsAndLabels",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateHeadingsAndLabels(doc, u), true),
				new GuidelineHandler("validateFocusVisible",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateFocusVisible(doc, u), true),
				new GuidelineHandler("validateLanguageOfParts",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateLanguageOfParts(doc, u), true),
				new GuidelineHandler("validateConsistentNavigation",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateConsistentNavigation(doc, u), true),
				new GuidelineHandler("validateConsistentIdentification",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateConsistentIdentification(doc, u), true),
				new GuidelineHandler("validateErrorSuggestion",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateErrorSuggestion(doc, u), true),
				new GuidelineHandler("validateErrorPreventionForCriticalActions",
						(doc, u) -> wcagAccessibilityGuidelinesAA.validateErrorPreventionForCriticalActions(doc, u),
						true)

		);
	}

	private List<GuidelineHandler> getGuidelineHandlersForLevelAAA(Document document, String url) {

		logger.info("inside the getGuidelineHandlersForLevelAAA");
		return List.of(
				new GuidelineHandler("Sign Language Prerecorded",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.signLanguagePrerecorded(doc), false),

				new GuidelineHandler("Extended Audio Description Prerecorded",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.extendedAudioDescriptionPrerecorded(doc), false),

				new GuidelineHandler("Media Alternative Prerecorded",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.mediaAlternativePrerecorded(doc), false),

				new GuidelineHandler("Audio Only Live", (doc, u) -> wcagAccessibilityGuidelinesAAA.audioOnlyLive(doc),
						false),

				new GuidelineHandler("validateContrastEnhancedLevelAAA",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateContrastEnhancedLevelAAA(doc, u), true),

				new GuidelineHandler("validateLowOrNoBackgroundAudio",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateLowOrNoBackgroundAudio(doc, u), true),

				new GuidelineHandler("validateVisualPresentation",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateVisualPresentation(doc, u), true),

				new GuidelineHandler("Images of Text (No Exception)",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.evaluateImagesOfText(doc, u), true),

				new GuidelineHandler("validateKeyboardAccessibilityNoException",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateKeyboardAccessibilityNoException(document,
								url),
						true),

				new GuidelineHandler("validateNoTiming",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateNoTiming(document, url), true),

				new GuidelineHandler("validateInterruptions",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateInterruptions(document, url), true),

				new GuidelineHandler("validateReauthentication",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateReauthentication(document, url), true),

				new GuidelineHandler("validateFlashingContentAAA",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateFlashingContentAAA(document, url), true),

				new GuidelineHandler("validateLocation",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateLocation(document, url), true),

				new GuidelineHandler("validateLinkPurpose",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateLinkPurpose(document, url), true),

				new GuidelineHandler("validateSect	ionHeadings",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateSectionHeadings(document, url), true),

				new GuidelineHandler("validateUnusualWords",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateUnusualWords(document, url), true),

				new GuidelineHandler("validateAbbreviations",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateAbbreviations(document, url), true),

				new GuidelineHandler("validateReadingLevel",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateReadingLevel(document, url), true),

				new GuidelineHandler("validatePronunciation",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validatePronunciation(document, url), true),

				new GuidelineHandler("validateChangeOnRequest",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateChangeOnRequest(document, url), true),

				new GuidelineHandler("validateContextSensitiveHelp",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateContextSensitiveHelp(doc, u), true),

				new GuidelineHandler("validateAllErrorPrevention",
						(doc, u) -> wcagAccessibilityGuidelinesAAA.validateAllErrorPrevention(doc, u), true)

		);
	}

	private List<GuidelineResponse> executeGuidelineTasks(List<GuidelineHandler> guidelineHandlers, Document document,
			String url) throws AccessibilityServiceException {

		List<GuidelineResponse> guidelineResponses = new ArrayList<>();
		List<Future<GuidelineResponse>> futures = new ArrayList<>();

		try {
			for (GuidelineHandler handler : guidelineHandlers) {
				if (!handler.isRequiresUrl() || (handler.isRequiresUrl() && url != null)) {
					Future<GuidelineResponse> future = executorService.submit(() -> handler.process(document, url));
					futures.add(future);
					logger.info("{} evaluation started.", handler.getName());
				}
			}

//			
//			for (GuidelineHandler handler : guidelineHandlers) {
//			    // Check if the handler requires a URL or not
//			    if (!handler.isRequiresUrl() || (handler.isRequiresUrl() && url != null)) {
//			        try {
//			            // Process the document and URL directly (synchronously)
//			            GuidelineResponse response = handler.process(document, url);
//
//			            // Add the response to a list if needed
//			            guidelineResponses.add(response);
//
//			            logger.info("{} evaluation completed.", handler.getName());
//			        } catch (Exception e) {
//			            // Handle exceptions gracefully and log the error
//			            logger.error("Error while processing handler {}: {}", handler.getName(), e.getMessage(), e);
//			        }
//			    }
//			}

			for (Future<GuidelineResponse> future : futures) {
				try {
					GuidelineResponse response = future.get(); // This blocks until the task completes
					guidelineResponses.add(response);
				} catch (Exception e) {
					logger.error("Error while executing guideline.", e);
					throw new AccessibilityServiceException("Error occurred while executing accessibility guidelines.",
							e);
				}
			}
		} catch (Exception e) {
			logger.error("Error during guideline execution", e);
			throw new AccessibilityServiceException("Error occurred while executing accessibility guidelines.", e);
		}

		return guidelineResponses;
	}

	@Override
	public GuidelineResponse executeGuidelinesA(Document document, String url) throws JsonProcessingException {
		if (document == null) {
			throw new AccessibilityServiceException("Input document cannot be null.");
		}

		logger.info("Starting level A guideline execution...");
		List<GuidelineHandler> guidelineHandlers = getGuidelineHandlersForLevelA(document, url);
		List<GuidelineResponse> guidelineResponses = executeGuidelineTasks(guidelineHandlers, document, url);

		return guidelineResponseBuilderService.aggregateResponses(guidelineResponses);
	}

	@Override
	public GuidelineResponse executeGuidelinesAA(Document document, String url) throws JsonProcessingException {
		if (document == null) {
			throw new AccessibilityServiceException("Input document cannot be null.");
		}

		logger.info("Starting level AA guideline execution...");
		List<GuidelineHandler> guidelineHandlers = getGuidelineHandlersForLevelAA(document, url);
		List<GuidelineResponse> guidelineResponses = executeGuidelineTasks(guidelineHandlers, document, url);

		return guidelineResponseBuilderService.aggregateResponses(guidelineResponses);
	}

	@Override
	public GuidelineResponse executeGuidelinesAAA(Document document, String url) throws JsonProcessingException {
		if (document == null) {
			throw new AccessibilityServiceException("Input document cannot be null.");
		}

		logger.info("Starting level AAA guideline execution...");
		List<GuidelineHandler> guidelineHandlers = getGuidelineHandlersForLevelAAA(document, url);
		List<GuidelineResponse> guidelineResponses = executeGuidelineTasks(guidelineHandlers, document, url);

		return guidelineResponseBuilderService.aggregateResponses(guidelineResponses);
	}
}
