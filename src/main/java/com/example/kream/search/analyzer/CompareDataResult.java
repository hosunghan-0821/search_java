package com.example.kream.search.analyzer;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CompareDataResult {

    private double finalPrice;
    private MoneyUnit moneyUnit;
    private boolean isFtaProduct;
    private double differenceRate;
    private double unitValue;
    private boolean isPassStandard;
}
