package search.order.julian;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.database.dto.Boutique;
import module.database.repository.ProductRepository;
import module.discord.DiscordBot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import search.chrome.ChromeDriverTool;
import search.common.log.Buffered;
import search.common.log.BufferedLog;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.common.NotificationService;
import search.order.common.OrderManager;
import search.order.common.dto.OrderResultDto;
import search.order.common.index.TokenEvaluator;
import search.pool.SeleniumDriverPool;
import search.util.SeleniumUtil;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Component
@Slf4j
public class JulianOrderManager implements OrderManager {
    private final JulianOrderService julianOrderService;
    private final SeleniumDriverPool seleniumDriverPool;
    private final TokenEvaluator tokenEvaluator;
    private final NotificationService notificationService;
    private final ReentrantLock finalOrderStepLock = new ReentrantLock(true);
    @Getter
    private final Map<String, ZonedDateTime> lastOrderedAt = new ConcurrentHashMap<>();

    @Async
    @Buffered
    @Override
    public void orderProduct(AutoOrderRequestDto autoOrderRequestDto) {
        BlockingQueue<ChromeDriverTool> julianBlockingQueue = seleniumDriverPool.getBrandBlockingQueue(Boutique.JULIAN.getName());
        ChromeDriverTool chromeDriverTool = null;
        OrderResultDto orderResultDto = null;
        try {
            chromeDriverTool = validateChromeDriverTool(julianBlockingQueue);
            ChromeDriver driver = chromeDriverTool.getChromeDriver();
            WebDriverWait wait = chromeDriverTool.getWebDriverWait();

            // 페이지 버그
            // 1시간 내 주문했던적이 있는 상품인지. validate
            {
                // 체크
                ZonedDateTime oneHourAgo = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1);
                String sku = autoOrderRequestDto.getSku();

                ZonedDateTime last = lastOrderedAt.get(sku);
                if (last != null && last.isAfter(oneHourAgo)) {
                    log.info("[Julian]: already ordered within 1 hour. sku: {}", sku);
                    return;
                }
            }


            // 공통 메소드로 추출된 Discord 알림
            notificationService.sendAutoOrderNotification("상품 주문 시작", autoOrderRequestDto, "", Color.GRAY, "LOG");

            julianOrderService.step1(driver, wait, autoOrderRequestDto);
            julianOrderService.step2(driver, wait, autoOrderRequestDto);
            orderResultDto = julianOrderService.step3(driver, wait, autoOrderRequestDto);
            julianOrderService.step4(driver, wait, autoOrderRequestDto);
            //정상주문 됬을 때, 관리하도록 변경
            lastOrderedAt.put(autoOrderRequestDto.getSku(), ZonedDateTime.now(ZoneId.of("Asia/Seoul")));

        } catch (Exception e) {
            log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + " ERROR msg : " + e.getMessage());
            notificationService.sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, e.getMessage(), Color.RED, "FAIL", autoOrderRequestDto.getSku());
            return;
        } finally {
            if (chromeDriverTool != null) {
                if (!julianBlockingQueue.offer(chromeDriverTool)) {
                    log.error("BlockingQueue Add Error");
                }
            }
        }

        //최종성공 후처리
        for (OrderResultDto.OrderInfo orderInfo : orderResultDto.getOrders()) {

            Long productId = autoOrderRequestDto.getProductId();
            long totalOrderNum = 0L;
            StringBuilder sizeOrderBuilder = new StringBuilder();
            sizeOrderBuilder.append("\n");
            for (var entry : orderInfo.getSizeOrderNumMap().entrySet()) {
                try {
                    long orderNum = Long.parseLong(entry.getValue().trim());
                    sizeOrderBuilder.append(String.format("SIZE : %s NUM: %s", entry.getKey(), entry.getValue()));
                    sizeOrderBuilder.append("\n");
                    totalOrderNum += orderNum;
                } catch (Exception e) {
                    BufferedLog.error("상품 주문 개수 parsing error Parsing String : {}, KEY: {}", entry.getValue(), entry.getKey());
                }
            }
            julianOrderService.updateOrderNum(productId, totalOrderNum);
            BufferedLog.info("JULIAN STEP4 상품 개수 차감 완료");
            notificationService.sendAutoOrderNotification("상품 주문 성공", productId.equals(autoOrderRequestDto.getProductId()) ? autoOrderRequestDto : AutoOrderRequestDto.builder().sku(orderInfo.getSku()).build(), "", Color.GREEN, "SUCCESS", orderInfo.getSku(), sizeOrderBuilder.toString());
        }


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
            chromeDriverTool = seleniumDriverPool.makeSeleniumDriverTool(Boutique.JULIAN.getName());
        }
        return chromeDriverTool;
    }


    private boolean isValidChromeDriver(ChromeDriverTool chromeDriverTool) {
        if (chromeDriverTool == null) {
            return false;
        }
        ChromeDriver driver = chromeDriverTool.getChromeDriver();
        return SeleniumUtil.isSessionAlive(driver);
    }
}
