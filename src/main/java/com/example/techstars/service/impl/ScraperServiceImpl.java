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
        log.info("=== Starting filter selection for: {} ===", jobFunction);
        // Використовуємо WebDriverWait для надійного очікування елементів
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // --- КРОК 1: Клік на кнопку фільтра "Job function" ---

            // Використовуємо стабільний data-testid для пошуку обгортки фільтра,
            // а потім знаходимо всередині неї елемент з role="button".
            By dropdownButtonLocator = By.cssSelector("div[data-testid='filter-option-item-0'] div[role='button']");

            WebElement dropdownButton = wait.until(ExpectedConditions.elementToBeClickable(dropdownButtonLocator));

            // JS-клік надійніший для елементів, створених фреймворками
            js.executeScript("arguments[0].click();", dropdownButton);
            log.info("Job function dropdown clicked successfully.");

            // --- КРОК 2: Вибір опції зі списку ---

            // Формуємо селектор, використовуючи data-testid, який містить назву функції.
            // Це набагато надійніше, ніж шукати за текстом.
            String optionTestId = String.format("job_functions-%s", jobFunction);
            By optionLocator = By.cssSelector(String.format("div[data-testid='%s']", optionTestId));

            WebElement option = wait.until(ExpectedConditions.elementToBeClickable(optionLocator));

            // Клікаємо на знайдену опцію
            option.click();
            log.info("Successfully selected option: {}", jobFunction);

            // --- КРОК 3: Очікування оновлення результатів ---

            // Невелика пауза, щоб DOM встиг оновитися після застосування фільтра.
            Thread.sleep(3000);

            log.info("=== Filter selection completed successfully ===");

        } catch (Exception e) {
            log.error("=== FILTER SELECTION FAILED ===", e);
            debugPageState(driver); // Ваш метод для збору налагоджувальної інформації
            throw new IllegalStateException("Could not select job function: " + jobFunction, e);
        }
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