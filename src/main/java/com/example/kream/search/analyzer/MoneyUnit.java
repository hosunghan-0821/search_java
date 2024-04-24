package com.example.kream.search.analyzer;

import lombok.Getter;

public enum MoneyUnit {

    EURO("유로"),
    DOLLAR("달러")

    ;

    MoneyUnit(String unit) {
        this.unit = unit;
    }


    @Getter
    private String unit;
}
