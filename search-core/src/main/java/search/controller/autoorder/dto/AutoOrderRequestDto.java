package search.controller.autoorder.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AutoOrderRequestDto {

    @Setter
    private Long productId;
    private String boutique;
    private double price;
    private String sku;
    private String id;
    private String productLink;
    @Setter
    private long orderNum;
    @Setter
    private List<String> validSizes;
}
