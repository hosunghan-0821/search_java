package com.example.kream.search.chrome;



import lombok.Getter;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.HashMap;
import java.util.HashSet;

@Getter
public class ChromeDriverTool {

    private final ChromeDriver chromeDriver;

    private final WebDriverWait webDriverWait;


    private boolean isLoadData = false;

    private boolean isRunning = false;

    public ChromeDriverTool(ChromeDriver chromeDriver, WebDriverWait webDriverWait) {
        this.chromeDriver = chromeDriver;
        this.webDriverWait = webDriverWait;
    }


    public void isRunning(boolean bool) {isRunning = bool;}
}
