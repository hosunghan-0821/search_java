package search.controller.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import search.controller.search.dto.SearchRequestDto;
import search.kream.KreamSearchCore;
import search.kream.SearchProduct;

import java.io.IOException;
import java.util.List;

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
