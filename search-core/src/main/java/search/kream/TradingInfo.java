package search.kream;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class TradingInfo {

    private String option;
    private Long tradingPrice;
    private String tradingDate;
}
