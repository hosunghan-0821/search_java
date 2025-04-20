package search.analyzer;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CompareData {

    private double inputPrice;
    private MoneyUnit moneyUnit;
    private Boolean isFtaProduct;
    private double searchAveragePrice;
    private double resultRate;
}
