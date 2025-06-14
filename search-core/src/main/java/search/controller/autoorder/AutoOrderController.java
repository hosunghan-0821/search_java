package search.controller.autoorder;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.gnb.GnbOrderManager;
import search.order.gnb.index.IndexBootstrap;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class AutoOrderController {

    private final GnbOrderManager gnbOrderManager;
    private final IndexBootstrap indexBootstrap;

    @PostMapping("/order/products")
    public ResponseEntity<Boolean> orderGnbProduct(@RequestBody AutoOrderRequestDto autoOrderRequestDto) {

        if (!gnbOrderManager.validateProduct(autoOrderRequestDto)) {

            return ResponseEntity.ok(false);
        }
        gnbOrderManager.setValidSizesAndOrderNum(autoOrderRequestDto);
        gnbOrderManager.orderProduct(autoOrderRequestDto);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/order/products/bulk")
    public ResponseEntity<Boolean> orderGnbProduct(@RequestBody List<AutoOrderRequestDto> autoOrderRequestDtoList) {

        for (AutoOrderRequestDto autoOrderRequestDto : autoOrderRequestDtoList) {
            Long validProductId = gnbOrderManager.findTokenAllMatched(autoOrderRequestDto);
            if (validProductId == -1L) {
                log.debug("품번에 해당하는 토큰집합이 없습니다. SKU: {}", autoOrderRequestDto.getSku());
                continue;
            }
            autoOrderRequestDto.setProductId(validProductId);
            if (!gnbOrderManager.validateProduct(autoOrderRequestDto)) {
                continue;
            }
            gnbOrderManager.setValidSizesAndOrderNum(autoOrderRequestDto);
            gnbOrderManager.orderProduct(autoOrderRequestDto);
        }
        return ResponseEntity.ok(true);
    }

    /**
     * 유효한 SKU인지 확인
     *
     * @param sku 난수품번
     * @return productId 상품ID / -1L존재X
     */
    @GetMapping("/order/products/sku")
    public ResponseEntity<Long> getValidProductId(@RequestParam String sku) {
        Long validProductId = gnbOrderManager.findTokenAllMatched(AutoOrderRequestDto.builder().sku(sku).build());

        return ResponseEntity.ok(validProductId);
    }

    @GetMapping("/order/trie/sync")
    public ResponseEntity<Boolean> syncDbTrie(){
        indexBootstrap.init();
        return ResponseEntity.ok(true);
    }
}
