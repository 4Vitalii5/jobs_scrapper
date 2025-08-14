package com.example.techstars.service.impl;

import com.example.techstars.exception.ResourceNotFoundException;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperServiceImpl implements ScraperService {

    private static final String BASE_URL = "https://jobs.techstars.com";
    private static final String JOBS_URL = BASE_URL + "/jobs";

    private static final By COOKIE_BUTTON_SELECTOR = By.cssSelector("#onetrust-accept-btn-handler");
    private static final By DROPDOWN_OPTION_SELECTOR = By.cssSelector("div[role='option']");
    private static final By JOB_FUNCTION_DROPDOWN_BUTTON = By.cssSelector("[data-testid='job-function']");
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

            jobsSaved = futures.stream()
                    .mapToInt(future -> future.join() ? 1 : 0)
                    .sum();

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
        options.addArguments("--disable-blink-features=AutomationControlled");

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

        return new ChromeDriver(options);
    }

    private void navigateToJobsPage(WebDriver driver) {
        driver.get(JOBS_URL);
    }

    private void dismissCookieBanner(WebDriver driver) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement cookieBtn = shortWait.until(ExpectedConditions.elementToBeClickable(COOKIE_BUTTON_SELECTOR));
            cookieBtn.click();
            shortWait.until(ExpectedConditions.invisibilityOfElementLocated(COOKIE_BUTTON_SELECTOR));
        } catch (Exception e) {
            log.info("Cookie banner not found or could not be clicked, continuing...");
        }
    }

    private void selectJobFunction(WebDriver driver, String jobFunction) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            WebElement dropdownButton = wait.until(ExpectedConditions.visibilityOfElementLocated(JOB_FUNCTION_DROPDOWN_BUTTON));

            js.executeScript("arguments[0].click();", dropdownButton);

            List<WebElement> optionsList = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(DROPDOWN_OPTION_SELECTOR));
            boolean found = optionsList.stream()
                    .filter(option -> option.getText().trim().equalsIgnoreCase(jobFunction.trim()))
                    .findFirst()
                    .map(option -> {
                        js.executeScript("arguments[0].click();", option);
                        return true;
                    }).orElse(false);

            if (!found) {
                throw new ResourceNotFoundException(
                        "Job function '" + jobFunction + "' not found in dropdown options.");
            }

            wait.until(ExpectedConditions.visibilityOfElementLocated(JOB_CARD_SELECTOR));

        } catch (Exception e) {
            log.error("Failed to select job function '{}'. The site might be blocking automation.", jobFunction, e);
            throw new IllegalStateException("Could not select job function: " + jobFunction, e);
        }
    }

    private List<WebElement> findJobCards(WebDriver driver, String jobFunction) {
        List<WebElement> jobCards = driver.findElements(JOB_CARD_SELECTOR);
        log.info("Found {} job cards for function: {}", jobCards.size(), jobFunction);
        return jobCards;
    }

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