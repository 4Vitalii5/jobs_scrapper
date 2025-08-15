package com.example.techstars.service.impl;

import com.example.techstars.model.Job;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.repository.OrganizationRepository;
import com.example.techstars.repository.TagRepository;
import com.example.techstars.service.ScraperService;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperServiceImpl implements ScraperService {

    private static final String BASE_URL = "https://jobs.techstars.com";
    private static final String JOBS_URL = BASE_URL + "/jobs";

    // Improved selectors
    private static final By COOKIE_BUTTON_SELECTOR = By.cssSelector("#onetrust-accept-btn-handler");
    private static final By RESULTS_CONTAINER = By.cssSelector("div[data-testid='results-list']");
    private static final By JOB_CARD_SELECTOR = By.cssSelector("div[data-testid='job-list-item']");
    private static final By JOB_TITLE_LINK_SELECTOR = By.cssSelector("a[data-testid='job-title-link']");
    private static final By COMPANY_LOGO_LINK_SELECTOR = By.cssSelector("a[data-testid='company-logo-link']");
    private static final By LOCATION_SELECTOR = By.cssSelector("div[itemprop='jobLocation'] span.vIGjl");
    private static final By POSTED_DATE_SELECTOR = By.cssSelector("meta[itemprop='datePosted']");
    private static final By DESCRIPTION_SELECTOR = By.cssSelector("meta[itemprop='description']");
    private static final By TAG_SELECTOR = By.cssSelector("div[data-testid='tag'] div");

    private final JobRepository jobRepository;
    private final OrganizationRepository organizationRepository;
    private final TagRepository tagRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    public int scrapeJobsByFunction(String jobFunction) {
        WebDriver driver = null;
        int jobsSaved = 0;
        try {
            driver = initDriver();
            navigateToJobsPage(driver);
            dismissCookieBanner(driver);
            selectJobFunction(driver, jobFunction);

            List<WebElement> jobCards = findJobCards(driver, jobFunction);
            log.info("Found {} job cards for function: {}", jobCards.size(), jobFunction);

            List<CompletableFuture<Boolean>> futures = jobCards.stream()
                    .map(card -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return parseAndSaveJob(card, jobFunction);
                        } catch (Exception e) {
                            log.error("Error parsing a job card: {}", e.getMessage());
                            return false;
                        }
                    }, executorService))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            jobsSaved = futures.stream().mapToInt(future -> future.join() ? 1 : 0).sum();

        } catch (Exception e) {
            log.error("A critical error occurred during scraping: {}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        return jobsSaved;
    }

    private WebDriver initDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.addArguments("--start-maximized");
        options.addArguments(
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-infobars",
                "--disable-extensions",
                "--disable-web-security",
                "--allow-running-insecure-content",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
        return new ChromeDriver(options);
    }

    private void navigateToJobsPage(WebDriver driver) {
        driver.get(JOBS_URL);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.urlContains("jobs"));
    }

    private void dismissCookieBanner(WebDriver driver) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement cookieBtn = shortWait.until(ExpectedConditions.elementToBeClickable(COOKIE_BUTTON_SELECTOR));
            cookieBtn.click();
            shortWait.until(ExpectedConditions.invisibilityOfElementLocated(COOKIE_BUTTON_SELECTOR));
            log.info("Cookie banner dismissed");
        } catch (Exception e) {
            log.info("Cookie banner not found or could not be clicked, continuing...");
        }
    }

    private void selectJobFunction(WebDriver driver, String jobFunction) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Actions actions = new Actions(driver);

        try {
            log.info("=== Starting filter selection for: {} ===", jobFunction);

            // Step 1: Find and click the Job function dropdown
            WebElement jobFunctionDropdown = findJobFunctionDropdown(driver, wait);
            if (jobFunctionDropdown == null) {
                throw new IllegalStateException("Could not locate Job function dropdown");
            }

            // Scroll to element and click
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", jobFunctionDropdown);
            wait.until(ExpectedConditions.elementToBeClickable(jobFunctionDropdown));

            try {
                jobFunctionDropdown.click();
            } catch (Exception e) {
                log.info("Regular click failed, trying JavaScript click");
                js.executeScript("arguments[0].click();", jobFunctionDropdown);
            }

            log.info("Job function dropdown clicked, waiting for options to appear...");
            Thread.sleep(2000); // Give time for dropdown to open

            // Step 2: Wait for dropdown options to be visible and select the option
            boolean optionSelected = selectFromDropdownOptions(driver, wait, js, jobFunction);

            if (!optionSelected) {
                throw new IllegalStateException("Could not select option: " + jobFunction);
            }

            // Step 3: Verify selection and wait for results to load
            Thread.sleep(3000);
            log.info("=== Filter selection completed successfully ===");

        } catch (Exception e) {
            log.error("=== FILTER SELECTION FAILED ===", e);
            debugPageState(driver);
            throw new IllegalStateException("Could not select job function: " + jobFunction, e);
        }
    }

    private WebElement findJobFunctionDropdown(WebDriver driver, WebDriverWait wait) {
        log.info("Searching for Job function dropdown...");

        // Multiple strategies to find the dropdown
        String[] dropdownStrategies = {
                "//button[contains(text(), 'Job function')]",
                "//div[@role='button' and contains(text(), 'Job function')]",
                "//*[contains(text(), 'Job function') and (@role='button' or ancestor::button)]",
                "//button[.//span[contains(text(), 'Job function')]]",
                "//div[.//span[contains(text(), 'Job function')] and (@role='button' or @onclick)]",
                "//*[contains(@class, 'dropdown') and contains(text(), 'Job function')]"
        };

        for (String strategy : dropdownStrategies) {
            try {
                List<WebElement> elements = driver.findElements(By.xpath(strategy));
                for (WebElement element : elements) {
                    if (element.isDisplayed() && element.isEnabled()) {
                        log.info("Found Job function dropdown using strategy: {}", strategy);
                        return element;
                    }
                }
            } catch (Exception e) {
                log.debug("Strategy failed: {} - {}", strategy, e.getMessage());
            }
        }

        // Fallback: look for any clickable element containing "Job function"
        try {
            List<WebElement> allElements = driver.findElements(By.xpath("//*[contains(text(), 'Job function')]"));
            for (WebElement element : allElements) {
                WebElement clickableParent = findClickableParent(element);
                if (clickableParent != null) {
                    log.info("Found Job function dropdown via parent traversal");
                    return clickableParent;
                }
            }
        } catch (Exception e) {
            log.debug("Parent traversal failed: {}", e.getMessage());
        }

        return null;
    }

    private WebElement findClickableParent(WebElement element) {
        WebElement current = element;
        for (int i = 0; i < 5; i++) {
            try {
                if (isClickableElement(current)) {
                    return current;
                }
                current = current.findElement(By.xpath(".."));
            } catch (Exception e) {
                break;
            }
        }
        return null;
    }

    private boolean isClickableElement(WebElement element) {
        try {
            String tagName = element.getTagName().toLowerCase();
            String role = element.getAttribute("role");
            String onclick = element.getAttribute("onclick");
            String className = element.getAttribute("class");

            return element.isDisplayed() && element.isEnabled() && (
                    tagName.equals("button") ||
                            "button".equals(role) ||
                            onclick != null ||
                            (className != null && (
                                    className.contains("dropdown") ||
                                            className.contains("select") ||
                                            className.contains("clickable") ||
                                            className.contains("btn")
                            ))
            );
        } catch (Exception e) {
            return false;
        }
    }

    private boolean selectFromDropdownOptions(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String jobFunction) {
        log.info("Searching for option: {}", jobFunction);

        // Wait for dropdown options to appear - increased timeout for dynamic content
        try {
            wait.until(drivers -> {
                List<WebElement> visibleElements = drivers.findElements(By.xpath("//*[text()]"));
                long optionsCount = visibleElements.stream()
                        .filter(el -> el.isDisplayed() && !el.getText().trim().isEmpty())
                        .count();
                log.debug("Found {} visible text elements", optionsCount);
                return optionsCount > 10; // Wait for multiple options to appear
            });
            Thread.sleep(1000); // Additional wait for full rendering
        } catch (Exception e) {
            log.warn("Timeout waiting for dropdown options to appear");
        }

        // Strategy 1: Look for exact text match with dynamic class handling
        String[] exactMatchSelectors = {
                String.format("//*[normalize-space(text())='%s']", jobFunction),
                String.format("//div[normalize-space(text())='%s']", jobFunction),
                String.format("//span[normalize-space(text())='%s']", jobFunction),
                String.format("//li[normalize-space(text())='%s']", jobFunction),
                String.format("//*[text()='%s']", jobFunction)
        };

        for (String selector : exactMatchSelectors) {
            if (trySelectOption(driver, js, selector, "exact match")) {
                return true;
            }
        }

        // Strategy 2: Look for elements that contain the text (for dynamic components)
        String[] containerSelectors = {
                String.format("//*[contains(normalize-space(text()), '%s') and string-length(normalize-space(text())) < 20]", jobFunction),
                String.format("//div[text()='%s']", jobFunction),
                String.format("//span[text()='%s']", jobFunction)
        };

        for (String selector : containerSelectors) {
            if (trySelectOption(driver, js, selector, "container match")) {
                return true;
            }
        }

        // Strategy 3: Handle dynamic class names (like divsc-beqWaB-cRYWHK)
        // Look for elements with dynamic classes that contain our text
        String[] dynamicSelectors = {
                String.format("//*[starts-with(@class, 'div') and contains(@class, '-') and normalize-space(text())='%s']", jobFunction),
                String.format("//*[contains(@class, 'beq') and normalize-space(text())='%s']", jobFunction),
                String.format("//*[contains(@class, 'sc-') and normalize-space(text())='%s']", jobFunction)
        };

        for (String selector : dynamicSelectors) {
            if (trySelectOption(driver, js, selector, "dynamic class match")) {
                return true;
            }
        }

        // Strategy 4: Try clicking on parent containers of text elements
        try {
            List<WebElement> textElements = driver.findElements(By.xpath(String.format("//*[normalize-space(text())='%s']", jobFunction)));
            for (WebElement textElement : textElements) {
                if (textElement.isDisplayed()) {
                    // Try the element itself
                    if (attemptClickWithParents(textElement, js, 0)) {
                        log.info("Successfully clicked on text element directly");
                        return true;
                    }

                    // Try parent elements
                    WebElement parent = textElement;
                    for (int level = 1; level <= 3; level++) {
                        try {
                            parent = parent.findElement(By.xpath(".."));
                            if (attemptClickWithParents(parent, js, level)) {
                                log.info("Successfully clicked on parent at level {}", level);
                                return true;
                            }
                        } catch (Exception e) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Parent traversal strategy failed: {}", e.getMessage());
        }

        // Strategy 5: Mouse hover and click approach
        try {
            Actions actions = new Actions(driver);
            List<WebElement> possibleOptions = driver.findElements(By.xpath(String.format("//*[normalize-space(text())='%s']", jobFunction)));

            for (WebElement option : possibleOptions) {
                if (option.isDisplayed()) {
                    try {
                        actions.moveToElement(option).pause(Duration.ofMillis(500)).click().perform();
                        log.info("Successfully clicked using Actions");
                        Thread.sleep(1000);
                        return true;
                    } catch (Exception e) {
                        log.debug("Actions click failed: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Actions strategy failed: {}", e.getMessage());
        }

        // Strategy 6: Debug - list all available options
        debugAvailableOptions(driver);

        return false;
    }

    private boolean attemptClickWithParents(WebElement element, JavascriptExecutor js, int level) {
        try {
            // Check if element looks clickable
            String tagName = element.getTagName().toLowerCase();
            String className = element.getAttribute("class");

            if (!element.isDisplayed() || !element.isEnabled()) {
                return false;
            }

            // Scroll to element
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(300);

            // Try clicking
            if (clickElement(element, js)) {
                Thread.sleep(1000);
                return true;
            }
        } catch (Exception e) {
            log.debug("Click attempt failed at level {}: {}", level, e.getMessage());
        }
        return false;
    }

    private boolean trySelectOption(WebDriver driver, JavascriptExecutor js, String selector, String strategy) {
        try {
            log.debug("Trying {} with selector: {}", strategy, selector);
            List<WebElement> options = driver.findElements(By.xpath(selector));

            for (WebElement option : options) {
                if (option.isDisplayed()) {
                    log.info("Found option using {}: text='{}', tag='{}'",
                            strategy, option.getText(), option.getTagName());

                    try {
                        // Scroll into view
                        js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                        Thread.sleep(500);

                        // Try different click methods
                        if (clickElement(option, js)) {
                            log.info("Successfully selected option using {}", strategy);
                            Thread.sleep(1000);
                            return true;
                        }
                    } catch (Exception e) {
                        log.debug("Failed to click option: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("{} strategy failed: {}", strategy, e.getMessage());
        }
        return false;
    }

    private boolean clickElement(WebElement element, JavascriptExecutor js) {
        // Try multiple click methods
        try {
            // Method 1: Regular click
            element.click();
            log.debug("Regular click succeeded");
            return true;
        } catch (Exception e1) {
            try {
                // Method 2: JavaScript click
                js.executeScript("arguments[0].click();", element);
                log.debug("JavaScript click succeeded");
                return true;
            } catch (Exception e2) {
                try {
                    // Method 3: Dispatch click event
                    js.executeScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));", element);
                    log.debug("Dispatch event click succeeded");
                    return true;
                } catch (Exception e3) {
                    try {
                        // Method 4: Force click via coordinates
                        js.executeScript(
                                "var rect = arguments[0].getBoundingClientRect();" +
                                        "var clickEvent = new MouseEvent('click', {" +
                                        "  bubbles: true," +
                                        "  cancelable: true," +
                                        "  clientX: rect.left + rect.width / 2," +
                                        "  clientY: rect.top + rect.height / 2" +
                                        "});" +
                                        "arguments[0].dispatchEvent(clickEvent);", element);
                        log.debug("Coordinate-based click succeeded");
                        return true;
                    } catch (Exception e4) {
                        try {
                            // Method 5: Try triggering mousedown and mouseup
                            js.executeScript(
                                    "var element = arguments[0];" +
                                            "element.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));" +
                                            "element.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}));" +
                                            "element.dispatchEvent(new MouseEvent('click', {bubbles: true}));", element);
                            log.debug("Mouse event sequence succeeded");
                            return true;
                        } catch (Exception e5) {
                            log.debug("All click methods failed. Last error: {}", e5.getMessage());
                            return false;
                        }
                    }
                }
            }
        }
    }

    private void debugAvailableOptions(WebDriver driver) {
        log.info("=== DEBUG: Available dropdown options ===");
        try {
            // Look for all visible text elements that might be options
            List<WebElement> allTextElements = driver.findElements(By.xpath("//*[text() and string-length(normalize-space(text())) > 0 and string-length(normalize-space(text())) < 50]"));

            int count = 0;
            Set<String> uniqueTexts = new HashSet<>();

            for (WebElement element : allTextElements) {
                if (element.isDisplayed() && count < 30) {
                    String text = element.getText().trim();
                    String tagName = element.getTagName();
                    String className = element.getAttribute("class");

                    if (!text.isEmpty() && !uniqueTexts.contains(text)) {
                        uniqueTexts.add(text);
                        log.info("Option {}: text='{}', tag='{}', class='{}'",
                                count++, text, tagName, className != null ? className.substring(0, Math.min(className.length(), 50)) : "null");

                        // Log parent info for potential clicking targets
                        try {
                            WebElement parent = element.findElement(By.xpath(".."));
                            String parentTag = parent.getTagName();
                            String parentClass = parent.getAttribute("class");
                            log.info("  -> Parent: tag='{}', class='{}'", parentTag,
                                    parentClass != null ? parentClass.substring(0, Math.min(parentClass.length(), 50)) : "null");
                        } catch (Exception e) {
                            // Ignore parent lookup errors
                        }
                    }
                }
            }

            // Also log elements with dynamic class names pattern
            log.info("=== Elements with dynamic classes ===");
            List<WebElement> dynamicElements = driver.findElements(By.xpath("//*[contains(@class, 'sc-') or contains(@class, 'divsc-') or starts-with(@class, 'div')]"));
            count = 0;
            for (WebElement element : dynamicElements) {
                if (element.isDisplayed() && count < 10) {
                    String text = element.getText().trim();
                    String className = element.getAttribute("class");
                    if (!text.isEmpty() && text.length() < 30) {
                        log.info("Dynamic {}: text='{}', class='{}'",
                                count++, text, className != null ? className.substring(0, Math.min(className.length(), 60)) : "null");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to debug options: {}", e.getMessage());
        }
        log.info("=== END DEBUG ===");
    }

    private void debugPageState(WebDriver driver) {
        try {
            log.error("=== PAGE DEBUG INFO ===");
            log.error("Current URL: {}", driver.getCurrentUrl());
            log.error("Page title: {}", driver.getTitle());

            // Log visible buttons
            List<WebElement> buttons = driver.findElements(By.tagName("button"));
            log.error("Found {} buttons on page", buttons.size());

            for (int i = 0; i < Math.min(buttons.size(), 10); i++) {
                WebElement btn = buttons.get(i);
                log.error("Button {}: text='{}', visible={}, enabled={}",
                        i, btn.getText(), btn.isDisplayed(), btn.isEnabled());
            }

        } catch (Exception e) {
            log.error("Could not debug page state: {}", e.getMessage());
        }
    }

    private List<WebElement> findJobCards(WebDriver driver, String jobFunction) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        try {
            // Wait for results to load
            wait.until(ExpectedConditions.presenceOfElementLocated(RESULTS_CONTAINER));
            Thread.sleep(2000); // Additional wait for dynamic content
        } catch (Exception e) {
            log.warn("Results container not found, proceeding anyway");
        }

        List<WebElement> jobCards = driver.findElements(JOB_CARD_SELECTOR);
        log.info("Found {} job cards for function: {}", jobCards.size(), jobFunction);
        return jobCards;
    }

    // Rest of the methods remain the same...
    private boolean parseAndSaveJob(WebElement card, String jobFunction) {
        try {
            String jobPageUrl = getAbsoluteUrl(card.findElement(JOB_TITLE_LINK_SELECTOR).getAttribute("href"));

            if (jobRepository.existsByJobPageUrl(jobPageUrl)) {
                log.debug("Job already exists: {}", jobPageUrl);
                return false;
            }

            Organization org = findOrCreateOrganization(card);
            String location = getElementText(card, LOCATION_SELECTOR).orElse("");

            Job job = Job.builder()
                    .positionName(card.findElement(JOB_TITLE_LINK_SELECTOR).getText())
                    .jobPageUrl(jobPageUrl)
                    .logoUrl(card.findElement(COMPANY_LOGO_LINK_SELECTOR).findElement(By.tagName("img")).getAttribute("src"))
                    .laborFunction(jobFunction)
                    .location(location)
                    .address(location)
                    .postedDate(getElementAttribute(card, POSTED_DATE_SELECTOR, "content").map(this::parseDate).orElse(0L))
                    .description(getElementAttribute(card, DESCRIPTION_SELECTOR, "content").orElse(""))
                    .organization(org)
                    .tags(findOrCreateTags(card))
                    .build();

            jobRepository.save(job);
            log.debug("Successfully saved job: {}", job.getPositionName());
            return true;
        } catch (Exception e) {
            log.error("Error parsing job card: {}", e.getMessage());
            return false;
        }
    }

    private Organization findOrCreateOrganization(WebElement card) {
        WebElement orgLink = card.findElement(COMPANY_LOGO_LINK_SELECTOR);
        String orgUrl = getAbsoluteUrl(orgLink.getAttribute("href"));
        String orgTitle = orgLink.findElement(By.tagName("img")).getAttribute("alt");

        return organizationRepository.findByUrl(orgUrl)
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .title(orgTitle)
                        .url(orgUrl)
                        .build()));
    }

    private Set<Tag> findOrCreateTags(WebElement card) {
        Set<Tag> tags = new HashSet<>();
        List<WebElement> tagElements = card.findElements(TAG_SELECTOR);
        for (WebElement tagEl : tagElements) {
            String tagName = tagEl.getText().trim();
            if (!tagName.isEmpty()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder()
                                .name(tagName)
                                .build()));
                tags.add(tag);
            }
        }
        return tags;
    }

    private String getAbsoluteUrl(String url) {
        if (url == null || url.startsWith("http")) {
            return url;
        }
        return BASE_URL + url;
    }

    private Optional<String> getElementText(WebElement parent, By selector) {
        try {
            return Optional.of(parent.findElement(selector).getText());
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    private Optional<String> getElementAttribute(WebElement parent, By selector, String attribute) {
        try {
            return Optional.of(parent.findElement(selector).getAttribute(attribute));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    private long parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        } catch (Exception e) {
            return 0L;
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
