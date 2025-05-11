package search.order.gnb;

import module.database.dto.Boutique;
import module.database.entity.Product;
import module.database.entity.ProductSize;
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

import java.util.ArrayList;
import java.util.List;
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
    private final ReentrantLock finalOrderStepLock = new ReentrantLock(true);

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
            //여기까진 병렬적으로 진행되나,
            // 최종 주문버튼 누르는 것은 직렬적으로 진행.
            try {
                boolean acquired = finalOrderStepLock.tryLock(3, TimeUnit.MINUTES);
                if (acquired) {
                    gnbOrderService.step3(driver, wait, autoOrderRequestDto);
                } else {
                    log.error("락 획득하지 못해서, 최종 주문 실패 SKU: {}, PRODUCT LINK: {}", autoOrderRequestDto.getSku(), autoOrderRequestDto.getProductLink());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            } catch (Exception e) {
                log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + "ERROR MSG : " + e.getMessage());
            } finally {
                if (finalOrderStepLock.isHeldByCurrentThread()) {
                    finalOrderStepLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("상품 주문 실패 : " + autoOrderRequestDto.toString() + "ERROR msg : " + e.getMessage());
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

    /*
     * 유효한 Size들 DB 로부터 ReqeustDto에 저장 (주문시 유효한 상품 order 넣기)
     *
     * */
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
}
