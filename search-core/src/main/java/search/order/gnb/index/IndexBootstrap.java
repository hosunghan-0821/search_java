package search.order.gnb.index;

import lombok.RequiredArgsConstructor;
import module.database.entity.ProductSkuToken;
import module.database.repository.ProductRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IndexBootstrap {
    private final ProductRepository productRepository;
    private final IndexHolder holder;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        //TODO Batch Size 만큼 조회해서 Load 고민
        List<ProductSkuToken> allSkuTokens = productRepository.findAllSkuTokens();

        List<Rule> rules = new ArrayList<>();
        Map<Long, Set<String>> productIdToRuleMap = new HashMap<>();
        for (ProductSkuToken productSkuToken : allSkuTokens) {
            if (productIdToRuleMap.containsKey(productSkuToken.getProduct().getId())) {
                productIdToRuleMap.get(productSkuToken.getProduct().getId()).add(productSkuToken.getToken());
            } else {
                productIdToRuleMap.put(productSkuToken.getProduct().getId(), new HashSet<>(Set.of(productSkuToken.getToken())));
            }
        }
        for (var entry : productIdToRuleMap.entrySet()) {
            rules.add(new Rule(entry.getKey(), entry.getValue()));
        }

        holder.loadInitial(rules);
    }
}
