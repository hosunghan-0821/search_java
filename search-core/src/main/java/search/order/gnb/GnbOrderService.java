package search.order.gnb;

import org.springframework.retry.annotation.Recover;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service

@RequiredArgsConstructor
public class GnbOrderService {


    @Value("${gebenegozi.user.id}")
    private String userId;

    @Value("${gebenegozi.user.pw}")
    private String userPw;


    /*
     * 상품 페이지 이동
     * */
    @Retryable(retryFor = {TimeoutException.class}, backoff = @Backoff(delay = 1000))
    public void step1(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        log.info("GNB STEP1 상품 페이지 이동 START");
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
        log.info("GNB STEP1 상품 페이지 이동 SUCCESS");
    }


    /*
     *
     * 상품 검색 쇼핑카트 등록
     * */
    @Retryable(retryFor = {TimeoutException.class}, backoff = @Backoff(delay = 1000))
    public void step2(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        log.info("GNB STEP2 상품 검색 쇼핑카트 등록 START");
        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));

        //상품페이지 검색 Step2
        for (WebElement productElement : elements) {

            //상품정보
            WebElement infoElement = productElement.findElement(By.xpath(".//div[@class='row title font-italic text-capitalize artPrezzi']"));
            List<WebElement> dataList = infoElement.findElements(By.xpath(".//div[@class='col-5']"));
            String sku = dataList.get(1).getText();

            // 원하는 값들 찾으면
            if (autoOrderRequestDto.getSku().equals(sku)) {
                Map<String, Integer> orderSizeMap = new HashMap<>();

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
                        log.info("해당하는 Size가 아니므로 넘깁니다 :{}", size);
                        continue;
                    }
                    WebElement inputElement = sizeElement.findElement(By.xpath(".//div[@class='artCod']//input"));
                    String orderNum = inputElement.getAttribute("placeholder");
                    log.debug("수량 : {}", orderNum);
                    if (orderNum.equals("0")) {
                        log.error("수량: 0개 이므로 주문을 할 수 없습니다. SKU : {} \t Product Link : {}", sku, autoOrderRequestDto.getProductLink());
                        continue;
                    }
                    inputElement.sendKeys(orderNum);
                    orderSizeMap.put(size, Integer.valueOf(orderNum));
                }
                if (orderSizeMap.isEmpty()) {
                    throw new RuntimeException("No Order Size");
                }

                log.info("원하는 상품 찾음");

                WebElement buttonElement = productElement.findElement(By.xpath(".//button[@class='btn btn-sm btn-sirio add-item-cart mt-3 ml-5']"));
                buttonElement.click();

                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='swal2-popup swal2-modal swal2-icon-success swal2-show']")));
                } catch (Exception e) {
                    String text = productElement.findElement(By.xpath("//div[@class='col-12 mb-2 font-weight-bold']")).getText();
                    if (text.equals("Items in cart")) {
                        log.info("GNB STEP2 상품 검색 쇼핑카트 등록 FINISH");
                        return;
                    } else {
                        break;
                    }

                }

                log.info("GNB STEP2 상품 검색 쇼핑카트 등록 FINISH");
                return;
            }
        }
        log.error("GNB STEP2 상품 존재하지 않음  sku : {}", autoOrderRequestDto.getSku());

    }

    /*
     * 쇼핑 카드 등록한 곳에서 주문 확정
     * */
    @Retryable(retryFor = {TimeoutException.class}, backoff = @Backoff(delay = 1000))
    public void step3(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto) {

        driver.get("http://93.46.41.5:1995/cart");

        String pattern = "\\S";
        Pattern p = Pattern.compile(pattern);
        wait.until(ExpectedConditions.textMatches(By.xpath("//div[@class='row title font-italic text-capitalize']//div[@class='col-5']"), p));

        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));

        boolean isOrderSaved = false;
        //상품 존재하는지 확인.

        for (WebElement productElement : elements) {

            //상품정보
            WebElement infoElement = productElement.findElement(By.xpath(".//div[@class='row title font-italic text-capitalize']"));
            List<WebElement> dataList = infoElement.findElements(By.xpath(".//div[@class='col-5']"));
            String sku = dataList.get(1).getText();

            if (autoOrderRequestDto.getSku().equals(sku)) {
                isOrderSaved = true;
                break;
            }
        }

        if (isOrderSaved) {
            WebElement confirmButton = driver.findElement(By.id("confirm-order"));
            confirmButton.click();
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='swal2-popup swal2-modal swal2-icon-warning swal2-show']")));
        WebElement finalConfirmButton = driver.findElement(By.xpath("//button[@class='swal2-confirm swal2-styled swal2-default-outline']"));

        // 최종 확인 시정에 설정.
        finalConfirmButton.click();

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
}
