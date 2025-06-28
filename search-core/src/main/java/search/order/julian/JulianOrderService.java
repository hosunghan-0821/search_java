package search.order.julian;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.database.repository.ProductRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import search.common.exception.ProductSearchFailException;
import search.common.log.BufferedLog;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.common.dto.OrderResultDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class JulianOrderService {


    private final ProductRepository productRepository;

    @Value("${julian.user.id}")
    private String userId;

    @Value("${julian.user.pw}")
    private String userPw;

    private final Environment env;


    /*
     * 상품 페이지 이동
     * */
    @Retryable(retryFor = {TimeoutException.class}, backoff = @Backoff(delay = 2000))
    public void step1(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        BufferedLog.info("JULIAN STEP1 상품 페이지 이동 START  SKU: {}", autoOrderRequestDto.getSku());
        validateLogin(driver, wait, autoOrderRequestDto);

        //상품페이지 이동 Step1
        if (driver.getCurrentUrl().equals(autoOrderRequestDto.getProductLink())) {
            driver.navigate().refresh();
        }

        driver.get(autoOrderRequestDto.getProductLink());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("box-product-grid")));

        BufferedLog.info("JULIAN STEP1 상품 페이지 이동 SUCCESS  SKU: {}", autoOrderRequestDto.getSku());
    }

    /*
     *
     * 상품 검색 쇼핑카트 등록
     * */
    @Retryable(retryFor = {TimeoutException.class, ProductSearchFailException.class}, backoff = @Backoff(delay = 1000))
    public void step2(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) throws InterruptedException {

        BufferedLog.info("JULIAN STEP2 상품 검색 쇼핑카트 등록 START SKU: {}", autoOrderRequestDto.getSku());

        //사이트 버그로 step2 2번해야함.
        this.step2Loop(driver, wait, autoOrderRequestDto, 1);
        driver.navigate().refresh();
        Map<String, Long> orderMap = this.step2Loop(driver, wait, autoOrderRequestDto, 2);

        //차감식 진행
        for (var entry : orderMap.entrySet()) {
            //log.info("주문 Size : {}, 갯수 Num: {} ", entry.getKey(), entry.getValue());
            autoOrderRequestDto.setOrderNum(autoOrderRequestDto.getOrderNum() - entry.getValue());
        }

    }

    private Map<String, Long> step2Loop(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto, int tryCount) {
        long dbMaxOrderNum = autoOrderRequestDto.getOrderNum();
        WebElement topDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("box-product-grid")));
        List<WebElement> productElements = topDiv.findElements(By.xpath("./div"));
        Map<String, Long> orderSizeMap = new HashMap<>();

        for (WebElement productElement : productElements) {
            String sku = productElement.findElement(By.xpath(".//div[@class='produt_reference']")).getText();
            if (autoOrderRequestDto.getSku().equals(sku)) {
                List<WebElement> sizeElements = productElement.findElements(By.xpath(".//div[@class='easycart_combhookpopup']//span[@class='sizeName']"));
                List<WebElement> orderInputElements = productElement.findElements(By.xpath(".//div[@class='easycart_combhookpopup']//input"));
                if (sizeElements.size() != orderInputElements.size()) {
                    BufferedLog.error("Size 개수와 Input 개수가 다름 확인 필요");
                    continue;
                }

                log.info("SKU: {}", sku);
                HashSet<String> sizeSet = new HashSet<>();
                for (int i = 0; i < sizeElements.size(); i++) {
                    WebElement sizeElement = sizeElements.get(i);
                    WebElement element = sizeElement.findElement(By.xpath("./parent::a"));
                    WebElement inputElement = orderInputElements.get(i);

                    String size = element.getText().trim();
                    long totalNum = Long.parseLong(inputElement.getAttribute("data-max-qty"));

                    if (!autoOrderRequestDto.getValidSizes().contains(size)) {
                        List<String> validSizes = Optional.of(autoOrderRequestDto.getValidSizes()).orElse(Collections.emptyList());
                        String productSizes = String.join(",", validSizes);
                        BufferedLog.error("해당하는 Size가 아니므로 넘깁니다 | JULIAN SIZE {} | MY SIZE {}", size, productSizes);
                        inputElement.sendKeys("0");
                        continue;
                    }
                    // 유효한 개수 확인
                    long validOrderNum = Math.min(totalNum, dbMaxOrderNum);
                    if (validOrderNum == 0 || sizeSet.contains(size)) {
                        continue;
                    }
                    // 차감식 적용
                    dbMaxOrderNum -= validOrderNum;
                    inputElement.clear();
                    inputElement.sendKeys(String.valueOf(validOrderNum));
                    orderSizeMap.put(size, validOrderNum);
                    BufferedLog.info("Size : {}, 수량 : {}", size, validOrderNum);
                    sizeSet.add(size);
                }
                if (orderSizeMap.isEmpty()) {
                    BufferedLog.error("No Order Size");
                    throw new RuntimeException("No Order Size");
                }

                BufferedLog.info("원하는 상품 찾음");

                WebElement buttonElement = productElement.findElement(By.xpath(".//a[@class='mc_add_to_cart_button ']//span[@class='multi_cart_icon_span']"));
                buttonElement.click();
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='addtocartconfirmation']")));
                } catch (Exception e) {
                    if (tryCount == 1) {
                        BufferedLog.error("JULIAN STEP2 상품 검색 쇼핑카트 등록 실패");
                        throw new RuntimeException("쇼핑카드 등록 실패 Add to itme Cart 실패");
                    }
                }

                BufferedLog.info("JULIAN STEP2 상품 검색 쇼핑카트 등록 FINISH");
                return orderSizeMap;
            }
        }

        return orderSizeMap;
    }

    /*
     *
     * 상품 카트 Checkout 버튼 확인
     * */
    public OrderResultDto step3(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        driver.get("https://b2bfashion.online/cart?action=show");
        boolean isOrderSaved = false;

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='cart-overview js-cart']")));
        List<WebElement> cartElements = driver.findElements(By.xpath("//ul[@class='cart-items']//li"));

        List<OrderResultDto.OrderInfo> orderInfo = new ArrayList<>();

        for (WebElement cartElement : cartElements) {
            Map<String, String> sizeOrderMap = new HashMap<>();

            List<WebElement> skuLists = cartElement.findElements(By.xpath(".//div[@class='product-line-info']//span[@class='manufacturer_name']"));
            String sku = null;
            if (skuLists != null && skuLists.size() >= 2) {
                sku = skuLists.get(1).getText();
                BufferedLog.info("주문 SKU :{}", sku);
            }

            List<WebElement> sizeInfos = cartElement.findElements(By.xpath(".//div[@class='size-in-cart']//div[@class='product-line-info']"));

            try {
                for (WebElement sizeInfo : sizeInfos) {
                    WebElement size = sizeInfo.findElement(By.xpath(".//span"));
                    String[] split = size.getText().split(":");
                    if (split.length >= 2) {
                        String eachSize = split[1].trim();
                        WebElement orderNumElement = sizeInfo.findElement(By.xpath(".//input[@class='js-cart-line-product-quantity']"));
                        long orderNum = Long.parseLong(orderNumElement.getAttribute("value"));
                        sizeOrderMap.putIfAbsent(eachSize, String.valueOf(orderNum));
                    } else {
                        BufferedLog.error("JULIAN STEP3 SIZE PARSING ERROR");
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                BufferedLog.error("SIZE ORDER NUM 정보 얻기 실패");
            }
            if (autoOrderRequestDto.getSku().equals(sku)) {
                isOrderSaved = true;
            }
            orderInfo.add(new OrderResultDto.OrderInfo(sku, sizeOrderMap));
        }

        if (!isOrderSaved) {
            return new OrderResultDto(false, orderInfo);
        }

        BufferedLog.info("JULIAN STEP3 상품 쇼핑카트내 상품 들어있는거 확인");

        //Checkout 버튼 클릭
        driver.findElement(By.xpath("//a[@class='btn btn-primary']")).click();

        return new OrderResultDto(true, orderInfo);

    }

    /*
     * 상품 마지막 동의 및
     *
     * */
    public void step4(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {
        BufferedLog.info("JULIAN STEP4 상품 마지막 컨펌 페이지 시작");

        WebElement element = driver.findElement(By.id("conditions_to_approve[terms-and-conditions]"));
        if (!element.isSelected()) {
            element.click();
        }

        WebElement finalConfirmCheckBox = driver.findElement(By.id("confirm_order"));
        System.out.println(finalConfirmCheckBox);

        BufferedLog.info("JULIAN STEP4 상품 마지막 컨펌 페이지 완료");
    }

    @Transactional
    public void updateOrderNum(Long productId, long remainCount) {
        productRepository.updateOrderNum(productId, remainCount);
    }


    public void login(ChromeDriver driver, WebDriverWait wait) {

        try {
            driver.get("https://b2bfashion.online/");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
            WebElement id = driver.findElement(By.id("email"));
            id.sendKeys(userId);

            WebElement password = driver.findElement(By.id("pass"));
            password.sendKeys(userPw);

            WebElement loginButton = driver.findElement(By.id("submit_login"));
            loginButton.click();

            //로그인 후 멈춤
            Thread.sleep(5000);
        } catch (Exception e) {
            log.error("줄리앙 로그인 에러");
        }
    }

    private void validateLogin(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {


        driver.get(autoOrderRequestDto.getProductLink());
        //로그인 체크
        if (!driver.getCurrentUrl().equals(autoOrderRequestDto.getProductLink())) {
            login(driver, wait);
        }

    }


}
