package com.example.kream.search.controller;

import com.example.kream.search.dto.SearchRequestDto;
import com.example.kream.search.kream.KreamSearchCore;
import com.example.kream.search.kream.SearchProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class SearchController {

    private final KreamSearchCore kreamSearchCore;

    @PostMapping("/search/products")
    public ResponseEntity<?> searchProduct(@RequestBody SearchRequestDto searchRequestDto) throws IOException {

        log.info(searchRequestDto.getMonitoringSite());
        List<SearchProduct> searchProductList = searchRequestDto.getData();

        kreamSearchCore.searchProductOrNull(searchProductList, searchRequestDto.getMonitoringSite());

        return new ResponseEntity<>(HttpStatus.OK);
    }


}
