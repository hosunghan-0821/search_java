package search.controller.autoorder.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AutoOrderRequestDto {

    private String boutique;
    private String price;
    private String sku;
    private String id;
    private String productLink;
}
