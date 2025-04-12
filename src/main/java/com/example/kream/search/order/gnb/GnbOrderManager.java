package com.example.kream.search.order.gnb;

import com.example.kream.search.chrome.ChromeDriverTool;
import com.example.kream.search.chrome.ChromeDriverToolFactory;
import com.example.kream.search.dto.OrderRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class GnbOrderManager {

    private final GnbOrderService gnbOrderService;
    private final ChromeDriverToolFactory chromeDriverToolFactory;

    /*
     *
     *
     * */
    public void orderProduct(OrderRequestDto orderRequestDto) {

        // ChromeDriver 할당받기 from DriverPool (sync)

        // OrderProcess By Async

        //우선 동기적으로 작성하고 비동기적으로 돌도록 하자.
        chromeDriverToolFactory.makeChromeDriverTool("GNB");
        ChromeDriverTool chromeDriverTool = chromeDriverToolFactory.getChromeDriverTool("GNB");

        ChromeDriver driver = chromeDriverTool.getChromeDriver();
        WebDriverWait wait = chromeDriverTool.getWebDriverWait();

        gnbOrderService.step1(driver, wait, orderRequestDto);
        gnbOrderService.step2(driver, wait, orderRequestDto);
        gnbOrderService.step3(driver, wait, orderRequestDto);

        //Discord Notice 어떤 상품 자동주문 진행시켰는지 Noti


    }

    public boolean validateProduct(OrderRequestDto orderRequestDto) {
        //DB에서 유효한 상품과 Size인지 확인

        return true;
    }
}
