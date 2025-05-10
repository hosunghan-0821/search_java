package search.order.gnb;

import module.database.dto.Boutique;
import module.database.entity.Product;
import module.database.repository.ProductRepository;
import org.openqa.selenium.remote.RemoteWebDriver;
import search.chrome.ChromeDriverTool;
import search.chrome.ChromeDriverToolFactory;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.pool.SeleniumDriverPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import search.util.SeleniumUtil;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@Slf4j
public class GnbOrderManager {

    private final GnbOrderService gnbOrderService;
    private final SeleniumDriverPool seleniumDriverPool;
    private final ProductRepository productRepository;

    /*
     *
     * 이 메소드 자체를 비동기 적으로 돌리는게 나아보임.
     * 왜? -> Api Response를 너무 오래잡고 있음.
     * */
    @Async
    public void orderProduct(AutoOrderRequestDto autoOrderRequestDto) {

        BlockingQueue<ChromeDriverTool> gnbBlockingQueue = seleniumDriverPool.getBrandBlockingQueue(Boutique.GNB.getName());
        ChromeDriverTool chromeDriverTool = null;
        try {

            chromeDriverTool = validateChromeDriverTool(gnbBlockingQueue);
            ChromeDriver driver = chromeDriverTool.getChromeDriver();
            WebDriverWait wait = chromeDriverTool.getWebDriverWait();
            gnbOrderService.step1(driver, wait, autoOrderRequestDto);
            gnbOrderService.step2(driver, wait, autoOrderRequestDto);
            gnbOrderService.step3(driver, wait, autoOrderRequestDto);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + "ERROR MSG : " + e.getMessage());
        } finally {
            if (chromeDriverTool != null) {
                if (!gnbBlockingQueue.offer(chromeDriverTool)) {
                    log.error("BlockingQueue Add Error");
                }
            }
        }

        //Discord Notice 어떤 상품 자동주문 진행시켰는지 Noti

    }

    private ChromeDriverTool validateChromeDriverTool(BlockingQueue<ChromeDriverTool> gnbBlockingQueue) throws InterruptedException {
        ChromeDriverTool chromeDriverTool = null;
        boolean isValid = false;
        for (int i = 0; i < 3; i++) {
            chromeDriverTool = gnbBlockingQueue.poll(5, TimeUnit.SECONDS);
            if (isValidChromeDriver(chromeDriverTool)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            chromeDriverTool = seleniumDriverPool.makeSeleniumDriverTool(Boutique.GNB.getName());
        }

        return chromeDriverTool;
    }

    public boolean validateProduct(AutoOrderRequestDto autoOrderRequestDto) {
        //DB에서 유효한 상품과 Size인지 확인
        String noWhiteSpaceSku = autoOrderRequestDto.getSku().replaceAll(" ", "").trim();
        Optional<Product> autoOrderProduct = productRepository.findAutoOrderProduct(noWhiteSpaceSku, autoOrderRequestDto.getBoutique());
        if (autoOrderProduct.isPresent()) {
            //TODO 가격비교 로직필요.
            return true;
        } else {
            return false;
        }

    }

    private boolean isValidChromeDriver(ChromeDriverTool chromeDriverTool) {

        if (chromeDriverTool == null) {
            return false;
        }
        ChromeDriver driver = chromeDriverTool.getChromeDriver();
        return SeleniumUtil.isSessionAlive(driver);
    }

}
