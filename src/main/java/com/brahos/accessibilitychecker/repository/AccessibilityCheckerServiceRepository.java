package com.brahos.accessibilitychecker.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.brahos.accessibilitychecker.model.GuidelineResponse;


public interface AccessibilityCheckerServiceRepository extends MongoRepository<GuidelineResponse, String> {
	
	

}
