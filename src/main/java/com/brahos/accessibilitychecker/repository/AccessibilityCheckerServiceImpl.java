package com.brahos.accessibilitychecker.repository;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.model.ScanDataRequest;
import com.brahos.accessibilitychecker.model.ScanDataResponse;

@Service
public class AccessibilityCheckerServiceImpl implements AccessibilityCheckerService {

	private static final Logger logger = LoggerFactory.getLogger(AccessibilityCheckerServiceImpl.class);

	@Autowired
	private AccessibilityCheckerRepository repository;

	@Override
	public ScanDataResponse getAllDetailsByScanId(String scanId) {
		if (scanId == null || scanId.isEmpty()) {
			logger.error("Scan ID is mandatory.");
			throw new IllegalArgumentException("Scan ID is mandatory.");
		}

		ObjectId objectId;
		try {
			objectId = new ObjectId(scanId); // Convert String to ObjectId
		} catch (IllegalArgumentException e) {
			logger.error("Invalid Scan ID format: {}", scanId);
			throw new IllegalArgumentException("Invalid Scan ID format.");
		}

		ScanDataRequest details = repository.findByScanId(objectId); // Pass ObjectId to repository

		if (details == null) {
			logger.error("No data found for the provided Scan ID: {}", scanId);
			throw new IllegalArgumentException("No data found for the provided Scan ID.");
		}

		String version = details.getVersion();
		String level = details.getLevel();

		List<String> allUrls = new ArrayList<>();
		if (details.getPageUrl() != null) {
			allUrls.add(details.getPageUrl());
		}
		if (details.getAllPageUrls() != null) {
			allUrls.addAll(details.getAllPageUrls());
		}

		return new ScanDataResponse(version, level, allUrls);
	}

//	@Override
//	public ScanDataResponse getAllDetailsByScanId(String scanId) {
//
//		if (scanId == null) {
//			logger.error("Scan ID is mandatory.");
//			throw new IllegalArgumentException("Scan ID is mandatory.");
//		}
//
//		ScanDataRequest details = repository.findByScanId(scanId);
//
//		if (details == null) {
//			logger.error("No data found for the provided Scan ID: {}", scanId);
//			throw new IllegalArgumentException("No data found for the provided Scan ID.");
//		}
//
//		String version = details.getVersion();
//
//		String level = details.getLevel();
//
//		List<String> allUrls = new ArrayList<>();
//		if (details.getPageUrl() != null) {
//			allUrls.add(details.getPageUrl());
//		}
//		if (details.getAllPageUrls() != null) {
//			allUrls.addAll(details.getAllPageUrls());
//		}
//
//		return new ScanDataResponse(version, level, allUrls);
//	}
}
