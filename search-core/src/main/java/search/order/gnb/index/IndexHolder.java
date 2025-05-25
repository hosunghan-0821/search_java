package search.order.gnb.index;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Getter
public class IndexHolder {

    private final AtomicReference<Trie> main = new AtomicReference<>();
    private final Map<Integer, Long> indexToProductId = new HashMap<>();
    private final Map<String, List<Integer>> tokenToRules = new HashMap<>();
    private final List<Long> productIds = new ArrayList<>();
    private List<Rule> rules = new ArrayList<>();

    /* 부팅 시 */
    public void loadInitial( List<Rule> rules) {
        this.rules = rules;
        /* 2-1. 이름/인덱스 매핑 */
        for (int i = 0; i < rules.size(); i++) {
            productIds.add(rules.get(i).productId());
            indexToProductId.put(i, rules.get(i).productId());
        }

        /* 2-2. 토큰 역-색인 */
        Set<String> allTokens = new HashSet<>();

        for (int idx = 0; idx < rules.size(); idx++) {
            for (String raw : rules.get(idx).tokens()) {
                String tok = raw.toUpperCase();
                allTokens.add(tok);

                tokenToRules
                        .computeIfAbsent(tok, k -> new ArrayList<>())
                        .add(idx);                      // ArrayList<Integer>
            }
        }

        main.set(AcBuilder.build(allTokens));
    }
}
