package search.controller;

import search.dto.OrderRequestDto;
import search.dto.SearchRequestDto;
import search.kream.KreamSearchCore;
import search.kream.SearchProduct;
import search.order.gnb.GnbOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class SearchController {

    private final KreamSearchCore kreamSearchCore;
    private final GnbOrderManager gnbOrderManager;

    @PostMapping("/search/products")
    public ResponseEntity<?> searchProduct(@RequestBody SearchRequestDto searchRequestDto) throws IOException {

        log.info(searchRequestDto.getMonitoringSite());
        List<SearchProduct> searchProductList = searchRequestDto.getData();

        kreamSearchCore.searchProductOrNull(searchProductList, searchRequestDto.getMonitoringSite());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/order/products")
    public void orderGnbProduct(@RequestBody OrderRequestDto orderRequestDto) {

        // 주문 시작
        if(!gnbOrderManager.validateProduct(orderRequestDto)){
            // 해당하지 않는 상품
            log.error("DB에서 선정되지 않은 상품입니다 SKU: {}",orderRequestDto.getSku());
        }
        gnbOrderManager.orderProduct(orderRequestDto);

    }

}
