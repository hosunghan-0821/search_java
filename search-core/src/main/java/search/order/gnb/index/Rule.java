package search.order.gnb.index;

import java.util.Set;

public record Rule(Long productId, Set<String> tokens) {
}
