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
import search.order.common.CommonOrderManager;
import search.order.common.OrderManager;
import search.order.common.index.IndexBootstrap;
import search.order.julian.JulianOrderManager;

import java.awt.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class AutoOrderController {


    private final CommonOrderManager commonOrderManager;
    private final IndexBootstrap indexBootstrap;
    private final JulianOrderManager julianOrderManager;

    @PostMapping("/order/products/bulk")
    public ResponseEntity<Boolean> orderGnbProduct(@RequestBody List<AutoOrderRequestDto> autoOrderRequestDtoList) {

        for (AutoOrderRequestDto autoOrderRequestDto : autoOrderRequestDtoList) {
            Long validProductId = commonOrderManager.findTokenAllMatched(autoOrderRequestDto.getSku());
            if (validProductId == -1L) {
                log.info("품번에 해당하는 토큰집합이 없습니다. SKU: {}", autoOrderRequestDto.getSku());
                continue;
            }
            autoOrderRequestDto.setProductId(validProductId);
            if (!commonOrderManager.validateProduct(autoOrderRequestDto)) {
                continue;
            }
            commonOrderManager.setValidSizesAndOrderNum(autoOrderRequestDto);
            if (autoOrderRequestDto.getOrderNum() == 0) {
                commonOrderManager.sendNotificationService("상품 주문 실패", autoOrderRequestDto, "재고 0", Color.RED, "FAIL");
                continue;
            }
            // Boutique 맞춰서 OrderManager 가져와서 처리
            OrderManager autoOrderManagerOrNull = commonOrderManager.getAutoOrderManagerOrNull(autoOrderRequestDto.getBoutique());
            if (autoOrderManagerOrNull != null) {
                autoOrderManagerOrNull.orderProduct(autoOrderRequestDto);
            } else {
                log.error("CANNOT FIND AUTO ORDER MANAGER :{}", autoOrderRequestDto.getBoutique());
            }

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
        Long validProductId = commonOrderManager.findTokenAllMatched(sku);

        return ResponseEntity.ok(validProductId);
    }

    @GetMapping("/order/trie/sync")
    public ResponseEntity<Boolean> syncDbTrie() {
        indexBootstrap.init();
        return ResponseEntity.ok(true);
    }
}
