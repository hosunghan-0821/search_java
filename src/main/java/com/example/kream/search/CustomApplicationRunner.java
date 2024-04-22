package com.example.kream.search;


import com.example.kream.search.chrome.ChromeDriverTool;
import com.example.kream.search.chrome.ChromeDriverToolFactory;
import com.example.kream.search.discord.DiscordBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.kream.search.kream.KreamString.KREAM;


@Slf4j
@Component
@RequiredArgsConstructor
public class CustomApplicationRunner implements ApplicationRunner {

    private final ChromeDriverToolFactory chromeDriverToolFactory;


    private final DiscordBot discordBot;

    @Override
    public void run(ApplicationArguments args) throws Exception {


//        chromeDriverToolFactory.makeChromeDriverTool(DOUBLE_F);
//        chromeDriverToolFactory.makeChromeDriverTool(ALL_CATEGORIES);
//        chromeDriverToolFactory.makeChromeDriverTool(PROMO);

        chromeDriverToolFactory.makeChromeDriverTool(KREAM);


        ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.getChromeDriverTool(KREAM);
        ChromeDriver driver = chromeDriverTool.getChromeDriver();
        WebDriverWait wait = chromeDriverTool.getWebDriverWait();


        //로그인
        driver.get("https://www.kream.co.kr/login");
        WebElement loginFormElement = driver.findElement(By.xpath("//input[@type='email']"));
        WebElement passwordFormElement = driver.findElement(By.xpath("//input[@type='password']"));

        loginFormElement.sendKeys("winsomed96@naver.com");
        passwordFormElement.sendKeys("Hosung1194!");
        WebElement loginButtonElement = driver.findElement(By.xpath("//div[@class='login_btn_box']/a[@class='btn full solid']"));
        loginButtonElement.click();

        Thread.sleep(1000);

        //품번 통해서  상품 검색 및 판매가 확인
        String productSKU = "id8349";

        //검색 시도
        driver.get("https://www.kream.co.kr/search?keyword=" + productSKU);

        try {
            //상품 결과
            WebElement topDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='search_result_list']")));
            WebElement tradeData = driver.findElement(By.xpath("//div[@class='status_value']"));

            log.info("거래량 " + tradeData.getText());

            List<WebElement> productElements = topDiv.findElements(By.xpath(".//div[@class='search_result_item product']"));
            if (!productElements.isEmpty()) {
                WebElement productElement = productElements.get(0);
                String kreamProductId = productElement.getAttribute("data-product-id");
                driver.get("https://www.kream.co.kr/products/" + kreamProductId);

                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@class='btn_action']//div[@class='price']")));
                List<WebElement> elements = driver.findElements(By.xpath("//button[@class='btn_action']//div[@class='price']"));

                //즉시 구매가
                log.info(elements.get(0).getText().split("\n")[0]);
                //즉시 판매가
                log.info(elements.get(1).getText().split("\n")[0]);
            }
        } catch (Exception e) {
            //상풒 상세정보 모두 적어주기
            log.error(productSKU+" : 품번 오류");
        }


    }
}
