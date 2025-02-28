package com.brahos.accessibilitychecker.service;

import org.jsoup.nodes.Document;

import com.brahos.accessibilitychecker.model.GuidelineResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Interface for executing accessibility guidelines.
 */
public interface GuidelineExecutorService {
	/**
	 * Executes accessibility checks on the provided HTML document.
	 *
	 * @param document The Jsoup Document object representing the HTML to analyze.
	 * @return A GuidelineResponse object with the results of the accessibility
	 *         check.
	 * @throws JsonProcessingException If an error occurs during JSON processing.
	 */

	GuidelineResponse executeGuidelinesA(Document document, String url) throws JsonProcessingException;

	GuidelineResponse executeGuidelinesAA(Document document, String url) throws JsonProcessingException;

	GuidelineResponse executeGuidelinesAAA(Document document, String url) throws JsonProcessingException;


}
