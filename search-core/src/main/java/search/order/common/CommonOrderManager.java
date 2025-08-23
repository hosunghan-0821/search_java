package search.order.common;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import module.database.dto.Boutique;
import module.database.entity.Product;
import module.database.entity.ProductSize;
import module.database.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import search.controller.autoorder.dto.AutoOrderRequestDto;
import search.order.common.index.TokenEvaluator;
import search.order.gnb.GnbOrderManager;
import search.order.julian.JulianOrderManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommonOrderManager {


    private final TokenEvaluator tokenEvaluator;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final JulianOrderManager julianOrderManager;
    private final GnbOrderManager gnbOrderManager;

    /*
     * 토큰값이 유효한 것들을 갖옴
     * */
    public Long findTokenAllMatched(String sku) {
        List<Long> evaluateResult = tokenEvaluator.evaluate(sku);

        if (evaluateResult == null) {
            log.info("evaluateResult null 이유파악 필요");
            return -1L;
        }
        if (evaluateResult.isEmpty()) {
            return -1L;
        }

        evaluateResult.forEach(v -> System.out.println("sku : " + sku + "허용한 상품 ID : " + v));
        return evaluateResult.get(0);
    }


    @Transactional(readOnly = true)
    public boolean validateProduct(AutoOrderRequestDto autoOrderRequestDto) {

        assert (autoOrderRequestDto.getProductId() != null);
        Optional<Product> autoOrderProduct = productRepository.findById(autoOrderRequestDto.getProductId());
        if (autoOrderProduct.isPresent()) {
            Product product = autoOrderProduct.get();
            if (!autoOrderRequestDto.getBoutique().equals(product.getBoutique())) {
                //log.info("[Auto Order] - BOUTIQUE 값이 상이합니다 BOUTIQUE:{} SKU: {}", product.getBoutique(), autoOrderRequestDto.getSku());
                return false;
            }

            if (product.getPrice() == 0 || autoOrderRequestDto.getPrice() <= product.getPrice()) {
                return true;
            } else {
                notificationService.sendAutoOrderNotification("상품 주문 실패", autoOrderRequestDto, "지정가보다 높음", Color.RED, "FAIL", autoOrderRequestDto.getSku());
                log.info("[Auto Order] - 기준 가격보다 상품의 현재 가격이 높아서 주문하지 않습니다. sku : {}", autoOrderRequestDto.getSku());
                return false;
            }
        } else {
            log.info("[Auto Order] - DB에서 선정되지 않은 상품입니다 SKU: {}", autoOrderRequestDto.getSku());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void setValidSizesAndOrderNum(AutoOrderRequestDto autoOrderRequestDto) {
        Optional<Product> autoOrderProduct = productRepository.findById(autoOrderRequestDto.getProductId());
        if (autoOrderProduct.isPresent()) {
            List<ProductSize> productSizes = autoOrderProduct.get().getProductSize();
            List<String> validSizes = new ArrayList<>();
            for (ProductSize productSize : productSizes) {
                validSizes.add(productSize.getName());
            }
            autoOrderRequestDto.setValidSizes(validSizes);
            autoOrderRequestDto.setOrderNum(autoOrderProduct.get().getCount());
        }
    }

    public void sendNotificationService(String title, AutoOrderRequestDto autoOrderRequestDto, String errorMessage, Color color, String sendType) {
        notificationService.sendAutoOrderNotification(title, autoOrderRequestDto, errorMessage, color, sendType, autoOrderRequestDto.getSku());

    }

    public OrderManager getAutoOrderManagerOrNull(String boutique) {

        if (Boutique.JULIAN.getName().equals(boutique)) {
            return julianOrderManager;
        } else if (Boutique.GNB.getName().equals(boutique)) {
            return gnbOrderManager;
        }

        return null;
    }
}
