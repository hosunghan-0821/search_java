package search.order.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderResultDto {

    private boolean isOrderSaved;

    private List<OrderInfo> orders = new ArrayList<>();

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class OrderInfo {
        private String sku;
        private Map<String, String> sizeOrderNumMap;
    }
}
