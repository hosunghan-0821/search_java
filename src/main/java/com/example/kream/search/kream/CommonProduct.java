package com.example.kream.search.kream;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class CommonProduct {

    //부띠끄
    private String sku;
    private String name;
    private String madeBy;
    private String monitoringSite;
    private String colorCode;
    private String imgUrl;
    private double price;
    private String unit;

    //kream 관련
    private String kreamImageUrl;
    private String tradingVolume;
    private String instantSalePrice;
    private String instantBuyPrice;
    private String averagePrice; //최근거래 평균가

    public void updateKreamInfo(String name, String tradingVolume, String instantSalePrice, String instantBuyPrice, String kreamImageUrl, String averagePrice) {
        this.name= name;
        this.tradingVolume = tradingVolume;
        this.instantSalePrice = instantSalePrice;
        this.instantBuyPrice = instantBuyPrice;
        this.kreamImageUrl = kreamImageUrl;
        this.averagePrice = averagePrice;
    }
}
