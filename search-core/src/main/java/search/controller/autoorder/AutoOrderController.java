package search.controller.autoorder;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.gnb.GnbOrderManager;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class AutoOrderController {

    private final GnbOrderManager gnbOrderManager;

    @PostMapping("/order/products")
    public ResponseEntity<Boolean> orderGnbProduct(@RequestBody AutoOrderRequestDto autoOrderRequestDto) {

        if (!gnbOrderManager.validateProduct(autoOrderRequestDto)) {

            return ResponseEntity.ok(false);
        }
        gnbOrderManager.setValidSizes(autoOrderRequestDto);
        gnbOrderManager.orderProduct(autoOrderRequestDto);
        return ResponseEntity.ok(true);
    }
}
