package search.order.common.index;

import java.util.Set;

public record Rule(Long productId, Set<String> tokens) {
}
