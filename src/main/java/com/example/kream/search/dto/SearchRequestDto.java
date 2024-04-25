package com.example.kream.search.dto;

import com.example.kream.search.kream.SearchProduct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Slf4j
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestDto {

    private String monitoringSite;
    private List<SearchProduct> data;
}
