package com.brahos.accessibilitychecker.service;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.brahos.accessibilitychecker.model.IssueDetails;

public interface HelperGuidelinesExecutorService {

	boolean isPlaceholderText(String altText);

	boolean isAltTextSufficient(String altText, Element img);

	boolean isFunctionalOrInformative(Element img);

	boolean hasCaptions(Element media);

	boolean hasTitle(Element media);

	boolean isPrerecorded(Element element);

	public boolean hasTextAlternative(Element element);

	public boolean hasAudioTrack(Element element);

	public int analyzeCaptionsForVideos(Elements videoElements, String mediaType, String level,
			List<IssueDetails> issueList, String fix);

}
