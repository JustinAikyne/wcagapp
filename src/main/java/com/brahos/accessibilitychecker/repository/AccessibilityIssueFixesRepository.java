package com.brahos.accessibilitychecker.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.brahos.accessibilitychecker.model.WcagFixesData;

public interface AccessibilityIssueFixesRepository extends MongoRepository<WcagFixesData, ObjectId> {

	Optional<WcagFixesData> findByWcagVersion(String wcagVersion);

	Optional<WcagFixesData> findByWcagVersionAndLevel(String wcagVersion, String level);

}
