package com.brahos.accessibilitychecker.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

public interface AccessibilityCheckerScanResponseRepository extends MongoRepository<GuidelineResponse, ObjectId> {

	List<GuidelineResponse> findByScanId(String scanId);

	List<GuidelineResponse> findByPageUrl(String pageUrl);

	public abstract List<GuidelineResponse> findByScanedTime(String scanedTime);

	@Query("{'scanedTime': ?0, 'scanId': ?1}")
	List<GuidelineResponse> findBySomeParent_ScanedTime_ScanId(String scanedTime, String scanId);

}
