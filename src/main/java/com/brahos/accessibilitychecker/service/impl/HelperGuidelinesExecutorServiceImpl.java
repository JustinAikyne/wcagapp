package com.brahos.accessibilitychecker.service.impl;

import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.brahos.accessibilitychecker.model.IssueDetails;
import com.brahos.accessibilitychecker.service.HelperGuidelinesExecutorService;

@Component
public class HelperGuidelinesExecutorServiceImpl implements HelperGuidelinesExecutorService {

	// Checks if the provided alt text is a placeholder (e.g., "image" or "graphic")
	@Override
	public boolean isPlaceholderText(String altText) {
		String[] placeholders = { "image", "photo", "picture", "graphic", "example image", "image description" };
		return Arrays.asList(placeholders).contains(altText.toLowerCase());
	}

	// Checks if the alt text is sufficient by ensuring it meets a minimum length
	@Override
	public boolean isAltTextSufficient(String altText, Element img) {
		int minLength = 3; // Define minimum length for sufficient alt text
		return altText.length() >= minLength;
	}

	// Checks if the image is either functional (part of a link/button) or
	// informative (has src and is not hidden)
	@Override
	public boolean isFunctionalOrInformative(Element img) {
		boolean isInteractive = img.parent().is("a") || img.parent().is("button") || img.hasAttr("onclick");
		boolean isInformative = img.hasAttr("src") && !img.hasAttr("aria-hidden");
		return isInteractive || isInformative;
	}

	// Checks if the media element (e.g., video or audio) has captions by looking
	// for track elements with kind='subtitles' or 'captions'
	public boolean hasCaptions(Element media) {
		// Selects the 'track' elements with 'kind' attribute set to 'subtitles' or
		// 'captions'
		return media.select("track[kind='subtitles'], track[kind='captions']").size() > 0;
	}

	// Checks if the media element has a title attribute
	@Override
	public boolean hasTitle(Element media) {
		return media.hasAttr("title");
	}

	private void checkOtherNonTextContent(Document doc, List<String> issues) {
		Elements mediaElements = doc.select("video, audio");
		for (Element media : mediaElements) {
			if (!media.hasAttr("title") || !hasCaptions(media)) {
				issues.add("WCAG 1.1.1: Media missing captions or title: " + media.outerHtml());
			}
		}
	}

	public boolean isPrerecorded(Element element) {
		// Example check: based on a custom attribute or other logic
		return element.hasAttr("src");
	}

	public boolean hasTextAlternative(Element element) {
		// Check if there is a text alternative like captions, alt text, etc.
		return element.attr("alt").length() > 0 || element.attr("title").length() > 0;
	}

	public boolean hasAudioTrack(Element element) {
		// Check if the video element has a track element for subtitles or captions
		return element.select("track").size() > 0;
	}

	// Helper method to check for captions in videos
	public int analyzeCaptionsForVideos(Elements videoElements, String mediaType, String level,
	                                    List<IssueDetails> issueList, String fix) {
	    int localIssueCount = 0;

	    for (Element video : videoElements) {
	        try {
	            // Check if the video element has a <track> element for captions
	            Elements trackElements = video.select("track[kind=captions]");

	            if (trackElements.isEmpty()) {
	                // No captions provided for this video, add an issue
	                issueList.add(new IssueDetails(video.outerHtml(), mediaType, "Captions are missing", level, fix));
	                localIssueCount++;
	            }

	        } catch (Exception e) {}
	    }

	    return localIssueCount;
	}

}
