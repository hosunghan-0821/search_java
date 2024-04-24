package com.example.kream.search.analyzer;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CompareStandard {

    private MoneyUnit moneyUnit = MoneyUnit.EURO;

    private double standardRate = 10.0;

    private double unitValue = 1400.0;


    public void setMoneyUnit(MoneyUnit moneyUnit) {
        this.moneyUnit = moneyUnit;
    }

    public void setStandardRate(double standardRate) {
        this.standardRate = standardRate;
    }

    public void setUnitValue(double unitValue) {
        this.unitValue = unitValue;
    }

}
