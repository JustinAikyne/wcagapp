package com.brahos.accessibilitychecker.repository;

import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.model.ScanDataResponse;

@Service
public interface AccessibilityCheckerService {
	/**
	 * Retrieve all URLs (pageUrl and allPageUrls) for a given scanId.
	 *
	 * @param scanId The ID of the scan document in MongoDB.
	 * @return A list of URLs.
	 * @throws IllegalArgumentException if scanId is invalid or not found.
	 */

	ScanDataResponse getAllDetailsByScanId(String scanId);

}
