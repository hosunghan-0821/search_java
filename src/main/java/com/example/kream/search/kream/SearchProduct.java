package com.example.kream.search.kream;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class SearchProduct {

    //부띠끄
    private String sku;
    private String name;
    private String madeBy;
    private String monitoringSite;
    private String colorCode;
    private String imgUrl;
    private double inputPrice; // 해외 떼오는 가격
    private String unit;
    private boolean isFta;

    //kream 관련
    private String kreamModelNum;
    private String kreamProductId;
    private String kreamImageUrl;
    private String tradingVolume;
    private String instantSalePrice;
    private String instantBuyPrice;
    private double averagePrice; //최근거래 평균가

    public void updateKreamInfo(String name, String tradingVolume, String instantSalePrice, String instantBuyPrice, String kreamImageUrl, double averagePrice, String kreamModelNum) {

        this.kreamModelNum = kreamModelNum;
        this.name= name;
        this.tradingVolume = tradingVolume;
        this.instantSalePrice = instantSalePrice;
        this.instantBuyPrice = instantBuyPrice;
        this.kreamImageUrl = kreamImageUrl;
        this.averagePrice = averagePrice;
    }
}
