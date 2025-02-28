package com.brahos.accessibilitychecker.service;

import java.io.IOException;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This class provides methods for parsing HTML content from a given URL and
 * handling retries.
 */
@Service
public class HtmlParser {

	private static final Logger logger = LoggerFactory.getLogger(HtmlParser.class);
	private static final int DEFAULT_TIMEOUT = 10_000; // 10 seconds

	/**
	 * Parses the HTML content from the specified URL.
	 *
	 * @param url The URL of the web page to fetch.
	 * @return An Optional containing the parsed HTML document if successful, or an
	 *         empty Optional if an error occurs.
	 */
	public Optional<Document> parseUrl(String url) {
		return parseUrl(url, DEFAULT_TIMEOUT,
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");
	}

	/**
	 * Parses the HTML content from the specified URL with custom timeout and user
	 * agent.
	 *
	 * @param url       The URL of the web page to fetch.
	 * @param timeout   The timeout value in milliseconds.
	 * @param userAgent The user-agent string to use for the request.
	 * @return An Optional containing the parsed HTML document if successful, or an
	 *         empty Optional if an error occurs.
	 */
	public Optional<Document> parseUrl(String url, int timeout, String userAgent) {
		try {
			logger.info("Fetching HTML from URL: {}", url);

			Document document = Jsoup.connect(url).timeout(timeout).userAgent(userAgent).get();

			if (document == null || document.html().isEmpty()) {
				logger.error("Received empty HTML content from URL: {}", url);
				return Optional.empty();
			}

			logger.info("Successfully fetched HTML from URL: {}", url);
			return Optional.of(document);

		} catch (IOException e) {
			logger.error("Error fetching HTML from URL: {} - {}", url, e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Utility method for handling retries in case of network failures.
	 *
	 * @param url     The URL to fetch.
	 * @param retries Number of retries to attempt in case of failure.
	 * @param delay   Delay in milliseconds between retries.
	 * @return Optional containing the Document if successful, otherwise empty.
	 */
	public Optional<Document> fetchWithRetry(String url, int retries, int delay) {
		int attempt = 0;
		Optional<Document> document = Optional.empty();
		while (attempt < retries && document.isEmpty()) {
			document = parseUrl(url);
			if (document.isEmpty()) {
				attempt++;
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					logger.error("Error during retry delay: {}", e.getMessage(), e);
				}
			}
		}
		return document;
	}

	/**
	 * Parses the raw HTML content passed as a string and returns a Jsoup Document.
	 *
	 * @param htmlContent Raw HTML content as a string.
	 * @return A Jsoup Document representing the HTML content.
	 */
	public Document parseHtmlContent(String htmlContent) {
		if (htmlContent == null || htmlContent.isEmpty()) {
			logger.error("Received empty HTML content for parsing.");
			return null;
		}
		try {
			return Jsoup.parse(htmlContent);
		} catch (Exception e) {
			logger.error("Error parsing HTML content: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Fetches the HTML content from the specified URL and returns it as a Jsoup
	 * Document.
	 *
	 * @param url The URL to fetch the HTML from.
	 * @return A Jsoup Document containing the parsed HTML content.
	 */
	public Document fetchHtmlFromUrl(String url) {
		Optional<Document> documentOpt = parseUrl(url);
		return documentOpt.orElse(null); // Return null if parsing failed
	}
}
