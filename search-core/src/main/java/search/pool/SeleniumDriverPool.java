package search.pool;

import search.chrome.ChromeDriverTool;
import search.chrome.ChromeDriverToolFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeleniumDriverPool {
    /*
     *
     */
    private HashMap<String, BlockingQueue<ChromeDriverTool>> brandSeleniumDriverMap;
    private final ChromeDriverToolFactory chromeDriverToolFactory;

    public SeleniumDriverPool(ChromeDriverToolFactory chromeDriverToolFactory) {
        this.brandSeleniumDriverMap = new HashMap<>();
        this.chromeDriverToolFactory = chromeDriverToolFactory;
    }


    public void initBrandSeleniumDriver(String brandName, int size) {
        BlockingQueue<ChromeDriverTool> blockingQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < size; i++) {
            ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.makeChromeDriverTool(brandName + "_" + i);
            blockingQueue.add(chromeDriverTool);
        }
        brandSeleniumDriverMap.put(brandName, blockingQueue);
    }

    public void refreshSeleniumDriver(String brandName) {
        BlockingQueue<ChromeDriverTool> blockingQueue = brandSeleniumDriverMap.get(brandName);
        for (ChromeDriverTool chromeDriverTool : blockingQueue) {
            try {
                boolean getLock = chromeDriverTool.getReentrantLock().tryLock(5, TimeUnit.SECONDS);
                if (getLock) {
                    chromeDriverTool.getChromeDriver().navigate().refresh();
                } else {
                    log.error(brandName + " chromeDriverPool Refresh Error");
                }

            } catch (InterruptedException e) {
                log.error(brandName + " chromeDriverPool Refresh Error");
            } finally {
                chromeDriverTool.getReentrantLock().unlock();
            }
        }
    }

    public BlockingQueue<ChromeDriverTool> getBrandBlockingQueue(String brandName) {
        return this.brandSeleniumDriverMap.get(brandName);
    }
}
