package search.pool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import search.chrome.ChromeDriverTool;
import search.chrome.ChromeDriverToolFactory;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeleniumDriverPool implements InitializingBean {
    private HashMap<String, BlockingQueue<ChromeDriverTool>> brandSeleniumDriverMap;
    private final ChromeDriverToolFactory chromeDriverToolFactory;


    public SeleniumDriverPool(ChromeDriverToolFactory chromeDriverToolFactory) {
        this.brandSeleniumDriverMap = new HashMap<>();
        this.chromeDriverToolFactory = chromeDriverToolFactory;
    }


    public void initBrandSeleniumDriver(String boutique, int size) {
        BlockingQueue<ChromeDriverTool> blockingQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < size; i++) {
            ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.makeChromeDriverTool(boutique + "_" + UUID.randomUUID().toString());
            blockingQueue.add(chromeDriverTool);
        }
        brandSeleniumDriverMap.put(boutique, blockingQueue);
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

    public BlockingQueue<ChromeDriverTool> getBrandBlockingQueue(String boutique) {
        return this.brandSeleniumDriverMap.get(boutique);
    }

    public boolean addSeleniumDriverTool(String boutique, ChromeDriverTool chromeDriverTool) {
        BlockingQueue<ChromeDriverTool> brandBlockingQueue = this.getBrandBlockingQueue(boutique);
        return brandBlockingQueue.add(chromeDriverTool);
    }

    public ChromeDriverTool makeSeleniumDriverTool(String boutique) {
        return chromeDriverToolFactory.makeChromeDriverTool(boutique + UUID.randomUUID());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.initBrandSeleniumDriver("GNB", 2);
        this.initBrandSeleniumDriver("JULIAN",2);
    }
}
