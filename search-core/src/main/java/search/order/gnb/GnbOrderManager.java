package search.order.gnb;

import module.database.dto.Boutique;
import module.database.entity.Product;
import module.database.entity.ProductSize;
import module.database.repository.ProductRepository;
import module.discord.DiscordBot;
import module.discord.DiscordString;
import search.chrome.ChromeDriverTool;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.pool.SeleniumDriverPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import search.util.SeleniumUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
@Slf4j
public class GnbOrderManager {

    private final GnbOrderService gnbOrderService;
    private final SeleniumDriverPool seleniumDriverPool;
    private final ProductRepository productRepository;
    private final ReentrantLock finalOrderStepLock = new ReentrantLock(true);
    private final DiscordBot discordBot;

    @Async
    public void orderProduct(AutoOrderRequestDto autoOrderRequestDto) {
        BlockingQueue<ChromeDriverTool> gnbBlockingQueue = seleniumDriverPool.getBrandBlockingQueue(Boutique.GNB.getName());
        ChromeDriverTool chromeDriverTool = null;
        try {
            chromeDriverTool = validateChromeDriverTool(gnbBlockingQueue);
            ChromeDriver driver = chromeDriverTool.getChromeDriver();
            WebDriverWait wait = chromeDriverTool.getWebDriverWait();

            // 공통 메소드로 추출된 Discord 알림
            sendAutoOrderNotification("상품 주문 시작", autoOrderRequestDto, "", Color.GRAY);

            gnbOrderService.step1(driver, wait, autoOrderRequestDto);
            gnbOrderService.step2(driver, wait, autoOrderRequestDto);

            try {
                boolean acquired = finalOrderStepLock.tryLock(3, TimeUnit.MINUTES);
                if (acquired) {
                    gnbOrderService.step3(driver, wait, autoOrderRequestDto);
                } else {
                    log.error("락 획득하지 못해서, 최종 주문 실패 SKU: {}, PRODUCT LINK: {}", autoOrderRequestDto.getSku(), autoOrderRequestDto.getProductLink());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + " ERROR MSG : " + e.getMessage());
            } finally {
                if (finalOrderStepLock.isHeldByCurrentThread()) {
                    finalOrderStepLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + " ERROR msg : " + e.getMessage());
            sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, e.getMessage(), Color.RED, autoOrderRequestDto.getSku());
            return;
        } finally {
            if (chromeDriverTool != null) {
                if (!gnbBlockingQueue.offer(chromeDriverTool)) {
                    log.error("BlockingQueue Add Error");
                }
            }
        }

        // 성공 알림
        sendAutoOrderNotification("상품 주문 성공", autoOrderRequestDto, "", Color.GREEN, autoOrderRequestDto.getSku());
    }

    /**
     * Discord Bot 알림을 위한 공통 메소드
     */
    private void sendAutoOrderNotification(
            String title,
            AutoOrderRequestDto dto,
            String errorMessage,
            Color color,
            String... skus
    ) {
        discordBot.sendAutoOrderMessage(
                DiscordString.GNB_AUTO_ORDER_CHANNEL,
                title,
                makeDiscordSendMessage(dto, title, errorMessage),
                dto.getProductLink(),
                skus,
                color
        );
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
        Optional<Product> autoOrderProduct = findProduct(autoOrderRequestDto);
        if (autoOrderProduct.isPresent()) {
            Product product = autoOrderProduct.get();
            if (product.getPrice() == 0 || autoOrderRequestDto.getPrice() <= product.getPrice()) {
                return true;
            } else {
                log.info("[Auto Order] - 기준 가격보다 상품의 현재 가격이 높아서 주문하지 않습니다. sku : {}", autoOrderRequestDto.getSku());
                return false;
            }
        } else {
            log.info("[Auto Order] - DB에서 선정되지 않은 상품입니다 SKU: {}", autoOrderRequestDto.getSku());
            return false;
        }
    }

    public void setValidSizes(AutoOrderRequestDto autoOrderRequestDto) {
        Optional<Product> autoOrderProduct = findProduct(autoOrderRequestDto);
        if (autoOrderProduct.isPresent()) {
            List<ProductSize> productSizes = autoOrderProduct.get().getProductSize();
            List<String> validSizes = new ArrayList<>();
            for (ProductSize productSize : productSizes) {
                if (productSize.isAutoBuy()) {
                    validSizes.add(productSize.getName());
                }
            }
            autoOrderRequestDto.setValidSizes(validSizes);
        }
    }

    private boolean isValidChromeDriver(ChromeDriverTool chromeDriverTool) {
        if (chromeDriverTool == null) {
            return false;
        }
        ChromeDriver driver = chromeDriverTool.getChromeDriver();
        return SeleniumUtil.isSessionAlive(driver);
    }

    private Optional<Product> findProduct(AutoOrderRequestDto autoOrderRequestDto) {
        String noWhiteSpaceSku = autoOrderRequestDto.getSku().replaceAll(" ", "").trim();
        return productRepository.findAutoOrderProduct(noWhiteSpaceSku, autoOrderRequestDto.getBoutique());
    }

    private String makeDiscordSendMessage(AutoOrderRequestDto autoOrderRequestDto, String headerMessage, String errorMessage) {
        return String.format(
                "%s%n" +
                        "sku                : %s%n" +
                        "오류 메시지        : %s",
                headerMessage,
                autoOrderRequestDto.getSku(),
                errorMessage
        );
    }
}
