package search.controller.search.dto;

import search.kream.SearchProduct;
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
