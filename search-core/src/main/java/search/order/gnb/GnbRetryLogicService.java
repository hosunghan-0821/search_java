package search.order.gnb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import search.common.log.BufferedLog;
import search.controller.autoorder.dto.AutoOrderRequestDto;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GnbRetryLogicService {

    @Retryable(retryFor = {Exception.class}, backoff = @Backoff(delay = 1000))
    public void deleteUnOrderCartingProduct(ChromeDriver driver, WebDriverWait wait, AutoOrderRequestDto autoOrderRequestDto, List<String> unOrderSkus) throws InterruptedException {

        for (String unOrderSku : unOrderSkus) {

            if (unOrderSku == null) {
                continue;
            }
            String pattern = "\\S";
            Pattern p = Pattern.compile(pattern);
            wait.until(ExpectedConditions.textMatches(By.xpath("//div[@class='row title font-italic text-capitalize']//div[@class='col-5']"), p));
            List<WebElement> elements = driver.findElements(By.xpath("//div[@class='shopping-cart mb-3']"));

            for (WebElement productElement : elements) {

                //상품정보
                WebElement infoElement = productElement.findElement(By.xpath(".//div[@class='row title font-italic text-capitalize']"));
                List<WebElement> dataList = infoElement.findElements(By.xpath(".//div[@class='col-5']"));
                String sku = dataList.get(1).getText();

                if (unOrderSku.equals(sku)) {
                    BufferedLog.error("내가 선택한 상품 아닌 다른 상품이 Cart에 존재 삭제. SKU: {}", sku);
                    WebElement buttonElement = productElement.findElement(By.xpath(".//button[@class='col-4 btn btn-sm btn-sirio mt-5 delete-item-cart']"));
                    buttonElement.click();

                    try {
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class='swal2-popup swal2-modal swal2-icon-success swal2-show']")));
                    } catch (Exception e) {
                        BufferedLog.error("아이템 삭제 모달 성공 나오기 전까지 대기 실패");
                    }
                    Thread.sleep(5000);
                    break;
                }
            }
        }
        driver.navigate().refresh();

    }
}
