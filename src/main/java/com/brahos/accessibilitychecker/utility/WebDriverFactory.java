package com.brahos.accessibilitychecker.utility;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebDriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(WebDriverFactory.class);
    private static WebDriver driver;
    private static final Object lock = new Object();
    private static final int MAX_RETRIES = 3;

    private WebDriverFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns a singleton WebDriver instance, creating one if not initialized.
     */
    public static WebDriver getDriver() {
        if (driver == null) {
            synchronized (lock) {
                if (driver == null) {
                    driver = createWebDriverWithRetries();
                }
            }
        }
        return driver;
    }

    
    public static WebDriver getDriver(String url) {
		if (driver == null) {
			synchronized (lock) {
				if (driver == null) {
					driver = createWebDriverWithRetries();
				}
			}
		}
		driver.get(url);
		return driver;
	}
    
    /**
     * Attempts to create a WebDriver instance with retries on failure.
     */
    private static WebDriver createWebDriverWithRetries() {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                logger.info("Initializing WebDriver (attempt {}/{})...", attempt + 1, MAX_RETRIES);
                return createWebDriver();
            } catch (WebDriverException e) {
                logger.error("WebDriver initialization failed (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
                attempt++;
                try {
                    TimeUnit.SECONDS.sleep(2 * attempt);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new RuntimeException("Failed to initialize WebDriver after " + MAX_RETRIES + " attempts.");
    }

    /**
     * Creates and configures a WebDriver instance.
     */
    public static WebDriver createWebDriver() {
        String gridUrl = System.getenv("GRID_URL");
        WebDriver driverInstance;
        try {
            logger.info("Initializing WebDriver...");
            
            // Determine ChromeDriver Path
            String driverPath = "src/main/resources/driver/chromedriver";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                driverPath += ".exe";
            }
            File driverFile = new File(driverPath);
            if (!driverFile.exists()) {
                throw new RuntimeException("ChromeDriver not found at: " + driverFile.getAbsolutePath());
            }
            System.setProperty("webdriver.chrome.driver", driverFile.getAbsolutePath());
            logger.info("Using ChromeDriver at: {}", driverFile.getAbsolutePath());
            
            // Determine Chrome Binary Path
            String chromeBinaryPath = "src/main/resources/driver/chrome-headless-shell.exe";
            File chromeFile = new File(chromeBinaryPath);
            if (!chromeFile.exists()) {
                throw new RuntimeException("Chrome binary not found at: " + chromeFile.getAbsolutePath());
            }
            
            // Configure ChromeOptions
            ChromeOptions options = new ChromeOptions();
            options.setBinary(chromeFile.getAbsolutePath());
            options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
            
            // Use Remote WebDriver if GRID_URL is specified
            if (gridUrl != null && !gridUrl.isEmpty()) {
                try {
                    logger.info("Using Remote WebDriver at {}", gridUrl);
                    driverInstance = new RemoteWebDriver(new URL(gridUrl), options);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Invalid GRID_URL: " + gridUrl, e);
                }
            } else {
                // Use Local ChromeDriver
                logger.info("Using Local ChromeDriver.");
                driverInstance = new ChromeDriver(options);
            }
            
            // Configure timeouts
            driverInstance.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driverInstance.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driverInstance.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            
            // Maximize window
            driverInstance.manage().window().maximize();
            logger.info("WebDriver created successfully.");
            
        } catch (Exception e) {
            logger.error("Error initializing WebDriver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create WebDriver instance.", e);
        }
        return driverInstance;
    }

    /**
     * Quits the WebDriver instance if it exists.
     */
    public static void quitDriver() {
        if (driver != null) {
            synchronized (lock) {
                if (driver != null) {
                    try {
                        driver.quit();
                        logger.info("WebDriver instance closed successfully.");
                    } catch (WebDriverException e) {
                        logger.error("Error while quitting WebDriver: {}", e.getMessage(), e);
                    } finally {
                        driver = null;
                    }
                }
            }
        }
    }
}
