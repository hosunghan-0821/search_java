package search.analyzer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceCompareCore {

    private final CompareStandard compareStandard;

    public CompareDataResult compare(CompareData compareData) {


        double searchAveragePrice = compareData.getSearchAveragePrice();
        double finalPrice = getFinalPrice(compareData);
        double differenceRate = ((searchAveragePrice * 0.945) - finalPrice) / finalPrice * 100;
        differenceRate = Math.round(differenceRate * 100.0) / 100.0;

        boolean isPassStandard = false;
        if (differenceRate > compareStandard.getStandardRate()) {
            isPassStandard = true;
        }
        CompareDataResult compareDataResult = CompareDataResult
                .builder()
                .differenceRate(differenceRate)
                .finalPrice(finalPrice)
                .isFtaProduct(compareData.getIsFtaProduct())
                .unitValue(compareStandard.getUnitValue())
                .isPassStandard(isPassStandard)
                .build();

        return compareDataResult;

    }

    private double getFinalPrice(CompareData compareData) {

        double changeToWon = compareData.getInputPrice() * compareStandard.getUnitValue();

        double finalPrice;
        if (compareData.getIsFtaProduct()) {
            finalPrice = changeToWon * 1.1;
        } else {
            finalPrice = changeToWon * 1.24;
        }

        return finalPrice;
    }
}
