package com.brahos.accessibilitychecker.model;

import java.util.function.BiFunction;
import org.jsoup.nodes.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuidelineHandler {
	private final String name;
	private final BiFunction<Document, String, GuidelineResponse> processor;
	private final boolean requiresUrl;

	public GuidelineResponse process(Document document, String url) {
		return processor.apply(document, url);
	}
}
