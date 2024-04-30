package com.example.kream.search.kream;

import com.example.kream.search.analyzer.CompareData;
import com.example.kream.search.analyzer.CompareDataResult;
import com.example.kream.search.analyzer.PriceCompareCore;
import com.example.kream.search.chrome.ChromeDriverTool;
import com.example.kream.search.chrome.ChromeDriverToolFactory;
import com.example.kream.search.discord.DiscordBot;
import com.example.kream.search.discord.DiscordString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.kream.search.kream.KreamString.KREAM;

@Component
@RequiredArgsConstructor
@Slf4j
public class KreamSearchCore {

    private final ChromeDriverToolFactory chromeDriverToolFactory;

    private final PriceCompareCore compareCore;

    private final DiscordBot discordBot;

    private void searchProductLogic(ChromeDriver driver, WebDriverWait wait, List<SearchProduct> searchProductList) throws InterruptedException {
        //

        //로그인
        login(driver, wait);

        for (SearchProduct searchProduct : searchProductList) {
            log.info("상품검색 시작합니다." + searchProduct.getSku());
            SearchProduct productOrNull = null;
            try {
                productOrNull = findProductOrNull(driver, wait, searchProduct);
            } catch (Exception e) {
                log.error("상품 검색 에러");
            }

            if (productOrNull == null) {
                continue;
            } else if (productOrNull.getTradingVolume() == null) {
                log.error(searchProduct.getSku() + " 거래량 없음");
                continue;
            }
            CompareDataResult compareDataResult = compareProduct(productOrNull);
            //기준이 넘는 경우에만 디스코드 알람
            TextChannel compareChannel = discordBot.getJda().getChannelById(TextChannel.class, DiscordString.KREAM_COMPARE_CHANNEL);
            TextChannel allCompareChannel = discordBot.getJda().getChannelById(TextChannel.class, DiscordString.KREAM_COMPARE_ALL_CHANNEL);
            if (compareDataResult.isPassStandard()) {
                log.info("수익 기준 넘는 제품 등장" + searchProduct);
                discordBot.getBotCommands().sendSearchAndCompareReport(compareChannel, searchProduct, compareDataResult);
                discordBot.getBotCommands().sendSearchAndCompareReport(allCompareChannel, searchProduct, compareDataResult);
            } else {
                log.info("수익률 안넘음  상품정보 : " + searchProduct + "예상 수익률" + compareDataResult.getDifferenceRate());
                discordBot.getBotCommands().sendSearchAndCompareReport(allCompareChannel, searchProduct, compareDataResult);
            }

            Thread.sleep(500);
        }
    }

    @Async
    public void searchProductOrNull(List<SearchProduct> searchProductList, String monitoringSite) {

        ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.getChromeDriverTool(monitoringSite);
        ChromeDriver driver = chromeDriverTool.getChromeDriver();
        WebDriverWait wait = chromeDriverTool.getWebDriverWait();
        ReentrantLock reentrantLock = chromeDriverTool.getReentrantLock();

        //크롬창이 닫혀있더라도 작동
        if (!chromeDriverTool.checkWindowIsClosed(driver)) {
            if (reentrantLock.isLocked()) {
                log.info(monitoringSite + " chrome driver 다른 제품들 검사중..");
            }
            try {
                boolean getLock = reentrantLock.tryLock(60, TimeUnit.SECONDS);
                if (!getLock) {
                    log.error(monitoringSite + "락 획득 실패");
                    return;
                }
                searchProductLogic(driver, wait, searchProductList);
            } catch (InterruptedException e) {
                log.error(monitoringSite + " Lock 획득 실패");
            } finally {
                reentrantLock.unlock();
            }
        } else {
            driver = new ChromeDriver(chromeDriverToolFactory.getChromeOptions());
            wait = new WebDriverWait(driver, Duration.ofMillis(1500)); // 최대 5초 대기

            try {
                searchProductLogic(driver, wait, searchProductList);
            } catch (Exception e) {
                e.printStackTrace();
            }

            driver.quit();
        }


    }

    public SearchProduct searchProductOrNull(SearchProduct findProduct) {

        ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.getChromeDriverTool(KREAM);

        ChromeDriver driver;
        WebDriverWait wait;
        boolean makeNewDriver = false;

        if (!chromeDriverTool.checkWindowIsClosed(chromeDriverTool.getChromeDriver()) && !chromeDriverTool.isRunning()) {
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
            Thread.sleep(3000);
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


                WebElement productElement = productElements.get(0);

                WebElement imageElement = productElement.findElement(By.xpath(".//picture[@class='picture product_img']//img"));
                imageUrl = imageElement.getAttribute("src");

                String kreamProductId = productElement.getAttribute("data-product-id");

                try {
                    WebElement tradeData = driver.findElement(By.xpath("//div[@class='status_value']"));
                    tradingVolume = tradeData.getText();
                } catch (Exception e) {
                    return searchProduct;
                }

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
                searchProduct.updateKreamInfo(name, tradingVolume, instantSalePrice, instantBuyPrice, imageUrl, averagePrice, modelNum);

            }
        } catch (Exception e) {
            log.error("상품 검색 에러 "+searchProduct.toString());
            //상풒 상세정보 모두 적어주기
            return null;
        }

        return searchProduct;
    }

    public CompareDataResult compareProduct(SearchProduct searchResultProduct) {

        CompareData compareData = CompareData.builder()
                .searchAveragePrice(searchResultProduct.getAveragePrice())
                .inputPrice(searchResultProduct.getInputPrice())
                .isFtaProduct(searchResultProduct.getFta())
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
