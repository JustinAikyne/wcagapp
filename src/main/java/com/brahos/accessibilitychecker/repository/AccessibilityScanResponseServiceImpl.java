package com.brahos.accessibilitychecker.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

@Service
public class AccessibilityScanResponseServiceImpl implements AccessibilityScanResponseService {

	@Autowired
	AccessibilityCheckerScanResponseRepository scanResponseRepository;

	@Override
	public Optional<GuidelineResponse> getScanResponeById(ObjectId id) {
		return scanResponseRepository.findById(id);

	}

	public List<GuidelineResponse> getByScanId(String scanId) {
		return scanResponseRepository.findByScanId(scanId);
	}

	public List<GuidelineResponse> getByPageUrl(String pageUrl) {
		return scanResponseRepository.findByPageUrl(pageUrl);
	}

	public List<GuidelineResponse> getByScannedTime(String scannedTime) {
		return scanResponseRepository.findByScanedTime(scannedTime);

	}


}
