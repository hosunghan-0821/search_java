package search.order.gnb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.database.dto.Boutique;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import search.chrome.ChromeDriverTool;
import search.common.log.Buffered;
import search.common.log.BufferedLog;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.common.NotificationService;
import search.order.common.dto.OrderResultDto;
import search.order.common.index.TokenEvaluator;
import search.pool.SeleniumDriverPool;
import search.util.SeleniumUtil;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Component
@Slf4j
public class GnbOrderManager {

    private final GnbOrderService gnbOrderService;
    private final SeleniumDriverPool seleniumDriverPool;
    private final NotificationService notificationService;
    private final TokenEvaluator tokenEvaluator;
    private final ReentrantLock finalOrderStepLock = new ReentrantLock(true);


    @Async
    @Buffered
    public void orderProduct(AutoOrderRequestDto autoOrderRequestDto) {
        BlockingQueue<ChromeDriverTool> gnbBlockingQueue = seleniumDriverPool.getBrandBlockingQueue(Boutique.GNB.getName());
        ChromeDriverTool chromeDriverTool = null;
        OrderResultDto orderResultDto = null;
        try {
            chromeDriverTool = validateChromeDriverTool(gnbBlockingQueue);
            ChromeDriver driver = chromeDriverTool.getChromeDriver();
            WebDriverWait wait = chromeDriverTool.getWebDriverWait();


            // 공통 메소드로 추출된 Discord 알림
            notificationService.sendAutoOrderNotification("상품 주문 시작", autoOrderRequestDto, "", Color.GRAY, "LOG");

            gnbOrderService.step1(driver, wait, autoOrderRequestDto);
            gnbOrderService.step2(driver, wait, autoOrderRequestDto);

            try {
                boolean acquired = finalOrderStepLock.tryLock(3, TimeUnit.MINUTES);
                if (acquired) {
                    orderResultDto = gnbOrderService.step3(driver, wait, autoOrderRequestDto);
                } else {
                    log.error("락 획득하지 못해서, 최종 주문 실패 SKU: {}, PRODUCT LINK: {}", autoOrderRequestDto.getSku(), autoOrderRequestDto.getProductLink());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + " ERROR MSG : " + e.getMessage());
                notificationService.sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, e.getMessage(), Color.RED, "LOG", autoOrderRequestDto.getSku());
                return;
            } finally {
                if (finalOrderStepLock.isHeldByCurrentThread()) {
                    finalOrderStepLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + " ERROR msg : " + e.getMessage());
            notificationService.sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, e.getMessage(), Color.RED, "FAIL", autoOrderRequestDto.getSku());
            return;
        } finally {
            if (chromeDriverTool != null) {
                if (!gnbBlockingQueue.offer(chromeDriverTool)) {
                    log.error("BlockingQueue Add Error");
                }
            }
        }

        // 성공 알림 비동기로 빼도 될듯하다 .(이미 비동기 스레드여서 문제 없으려나)
        if (Objects.nonNull(orderResultDto) && orderResultDto.isOrderSaved()) {

            for (OrderResultDto.OrderInfo orderInfo : orderResultDto.getOrders()) {
                List<Long> evaluateResult = tokenEvaluator.evaluate(orderInfo.getSku());
                if (evaluateResult == null || evaluateResult.isEmpty()) {
                    continue;
                }
                Long productId = evaluateResult.get(0);
                if (productId == -1L) {
                    notificationService.sendAutoOrderNotification("DB 차감 에러", AutoOrderRequestDto.builder().sku(orderInfo.getSku()).build(), String.format("주문내역 확인 후 DB차감 개수 수기 변경 필요합니다. SKU:%s", orderInfo.getSku()), Color.RED, "FAIL");
                    BufferedLog.error("DB 차감 개수 실패로, 주문내역 확인 후 차감개수 변경 필요합니다. SKU:{}", orderInfo.getSku());
                    continue;
                }
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

                gnbOrderService.updateOrderNum(productId, totalOrderNum);
                BufferedLog.info("GNB STEP3 상품 개수 차감 완료");
                notificationService.sendAutoOrderNotification("상품 주문 성공", productId.equals(autoOrderRequestDto.getProductId()) ? autoOrderRequestDto : AutoOrderRequestDto.builder().sku(orderInfo.getSku()).build(), "", Color.GREEN, "SUCCESS", orderInfo.getSku(), sizeOrderBuilder.toString());
            }
        } else {
            notificationService.sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, "STEP3 실패 장바구니에 상품이 있는지 확인하세요 | 다른 주문과 동시에 들어갈 수 있습니다.", Color.RED, "FAIL", autoOrderRequestDto.getSku());
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
            chromeDriverTool = seleniumDriverPool.makeSeleniumDriverTool(Boutique.GNB.getName());
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
