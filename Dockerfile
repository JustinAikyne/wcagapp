# Use the AWS Lambda base image for Java 21
FROM public.ecr.aws/lambda/java:21

# Install unzip using dnf (Amazon Linux 2023+)
RUN dnf install -y unzip

# Set working directory
WORKDIR /var/task

# Download and extract Chromium
RUN curl -SL https://storage.googleapis.com/chrome-for-testing-public/132.0.6834.159/linux64/chrome-headless-shell-linux64.zip \
    -o /tmp/chrome.zip && \
    unzip /tmp/chrome.zip -d /opt/bin/ && \
    chmod +x /opt/bin/chrome-headless-shell-linux64/chrome-headless-shell && \
    mv /opt/bin/chrome-headless-shell-linux64/chrome-headless-shell /opt/bin/chrome-headless-shell

# Download and extract ChromeDriver
RUN curl -SL https://storage.googleapis.com/chrome-for-testing-public/132.0.6834.159/linux64/chromedriver-linux64.zip \
    -o /tmp/chromedriver.zip && \
    unzip /tmp/chromedriver.zip -d /opt/bin/ && \
    chmod +x /opt/bin/chromedriver-linux64/chromedriver && \
    mv /opt/bin/chromedriver-linux64/chromedriver /opt/bin/chromedriver

# Set environment variables for Selenium
ENV CHROMIUM_PATH="/opt/bin/chrome-headless-shell"
ENV CHROMEDRIVER_PATH="/opt/bin/chromedriver"

# Copy application JAR file
COPY target/accessibilityCheckerService-0.0.1-SNAPSHOT.jar application.jar

# Expose port 8080 for Spring Boot
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "application.jar"]
