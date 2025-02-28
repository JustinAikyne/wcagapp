package com.brahos.accessibilitychecker_enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WcagGuidelineA {
	NON_TEXT_CONTENT("1.1.1 Non-text Content"),
	AUDIO_ONLY_VIDEO_ONLY("1.2.1 Audio-only and Video-only (Prerecorded)"),
	CAPTIONS("1.2.2 Captions (Prerecorded)"),
	AUDIO_DESCRIPTION("1.2.3 Audio Description or Media Alternative (Prerecorded)"),
	INFORMATION_RELATIONSHIPS("1.3.1 Information and Relationships"), 
	MEANINGFUL_SEQUENCE("1.3.2 Meaningful Sequence"),
	SENSORY_CHARACTERISTICS("1.3.3 Sensory Characteristics"), 
	USE_OF_COLOR("1.4.1 Use of Color"),
	AUDIO_CONTROL("1.4.2 Audio Control"),
	IMAGES_OF_TEXT("1.4.8 Images of Text (No Exception)"),
	KEYBOARD("2.1.1 Keyboard (Level A)"), 
	NO_KEYBOARD_TRAP("2.1.2 No Keyboard Trap (Level A)"),
	TIMING_ADJUSTABLE("2.2.1 Timing Adjustable (Level A)"),
	PAUSE_STOP_HIDE("2.2.2 Pause, Stop, Hide (Level A)"),
	THREE_FLASHES("2.3.1 Three Flashes or Below Threshold (Level A)"),
	BYPASS_BLOCKS("2.4.1 Bypass Blocks (Level A)"),
	PAGE_TITLED("2.4.2 Page Titled"), 
	FOCUS_ORDER("2.4.3 Focus Order"),
	LINK_PURPOSE("2.4.4 Link Purpose (In Context)"),
	LANGUAGE_OF_PAGE("3.1.1 Language of Page"),
	ON_FOCUS("3.2.1 On Focus"), 
	ON_INPUT("3.2.2 On Input"),
	ERROR_IDENTIFICATION("3.3.1 Error Identification"),
	LABELS_OR_INSTRUCTIONS("3.3.2 Labels or Instructions"),
	NAME_ROLE_VALUE("4.1.2 Name, Role, Value"), 
	PARSING("4.1.1 Parsing (Obsolete and Removed)");

	private final String description;
}
