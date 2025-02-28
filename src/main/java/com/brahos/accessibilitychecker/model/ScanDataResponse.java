package com.brahos.accessibilitychecker.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class ScanDataResponse {

	private String version;
	private String level;
	private List<String> urls;

}
