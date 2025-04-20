package search.order.gnb;

import search.chrome.ChromeDriverTool;
import search.chrome.ChromeDriverToolFactory;
import search.dto.OrderRequestDto;
import search.pool.SeleniumDriverPool;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
@Component
@Slf4j
public class GnbOrderManager {

    private final GnbOrderService gnbOrderService;
    private final ChromeDriverToolFactory chromeDriverToolFactory;
    private final SeleniumDriverPool seleniumDriverPool;

    /*
     *
     * 이 메소드 자체를 비동기 적으로 돌리는게 나아보임.
     * */
    @Async
    public void orderProduct(OrderRequestDto orderRequestDto) {

        // ChromeDriver 할당받기 from DriverPool (sync)
        BlockingQueue<ChromeDriverTool> gnbBlockingQueue = seleniumDriverPool.getBrandBlockingQueue("GNB");
        ChromeDriverTool chromeDriverTool = null;
        try {
            chromeDriverTool = gnbBlockingQueue.take();
            ChromeDriver driver = chromeDriverTool.getChromeDriver();
            WebDriverWait wait = chromeDriverTool.getWebDriverWait();
            gnbOrderService.step1(driver, wait, orderRequestDto);
            gnbOrderService.step2(driver, wait, orderRequestDto);
            // gnbOrderService.step3(driver, wait, orderRequestDto);
        } catch (Exception e) {
            log.error("상품 주문 실패 : " + orderRequestDto.toString());
        } finally {
            if (chromeDriverTool != null) {
                if (!gnbBlockingQueue.offer(chromeDriverTool)) {
                    log.error("BlockingQueue Add Error");
                }
            }
        }

        //Discord Notice 어떤 상품 자동주문 진행시켰는지 Noti


    }

    public boolean validateProduct(OrderRequestDto orderRequestDto) {
        //DB에서 유효한 상품과 Size인지 확인

        return true;
    }
}
