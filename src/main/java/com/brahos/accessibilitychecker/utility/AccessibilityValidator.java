package com.brahos.accessibilitychecker.utility;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
public class AccessibilityValidator {

	private static final Logger logger = LoggerFactory.getLogger(AccessibilityValidator.class);

	@Autowired
	private WebDriverFactory webDriverFactory;

	private WebDriver driver = null;

	/**
	 * Initializes WebDriver once and reuses it for subsequent method calls.
	 */
	public WebDriver initDriver(String url) {
		try {


			if (driver == null) {
				logger.info("Initializing WebDriver for the first time.");
			//	driver = webDriverFactory.getDriver();
				driver.get(url);
			} else {
				logger.info("Reusing existing WebDriver instance.");
			}
		} catch (Exception e) {
			logger.error("Error initializing WebDriver: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to initialize WebDriver.", e);
		}
		return driver;
	}

	/**
	 * Fetches global Selenium data using WebDriver.
	 * 
	 * @param url The URL of the webpage to scrape.
	 * @return List of WebElements matching the specified CSS selector.
	 */
	public List<WebElement> getGlobalSeleniumData(String url) {
		try {
			WebDriver driver = initDriver(url);
			return driver.findElements(By.cssSelector(
					"p, h1, h2, h3, h4, h5, h6, span, section, div, a, li, button, input, textarea, audio, select, "
							+ "[tabindex], [role='button'], [role='link'], [role='menuitem'], [data-time-limit], "
							+ "[class*='countdown'], [id*='timer'], [class*='timer'], [role='timer'], meta[http-equiv='refresh']"));
		} catch (Exception e) {
			logger.error("Error fetching Selenium data: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Closes the WebDriver instance and releases resources.
	 */
	public void closeDriver() {

		if (driver != null) {
			for (String handle : driver.getWindowHandles()) {
	            driver.switchTo().window(handle);
	            driver.close();
	        }
			driver.quit();
			driver = null;
			logger.info("WebDriver instance closed successfully.");
		}
	}
	
}
