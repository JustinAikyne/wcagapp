package com.brahos.accessibilitychecker.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.brahos.accessibilitychecker.model.ScanDataRequest;

public interface AccessibilityCheckerRepository extends MongoRepository<ScanDataRequest, String> {

//	ScanDataRequest findByScanId(String scanId);

	ScanDataRequest findByScanId(ObjectId scanId);
}
