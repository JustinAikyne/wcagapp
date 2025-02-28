package com.brahos.accessibilitychecker.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScanedPageDetails {

	private String id;
	
	private String pageUrl; 
	
	
	private String scanId; 

	private LocalDateTime scannedTime;

}
