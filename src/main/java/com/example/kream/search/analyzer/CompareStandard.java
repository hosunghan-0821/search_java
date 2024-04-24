package com.example.kream.search.analyzer;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CompareStandard {

    private MoneyUnit moneyUnit = MoneyUnit.EURO;

    private double standardRate = 10.0;

    private Double unitValue = 1400.0;


    protected void setMoneyUnit(MoneyUnit moneyUnit) {
        this.moneyUnit = moneyUnit;
    }

    protected void setStandardRate(double standardRate) {
        this.standardRate = standardRate;
    }

    protected void setUnitValue(Double unitValue) {
        this.unitValue = unitValue;
    }

}
