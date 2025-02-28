package com.brahos.accessibilitychecker.service;

import com.brahos.accessibilitychecker.model.GuidelineResponse;

@FunctionalInterface
public interface AllWCAGGuidelineExecutor {

	GuidelineResponse run();
}
