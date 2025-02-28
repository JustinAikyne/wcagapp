
package com.brahos.accessibilitychecker.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

@Component
public interface AccessibilityScanResponseService {

	Optional<GuidelineResponse> getScanResponeById(ObjectId id);

	List<GuidelineResponse> getByScanId(String scanId);

	List<GuidelineResponse> getByPageUrl(String pageUrl);

	List<GuidelineResponse> getByScannedTime(String scannedTime);

}
