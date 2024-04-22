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

        driver.get("https://www.kream.co.kr/login");
        WebElement loginFormElement = driver.findElement(By.xpath("//input[@type='email']"));
        WebElement passwordFormElement = driver.findElement(By.xpath("//input[@type='password']"));

        loginFormElement.sendKeys("winsomed96@naver.com");
        passwordFormElement.sendKeys("Hosung1194!");
        WebElement loginButtonElement = driver.findElement(By.xpath("//div[@class='login_btn_box']/a[@class='btn full solid']"));
        loginButtonElement.click();

        Thread.sleep(1000);

        String productSKU = "id8349";
        driver.get("https://www.kream.co.kr/search?keyword=" + productSKU);

        try {
            WebElement topDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='search_result_list']")));
            List<WebElement> productElements = topDiv.findElements(By.xpath(".//div[@class='search_result_item product']"));
            if (!productElements.isEmpty()) {
                WebElement productElement = productElements.get(0);
                String kreamProductId = productElement.getAttribute("data-product-id");
                driver.get("https://www.kream.co.kr/products/" + kreamProductId);
            }
        } catch (Exception e) {
            //상풒 상세정보 모두 적어주기
            log.error(productSKU+" : 품번 오류");
        }


    }
}
