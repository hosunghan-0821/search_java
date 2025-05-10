package search.chrome;


import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;

@Component
public class ChromeDriverToolFactory {

    private final HashMap<String, ChromeDriverTool> factoryHashMap = new HashMap<String, ChromeDriverTool>();


    public ChromeDriverTool getChromeDriverTool(String key) {
        return factoryHashMap.get(key);
    }

    public ChromeDriverTool makeChromeDriverTool(String key) {
        ChromeDriver chromeDriver = new ChromeDriver(setOptions());
        WebDriverWait wait = new WebDriverWait(chromeDriver, Duration.ofMillis(7000)); // 최대 7초 대기
        ChromeDriverTool chromeDriverTool = new ChromeDriverTool(chromeDriver, wait);

        factoryHashMap.put(key, chromeDriverTool);
        return chromeDriverTool;
    }


    public ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("window-size=1920x1080");
        options.addArguments("start-maximized");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-automation");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("detach", true);
        return options;
    }

    private ChromeOptions setOptions() {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("window-size=1920x1080");
        options.addArguments("start-maximized");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-automation");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("detach", true);
        return options;
    }


}
