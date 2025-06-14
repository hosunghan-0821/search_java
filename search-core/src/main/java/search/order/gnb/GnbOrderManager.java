package search.order.gnb;

import module.database.dto.Boutique;
import module.database.entity.Product;
import module.database.entity.ProductSize;
import module.database.repository.ProductRepository;
import module.discord.DiscordBot;
import module.discord.DiscordString;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.annotation.Transactional;
import search.chrome.ChromeDriverTool;
import search.common.log.Buffered;
import search.common.log.BufferedLog;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.gnb.dto.OrderResultDto;
import search.order.gnb.index.TokenEvaluator;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Component
@Slf4j
public class GnbOrderManager {

    private final GnbOrderService gnbOrderService;
    private final SeleniumDriverPool seleniumDriverPool;
    private final ProductRepository productRepository;
    private final TokenEvaluator tokenEvaluator;
    private final ReentrantLock finalOrderStepLock = new ReentrantLock(true);
    private final DiscordBot discordBot;
    private final Environment env;

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
            sendAutoOrderNotification("상품 주문 시작", autoOrderRequestDto, "", Color.GRAY, "LOG");

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
                sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, e.getMessage(), Color.RED, "LOG", autoOrderRequestDto.getSku());
                return;
            } finally {
                if (finalOrderStepLock.isHeldByCurrentThread()) {
                    finalOrderStepLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + " ERROR msg : " + e.getMessage());
            sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, e.getMessage(), Color.RED, "FAIL", autoOrderRequestDto.getSku());
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
                Long productId = this.findTokenAllMatched(orderInfo.getSku());
                if (productId == -1L) {
                    sendAutoOrderNotification("DB 차감 에러", AutoOrderRequestDto.builder().sku(orderInfo.getSku()).build(), String.format("주문내역 확인 후 DB차감 개수 수기 변경 필요합니다. SKU:%s", orderInfo.getSku()), Color.RED, "FAIL");
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
                sendAutoOrderNotification("상품 주문 성공", productId.equals(autoOrderRequestDto.getProductId()) ? autoOrderRequestDto : AutoOrderRequestDto.builder().sku(orderInfo.getSku()).build(), "", Color.GREEN, "SUCCESS", orderInfo.getSku(), sizeOrderBuilder.toString());
            }
        } else {
            sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, "STEP3 실패 장바구니에 상품이 있는지 확인하세요 | 다른 주문과 동시에 들어갈 수 있습니다.", Color.RED, "FAIL", autoOrderRequestDto.getSku());
        }

    }

    /**
     * Discord Bot 알림을 위한 공통 메소드
     */
    private void sendAutoOrderNotification(String title, AutoOrderRequestDto dto, String errorMessage, Color color, String sendType, String... skus

    ) {
        Long discordChannel = null;

        boolean isDev = env.acceptsProfiles(Profiles.of("dev"));   // ★ 현재 dev?

        if (isDev) {
            discordChannel = DiscordString.GNB_TEST_ORDER_LOG_CHANNEL;
        } else {
            switch (sendType) {
                case "SUCCESS":
                    discordChannel = DiscordString.GNB_AUTO_ORDER_CHANNEL;
                    break;
                case "FAIL":
                    discordChannel = DiscordString.GNB_AUTO_ORDER_FAIL_CHANNEL;
                    break;
                case "LOG":
                    discordChannel = DiscordString.GNB_AUTO_ORDER_LOG_CHANNEL;
                    break;
                default:
                    break;
            }
        }


        try {
            discordBot.sendAutoOrderMessage(discordChannel, title, makeDiscordSendMessage(dto, title, errorMessage), dto.getProductLink(), skus, color);
        } catch (Exception e) {
            log.error("DISCORD SNED ERROR:  MESSAGE TITLE: {}", title);
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

    /*
     * 토큰값이 유효한 것들을 갖옴
     * */
    public Long findTokenAllMatched(String sku) {
        List<Long> evaluateResult = tokenEvaluator.evaluate(sku);

        if (evaluateResult == null) {
            log.info("evaluateResult null 이유파악 필요");
            return -1L;
        }
        if (evaluateResult.isEmpty()) {
            return -1L;
        }

        evaluateResult.forEach(v -> System.out.println("sku : " + sku + "허용한 상품 ID : " + v));
        return evaluateResult.get(0);
    }

    @Transactional(readOnly = true)
    public boolean validateProduct(AutoOrderRequestDto autoOrderRequestDto) {

        assert (autoOrderRequestDto.getProductId() != null);
        Optional<Product> autoOrderProduct = productRepository.findById(autoOrderRequestDto.getProductId());
        if (autoOrderProduct.isPresent()) {
            Product product = autoOrderProduct.get();
            if (!Boutique.GNB.getName().equals(product.getBoutique())) {
                log.info("[Auto Order] - BOUTIQUE 값이 상이합니다 BOUTIQUE:{} SKU: {}", product.getBoutique(), autoOrderRequestDto.getSku());
                return false;
            }

            if (product.getPrice() == 0 || autoOrderRequestDto.getPrice() <= product.getPrice()) {
                return true;
            } else {
                sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, "지정가보다 높음", Color.RED, "FAIL", autoOrderRequestDto.getSku());
                log.info("[Auto Order] - 기준 가격보다 상품의 현재 가격이 높아서 주문하지 않습니다. sku : {}", autoOrderRequestDto.getSku());
                return false;
            }
        } else {
            log.info("[Auto Order] - DB에서 선정되지 않은 상품입니다 SKU: {}", autoOrderRequestDto.getSku());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void setValidSizesAndOrderNum(AutoOrderRequestDto autoOrderRequestDto) {
        Optional<Product> autoOrderProduct = productRepository.findById(autoOrderRequestDto.getProductId());
        if (autoOrderProduct.isPresent()) {
            List<ProductSize> productSizes = autoOrderProduct.get().getProductSize();
            List<String> validSizes = new ArrayList<>();
            for (ProductSize productSize : productSizes) {
                validSizes.add(productSize.getName());
            }
            autoOrderRequestDto.setValidSizes(validSizes);
            autoOrderRequestDto.setOrderNum(autoOrderProduct.get().getCount());
            if (autoOrderProduct.get().getCount() == 0) {
                sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, "재고 0", Color.RED, "FAIL", autoOrderRequestDto.getSku());
            }
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
        //TODO 대문자로 바꾸는것도 필요할듯?
        String noWhiteSpaceSku = autoOrderRequestDto.getSku().replaceAll(" ", "").trim();
        return productRepository.findAutoOrderProduct(noWhiteSpaceSku, autoOrderRequestDto.getBoutique());
    }

    private String makeDiscordSendMessage(AutoOrderRequestDto autoOrderRequestDto, String headerMessage, String errorMessage) {
        return String.format("%s%n" + "sku                : %s%n" + "오류 메시지        : %s", headerMessage, autoOrderRequestDto.getSku(), errorMessage);
    }
}
