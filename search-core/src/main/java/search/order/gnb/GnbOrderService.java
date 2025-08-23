package search.order.gnb;

import module.database.repository.ProductRepository;
import org.openqa.selenium.NotFoundException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.annotation.Transactional;
import search.common.exception.ProductSearchFailException;
import search.common.log.BufferedLog;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import search.order.common.dto.OrderResultDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GnbOrderService {

    private final ProductRepository productRepository;

    private final GnbRetryLogicService gnbRetryLogicService;

    @Value("${gebenegozi.user.id}")
    private String userId;

    @Value("${gebenegozi.user.pw}")
    private String userPw;

    private final Environment env;


    /*
     * 상품 페이지 이동
     * */
    @Retryable(retryFor = {TimeoutException.class}, backoff = @Backoff(delay = 2000))
    public void step1(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        BufferedLog.info("GNB STEP1 상품 페이지 이동 START  SKU: {}", autoOrderRequestDto.getSku());
        validateLogin(driver, wait, autoOrderRequestDto);

        //상품페이지 이동 Step1
        if (driver.getCurrentUrl().equals(autoOrderRequestDto.getProductLink())) {
            driver.navigate().refresh();
        }

        driver.get(autoOrderRequestDto.getProductLink());

        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));

        String pattern = "\\S";
        Pattern p = Pattern.compile(pattern);
        wait.until(ExpectedConditions.textMatches(By.xpath("//div[@class='row title font-italic text-capitalize artPrezzi']//div[@class='col-5']"), p));
        BufferedLog.info("GNB STEP1 상품 페이지 이동 SUCCESS  SKU: {}", autoOrderRequestDto.getSku());
    }


    /*
     *
     * 상품 검색 쇼핑카트 등록
     * */
    @Retryable(retryFor = {TimeoutException.class, ProductSearchFailException.class}, backoff = @Backoff(delay = 1000))
    public void step2(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        BufferedLog.info("GNB STEP2 상품 검색 쇼핑카트 등록 START SKU: {}", autoOrderRequestDto.getSku());

        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));

        //상품페이지 검색 Step2
        for (WebElement productElement : elements) {

            //상품정보
            WebElement infoElement = productElement.findElement(By.xpath(".//div[@class='row title font-italic text-capitalize artPrezzi']"));
            List<WebElement> dataList = infoElement.findElements(By.xpath(".//div[@class='col-5']"));
            String sku = dataList.get(1).getText();

            // 원하는 값들 찾으면
            if (autoOrderRequestDto.getSku().equals(sku)) {
                log.info("GNB STEP2 상품 찾음, Size 찾으러 이동 {}", autoOrderRequestDto.getSku());
                BufferedLog.info("GNB STEP2 상품 찾음, Size 찾으러 이동 {}", autoOrderRequestDto.getSku());

                Map<String, Long> orderSizeMap = new HashMap<>();

                try {
                    WebElement imageElement = productElement.findElement(By.xpath(".//img[@class='zoom lozad']"));
                    Actions actions = new Actions(driver);
                    actions.moveToElement(productElement);
                    actions.perform();
                    wait.until(ExpectedConditions.attributeToBeNotEmpty(imageElement, "src"));
                } catch (Exception e) {
                    log.error("이미지 경로 찾기 실패 sku" + sku);
                }


                List<WebElement> sizeElements = productElement.findElements(By.xpath(".//div[@class='row ml-3']//div[@class='col-1 mr-1']"));

                for (WebElement sizeElement : sizeElements) {
                    String size = sizeElement.findElement(By.xpath(".//div[@class='artCod']//div[@class='size']")).getText();
                    log.debug("size : {}", size);
                    if (!autoOrderRequestDto.getValidSizes().contains(size)) {
                        List<String> validSizes = Optional.of(autoOrderRequestDto.getValidSizes()).orElse(Collections.emptyList());
                        String productSizes = String.join(",", validSizes);
                        BufferedLog.info("해당하는 Size가 아니므로 넘깁니다 | GNB SIZE {} | MY SIZE {}", size, productSizes);
                        continue;
                    }
                    WebElement inputElement = sizeElement.findElement(By.xpath(".//div[@class='artCod']//input"));
                    String productSizeMaxOrderNum = inputElement.getAttribute("placeholder");

                    long validOrderNum = Math.min(Long.parseLong(productSizeMaxOrderNum), autoOrderRequestDto.getOrderNum());
                    BufferedLog.info("사이트 수량 : {}, DB 수량: {} , 사려는 수량 : {}", productSizeMaxOrderNum, autoOrderRequestDto.getOrderNum(), validOrderNum);
                    if (validOrderNum == 0) {
                        BufferedLog.error("DB 수량: 0개 이므로 주문을 할 수 없습니다. SKU : {} \t Product Link : {}", sku, autoOrderRequestDto.getProductLink());
                        continue;
                    }
                    //주문완료되면 차감해야함.
                    autoOrderRequestDto.setOrderNum(autoOrderRequestDto.getOrderNum() - validOrderNum);
                    inputElement.sendKeys(String.valueOf(validOrderNum));
                    orderSizeMap.put(size, validOrderNum);
                    BufferedLog.info("Size : {}, 수량 : {}", size, validOrderNum);
                }
                if (orderSizeMap.isEmpty()) {
                    BufferedLog.error("No Order Size");
                    throw new RuntimeException("No Order Size");
                }

                BufferedLog.info("원하는 상품 찾음");

                WebElement buttonElement = productElement.findElement(By.xpath(".//button[@class='btn btn-sm btn-sirio add-item-cart mt-3 ml-5']"));
                buttonElement.click();

                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='swal2-popup swal2-modal swal2-icon-success swal2-show']")));
                } catch (Exception e) {
                    String text = productElement.findElement(By.xpath("//div[@class='col-12 mb-2 font-weight-bold']")).getText();
                    if (text.equals("Items in cart")) {
                        BufferedLog.info("GNB STEP2 상품 검색 쇼핑카트 등록 FINISH");
                        return;
                    } else {
                        break;
                    }

                }
                BufferedLog.info("GNB STEP2 상품 검색 쇼핑카트 등록 FINISH");
                return;
            } else {
                log.info("GNB STEP2 페이지 내 데이터 순회 하면서 상품 검색 현재 검색 SKU : {}  주문 SKU : {}", sku, autoOrderRequestDto.getSku());
            }
        }
        log.error("GNB STEP2 상품 존재하지 않음  sku : {}", autoOrderRequestDto.getSku());
        BufferedLog.error("GNB STEP2 상품 존재하지 않음 - 새로고침 진행 START");
        //새로고침 추가
        {
            validateLogin(driver, wait, autoOrderRequestDto);

            if (driver.getCurrentUrl().equals(autoOrderRequestDto.getProductLink())) {
                driver.navigate().refresh();
            }

            driver.get(autoOrderRequestDto.getProductLink());

            driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));
            String pattern = "\\S";
            Pattern p = Pattern.compile(pattern);
            wait.until(ExpectedConditions.textMatches(By.xpath("//div[@class='row title font-italic text-capitalize artPrezzi']//div[@class='col-5']"), p));
        }
        BufferedLog.error("GNB STEP2 상품 존재하지 않음 - 새로고침 진행 END");
        throw new ProductSearchFailException("GNB STEP2 상품 존재하지 않음");
    }

    /*
     * 쇼핑 카드 등록한 곳에서 주문 확정
     * */
    @Retryable(retryFor = {TimeoutException.class}, backoff = @Backoff(delay = 1000))
    public OrderResultDto step3(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) throws InterruptedException {

        BufferedLog.info("GNB STEP3 상품 쇼핑카트내 주문 시작 START sku: {}", autoOrderRequestDto.getSku());
        driver.get("http://93.46.41.5:1995/cart");

        // 지금 들어온 주문 정보가 아니면 휴지통 누르기
        try {
            List<String> unOrderCartingProduct = findUnOrderCartingProduct(driver, wait, autoOrderRequestDto);
            if (!unOrderCartingProduct.isEmpty()) {
                gnbRetryLogicService.deleteUnOrderCartingProduct(driver, wait, autoOrderRequestDto, unOrderCartingProduct);
            }
        } catch (Exception e) {
            e.printStackTrace();
            BufferedLog.error("UnCarting 정보 삭제 실패 ");
        }


        String pattern = "\\S";
        Pattern p = Pattern.compile(pattern);
        wait.until(ExpectedConditions.textMatches(By.xpath("//div[@class='row title font-italic text-capitalize']//div[@class='col-5']"), p));

        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));

        boolean isOrderSaved = false;
        //상품 존재하는지 확인.

        List<OrderResultDto.OrderInfo> orderInfo = new ArrayList<>();
        for (WebElement productElement : elements) {

            //상품정보
            WebElement infoElement = productElement.findElement(By.xpath(".//div[@class='row title font-italic text-capitalize']"));
            List<WebElement> dataList = infoElement.findElements(By.xpath(".//div[@class='col-5']"));
            String sku = dataList.get(1).getText();
            Map<String, String> sizeOrderMap = new HashMap<>();
            try {
                List<WebElement> sizeInfoElements = productElement.findElements(By.xpath(".//div[@class='row col-12 mb-2 mt-5 art']//div[@class='col-1 mr-1']"));
                for (WebElement sizeInfoElement : sizeInfoElements) {
                    String size = sizeInfoElement.findElement(By.xpath(".//div[@class='mr-1']")).getText();
                    WebElement inputElement = sizeInfoElement.findElement(By.xpath(".//input[@class='form-control form-control-sm qta-ord']"));
                    String orderNum = inputElement.getAttribute("placeholder");
                    sizeOrderMap.putIfAbsent(size, orderNum);
                    BufferedLog.info("STEP3 주문 SKU: {} , SIZE: {}, NUM : {}", sku, size, orderNum);
                }
            } catch (Exception e) {
                log.error("SIZE ORDER_NUM 정보 얻기 실패");
            }


            if (autoOrderRequestDto.getSku().equals(sku)) {
                isOrderSaved = true;
            }
            orderInfo.add(new OrderResultDto.OrderInfo(sku, sizeOrderMap));
        }
        Thread.sleep(2000);

        if (!isOrderSaved) {
            return new OrderResultDto(false, orderInfo);
        }

        BufferedLog.info("GNB STEP3 상품 쇼핑카트내 상품 들어있는거 확인");

        // 최종 확인 시정에 설정.
        boolean isDev = env.acceptsProfiles(Profiles.of("dev"));


        if (!isDev) {
            WebElement confirmButton = driver.findElement(By.id("confirm-order"));
            confirmButton.click();

            Thread.sleep(2000);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='swal2-popup swal2-modal swal2-icon-warning swal2-show']")));
            WebElement finalConfirmButton = driver.findElement(By.xpath("//button[@class='swal2-confirm swal2-styled swal2-default-outline']"));

            Thread.sleep(2000);

            finalConfirmButton.click();
        }

        BufferedLog.info("GNB STEP3 상품 쇼핑카트 내 상품 주문버튼 완료");

        return new OrderResultDto(true, orderInfo);

    }

    private List<String> findUnOrderCartingProduct(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) throws InterruptedException {

        String pattern = "\\S";
        Pattern p = Pattern.compile(pattern);
        wait.until(ExpectedConditions.textMatches(By.xpath("//div[@class='row title font-italic text-capitalize']//div[@class='col-5']"), p));
        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));
        List<String> unCartingProductSku = new ArrayList<>();
        for (WebElement productElement : elements) {

            //상품정보
            WebElement infoElement = productElement.findElement(By.xpath(".//div[@class='row title font-italic text-capitalize']"));
            List<WebElement> dataList = infoElement.findElements(By.xpath(".//div[@class='col-5']"));
            String sku = dataList.get(1).getText();

            if (!autoOrderRequestDto.getSku().equals(sku)) {
                BufferedLog.error("내가 선택한 상품아닌 다른 상품이 Cart에 존재함. SKU: {}", sku);
                unCartingProductSku.add(sku);
            }
        }

        return unCartingProductSku;
    }


    private void validateLogin(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {


        driver.get(autoOrderRequestDto.getProductLink());
        //로그인 체크
        if (driver.getCurrentUrl().equals("http://93.46.41.5:1995/login")) {
            login(driver, wait);
        }

    }

    public void login(ChromeDriver driver, WebDriverWait wait) {

        //로그인페이지 로그인
        driver.get("http://93.46.41.5:1995/login");

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("login error");
        }

        WebElement loginInput = driver.findElement(By.id("username"));
        loginInput.sendKeys(userId);
        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys(userPw);
        WebElement submitButton = driver.findElement(By.id("doLogin"));

        submitButton.click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("containerLineeModal")));
    }

    @Transactional
    public void updateOrderNum(long productId, long remainCount) {
        productRepository.updateOrderNum(productId, remainCount);
    }
}
