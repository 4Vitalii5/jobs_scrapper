package com.example.techstars.service;

import com.example.techstars.model.Job;
import com.example.techstars.model.Organization;
import com.example.techstars.model.Tag;
import com.example.techstars.repository.JobRepository;
import com.example.techstars.repository.OrganizationRepository;
import com.example.techstars.repository.TagRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobScraperService {

    private static final Logger log = LoggerFactory.getLogger(JobScraperService.class);
    private static final String BASE_URL = "https://jobs.techstars.com";
    private static final String JOBS_URL = BASE_URL + "/jobs";

    private static final By COOKIE_BUTTON_SELECTOR = By.cssSelector("#onetrust-accept-btn-handler");
    private static final By JOB_FUNCTION_TEXT_SELECTOR = By.xpath("//*[contains(text(),'Job function')]");
    private static final By DROPDOWN_OPTION_SELECTOR = By.cssSelector("div[role='option']");
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

    /**
     * Scrapes jobs from jobs.techstars.com filtered by the given job function using Selenium.
     * @param jobFunction the job function to filter by (e.g., "Software Engineering")
     * @return the number of jobs scraped and saved
     */
    public int scrapeJobsByFunction(String jobFunction) {
        WebDriver driver = null;
        int jobsSaved = 0;
        try {
            driver = initDriver();
            navigateToJobsPage(driver);
            dismissCookieBanner(driver);
            selectJobFunction(driver, jobFunction);
            
            List<WebElement> jobCards = findJobCards(driver, jobFunction);
            for (WebElement card : jobCards) {
                try {
                    parseAndSaveJob(card, jobFunction);
                    jobsSaved++;
                } catch (Exception e) {
                    log.error("Error parsing a job card: {}", e.getMessage());
                }
            }
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
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        // For debugging, you can uncomment the next line to see the browser UI.
        // options.addArguments("--headless=new"); 
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
            Thread.sleep(500);
        } catch (Exception e) {
            log.info("Cookie banner not found or could not be clicked, continuing...");
        }
    }

    private void selectJobFunction(WebDriver driver, String jobFunction) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        WebElement jobFunctionText = wait.until(ExpectedConditions.presenceOfElementLocated(JOB_FUNCTION_TEXT_SELECTOR));
        
        WebElement parent = (WebElement) ((JavascriptExecutor) driver).executeScript("return arguments[0].parentNode;", jobFunctionText);
        WebElement grandparent = (WebElement) ((JavascriptExecutor) driver).executeScript("return arguments[0].parentNode;", parent);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", grandparent);
        grandparent.click();

        List<WebElement> optionsList = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(DROPDOWN_OPTION_SELECTOR));
        boolean found = optionsList.stream()
                .filter(option -> option.getText().trim().equalsIgnoreCase(jobFunction.trim()))
                .findFirst()
                .map(option -> {
                    option.click();
                    return true;
                }).orElse(false);

        if (!found) {
            throw new RuntimeException("Job function '" + jobFunction + "' not found in dropdown options.");
        }
        
        // Wait for job cards to update after filtering
        wait.until(ExpectedConditions.presenceOfElementLocated(JOB_CARD_SELECTOR));
        Thread.sleep(2000); // Allow extra time for JS to render
    }

    private List<WebElement> findJobCards(WebDriver driver, String jobFunction) {
        List<WebElement> jobCards = driver.findElements(JOB_CARD_SELECTOR);
        log.info("Found {} job cards for function: {}", jobCards.size(), jobFunction);
        return jobCards;
    }

    private void parseAndSaveJob(WebElement card, String jobFunction) {
        String jobPageUrl = getAbsoluteUrl(card.findElement(JOB_TITLE_LINK_SELECTOR).getAttribute("href"));
        
        if (jobRepository.existsByJobPageUrl(jobPageUrl)) {
            return;
        }

        Organization org = findOrCreateOrganization(card);

        Job job = Job.builder()
                .positionName(card.findElement(JOB_TITLE_LINK_SELECTOR).getText())
                .jobPageUrl(jobPageUrl)
                .logoUrl(card.findElement(COMPANY_LOGO_LINK_SELECTOR).findElement(By.tagName("img")).getAttribute("src"))
                .laborFunction(jobFunction)
                .location(getElementText(card, LOCATION_SELECTOR).orElse(""))
                .postedDate(getElementAttribute(card, POSTED_DATE_SELECTOR, "content").map(this::parseDate).orElse(0L))
                .description(getElementAttribute(card, DESCRIPTION_SELECTOR, "content").orElse(""))
                .organization(org)
                .tags(findOrCreateTags(card))
                .build();

        jobRepository.save(job);
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
} 