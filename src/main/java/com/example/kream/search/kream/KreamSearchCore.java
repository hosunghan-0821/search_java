package com.example.kream.search.kream;

import com.example.kream.search.analyzer.CompareData;
import com.example.kream.search.analyzer.CompareDataResult;
import com.example.kream.search.analyzer.MoneyUnit;
import com.example.kream.search.analyzer.PriceCompareCore;
import com.example.kream.search.chrome.ChromeDriverTool;
import com.example.kream.search.chrome.ChromeDriverToolFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.kream.search.kream.KreamString.KREAM;

@Component
@RequiredArgsConstructor
@Slf4j
public class KreamSearchCore {

    private final ChromeDriverToolFactory chromeDriverToolFactory;

    private final PriceCompareCore compareCore;


    public SearchProduct searchProductOrNull(SearchProduct findProduct) {

        ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.getChromeDriverTool(KREAM);

        ChromeDriver driver;
        WebDriverWait wait;
        boolean makeNewDriver = false;

        if (!chromeDriverTool.isRunning()) {
            chromeDriverTool.isRunning(true);
            driver = chromeDriverTool.getChromeDriver();
            wait = chromeDriverTool.getWebDriverWait();
        } else {
            makeNewDriver = true;
            driver = new ChromeDriver(chromeDriverToolFactory.getChromeOptions());
            wait = new WebDriverWait(driver, Duration.ofMillis(5000)); // 최대 5초 대기
        }


        login(driver, wait);
        SearchProduct productOrNull = findProductOrNull(driver, wait, findProduct);

        if (makeNewDriver) {
            driver.quit();
        } else {
            chromeDriverTool.isRunning(false);
        }

        return productOrNull;
    }

    public void login(ChromeDriver driver, WebDriverWait wait) {
        //로그인
        driver.get("https://www.kream.co.kr/login");

        if (driver.getCurrentUrl().equals("https://www.kream.co.kr/")) {
            return;
        }

        WebElement loginFormElement = driver.findElement(By.xpath("//input[@type='email']"));
        WebElement passwordFormElement = driver.findElement(By.xpath("//input[@type='password']"));

        loginFormElement.sendKeys("winsomed96@naver.com");
        passwordFormElement.sendKeys("Hosung1194!");
        WebElement loginButtonElement = driver.findElement(By.xpath("//div[@class='login_btn_box']/a[@class='btn full solid']"));
        loginButtonElement.click();

        //로그인하고 잠시 정지
        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        }
    }

    public SearchProduct findProductOrNull(ChromeDriver driver, WebDriverWait wait, SearchProduct searchProduct) {

        driver.get("https://www.kream.co.kr/search?keyword=" + searchProduct.getSku());

        String name;
        String tradingVolume;
        String instantSalePrice;
        String instantBuyPrice;
        String imageUrl;
        String modelNum;

        try {
            //상품 결과
            WebElement topDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='search_result_list']")));

            List<WebElement> productElements = topDiv.findElements(By.xpath(".//div[@class='search_result_item product']"));
            if (!productElements.isEmpty()) {

                WebElement nameData = driver.findElement(By.xpath("//p[@class='name']"));
                name = nameData.getText();

                WebElement tradeData = driver.findElement(By.xpath("//div[@class='status_value']"));
                tradingVolume = tradeData.getText();

                WebElement productElement = productElements.get(0);

                WebElement imageElement = productElement.findElement(By.xpath(".//picture[@class='picture product_img']//img"));
                imageUrl = imageElement.getAttribute("src");

                String kreamProductId = productElement.getAttribute("data-product-id");

                //상세페이지 이동
                driver.get("https://www.kream.co.kr/products/" + kreamProductId);

                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@class='btn_action']//div[@class='price']")));
                List<WebElement> elements = driver.findElements(By.xpath("//button[@class='btn_action']//div[@class='price']"));

                WebElement getTradingData = driver.findElement(By.xpath("//div[@id='panel1']//a[@class='btn outlinegrey full medium']"));
                //즉시 구매가
                instantBuyPrice = elements.get(0).getText().split("\n")[0];

                //즉시 판매가
                instantSalePrice = elements.get(1).getText().split("\n")[0];

                //모델번호 가져오기
                WebElement modelElement = driver.findElement(By.xpath("//div[@class='product_title' and contains(text(), '모델번호')]/.."));
                modelNum = modelElement.findElement(By.xpath(".//div[@class='product_info']")).getText();

                //거래량 가져오기
                Actions actions = new Actions(driver);
                actions.moveToElement(getTradingData);
                actions.click();
                actions.perform();

                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='layer_container']//div[@class='price_body']//div[@class='body_list']")));
                List<WebElement> tradingElements = driver.findElements(By.xpath("//div[@class='layer_container']//div[@class='price_body']//div[@class='body_list']"));
                List<TradingInfo> tradingInfoDataList = new ArrayList<>();

                for (WebElement tradingElement : tradingElements) {
                    List<WebElement> tradingElementList = tradingElement.findElements(By.xpath(".//div[@class='list_txt']"));
                    String option = "";
                    Long tradingPrice = 0L;
                    String tradingDate = "";
                    if (tradingElementList.size() == 2) {

                        WebElement optionElement = tradingElementList.get(0).findElement(By.xpath(".//span"));
                        option = optionElement.getText();

                        WebElement priceElement = tradingElementList.get(1).findElement(By.xpath(".//span"));
                        Long price = Long.parseLong(priceElement.getText().replaceAll(",", "").replace("원", ""));
                        tradingPrice = price;
                    }

                    List<WebElement> dateElements = tradingElement.findElements(By.xpath(".//div[@class='list_txt is_active']"));
                    if (dateElements.size() == 1) {
                        WebElement dateElement = dateElements.get(0).findElement(By.xpath(".//span"));
                        tradingDate = dateElement.getText();
                    }

                    TradingInfo tradingInfo = TradingInfo.builder()
                            .option(option)
                            .tradingPrice(tradingPrice)
                            .tradingDate(tradingDate)
                            .build();
                    tradingInfoDataList.add(tradingInfo);
                }


                int averagePrice = getAveragePrice(tradingInfoDataList);
                searchProduct.updateKreamInfo(name, tradingVolume, instantSalePrice, instantBuyPrice, imageUrl, averagePrice,modelNum);

            }
        } catch (Exception e) {
            //상풒 상세정보 모두 적어주기
            log.error(searchProduct.getSku() + " : 품번 오류");
            return null;
        }

        return searchProduct;
    }

    public CompareDataResult compareProduct(SearchProduct searchResultProduct) {

        CompareData compareData = CompareData.builder()
                .searchAveragePrice(searchResultProduct.getAveragePrice())
                .inputPrice(searchResultProduct.getInputPrice())
                .moneyUnit(MoneyUnit.valueOf(searchResultProduct.getUnit()))
                .isFtaProduct(searchResultProduct.isFta()) //TO-DO 나중에 바꿔야함
                .build();

        return compareCore.compare(compareData);

    }


    private int getAveragePrice(List<TradingInfo> tradingInfoDataList) {

        int count = 1;
        int sum = 1;

        for (TradingInfo tradingInfo : tradingInfoDataList) {
            if (count > 5) {
                break;
            }
            sum += tradingInfo.getTradingPrice();
            count++;
        }

        int averagePrice = sum / count;
        return averagePrice;
    }
}
