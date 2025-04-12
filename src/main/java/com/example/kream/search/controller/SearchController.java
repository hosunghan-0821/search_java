package com.example.kream.search.controller;

import com.example.kream.search.chrome.ChromeDriverTool;
import com.example.kream.search.chrome.ChromeDriverToolFactory;
import com.example.kream.search.dto.OrderRequestDto;
import com.example.kream.search.dto.SearchRequestDto;
import com.example.kream.search.kream.KreamSearchCore;
import com.example.kream.search.kream.SearchProduct;
import com.example.kream.search.order.gnb.GnbOrderManager;
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
import java.util.regex.Pattern;

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
