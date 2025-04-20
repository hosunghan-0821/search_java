package search.chrome;


import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.concurrent.locks.ReentrantLock;

@Getter
public class ChromeDriverTool{

    private final ChromeDriver chromeDriver;

    private final WebDriverWait webDriverWait;

    private final ReentrantLock reentrantLock = new ReentrantLock(true);

    /*
    * 해당 driver가 로그인이 되어있고 나서 모든 로직이 타야함.
    * */
    private boolean isReady = false;

    private boolean isRunning = false;

    public ChromeDriverTool(ChromeDriver chromeDriver, WebDriverWait webDriverWait) {
        this.chromeDriver = chromeDriver;
        this.webDriverWait = webDriverWait;
    }

    public boolean checkWindowIsClosed(WebDriver driver) {
        try {
            driver.getCurrentUrl();
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public void isRunning(boolean bool) {
        isRunning = bool;
    }
    public void isReady(boolean bool) {
        isReady = bool;
    }

}
