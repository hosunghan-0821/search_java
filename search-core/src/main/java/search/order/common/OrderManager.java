package search.order.common;

import search.controller.autoorder.dto.AutoOrderRequestDto;

public interface OrderManager {

    void orderProduct(AutoOrderRequestDto autoOrderRequestDto);
}
