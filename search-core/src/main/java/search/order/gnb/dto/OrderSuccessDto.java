package search.order.gnb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderSuccessDto {

    private String sku;
    private Map<String, String> sizeOrderNumMap;
}
