package search.order.gnb.index;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Getter
public class IndexHolder {

    /**
     * 한 번에 스왑되는 전체 스냅샷
     */
    private final AtomicReference<Snapshot> ref = new AtomicReference<>();

//    private final AtomicReference<Trie> main = new AtomicReference<>();
//    private final Map<Integer, Long> indexToProductId = new HashMap<>();
//    private final Map<String, List<Integer>> tokenToRules = new HashMap<>();
//    private final List<Long> productIds = new ArrayList<>();
//    private List<Rule> rules = new ArrayList<>();

    /* ------------  조회용 API  ------------ */
    public Trie trie()                   { return ref.get().trie(); }
    public Map<Integer, Long> indexMap() { return ref.get().indexToProductId(); }
    public Map<String, List<Integer>> invertedIndex() { return ref.get().tokenToRules(); }
    public List<Long> productIds()       { return ref.get().productIds(); }
    public List<Rule> rules()            { return ref.get().rules(); }

    /* 부팅 시 */
    public void loadInitial(List<Rule> rules) {
        Snapshot next = Snapshot.build(rules);
        ref.set(next);   // 단 한 줄로 원자적 교체

//        this.rules = rules;
//        /* 2-1. 이름/인덱스 매핑 */
//        for (int i = 0; i < rules.size(); i++) {
//            productIds.add(rules.get(i).productId());
//            indexToProductId.put(i, rules.get(i).productId());
//        }
//
//        /* 2-2. 토큰 역-색인 */
//        Set<String> allTokens = new HashSet<>();
//
//        for (int idx = 0; idx < rules.size(); idx++) {
//            for (String raw : rules.get(idx).tokens()) {
//                String tok = raw.toUpperCase();
//                allTokens.add(tok);
//
//                tokenToRules
//                        .computeIfAbsent(tok, k -> new ArrayList<>())
//                        .add(idx);                      // ArrayList<Integer>
//            }
//        }
//
//        main.set(AcBuilder.build(allTokens));
    }

    public record Snapshot(Trie trie,
                           Map<Integer, Long> indexToProductId,
                           Map<String, List<Integer>> tokenToRules,
                           List<Long> productIds,
                           List<Rule> rules
    ) {

        /* DB → 메모리 빌드 */
        static Snapshot build(List<Rule> rules) {

            Map<Integer, Long> indexToProductId = new HashMap<>();
            Map<String, List<Integer>> tokenToRules = new HashMap<>();
            List<Long> productIds = new ArrayList<>();
            Set<String> allTokens = new HashSet<>();

            for (int idx = 0; idx < rules.size(); idx++) {
                Rule r = rules.get(idx);

                indexToProductId.put(idx, r.productId());
                productIds.add(r.productId());

                for (String raw : r.tokens()) {
                    String tok = raw.toUpperCase();
                    allTokens.add(tok);
                    tokenToRules
                            .computeIfAbsent(tok, k -> new ArrayList<>())
                            .add(idx);
                }
            }

            Trie trie = AcBuilder.build(allTokens);

            // 불변 래퍼로 감싸 노출 이후 수정 불가
            return new Snapshot(
                    trie,
                    Collections.unmodifiableMap(indexToProductId),
                    Collections.unmodifiableMap(
                            tokenToRules.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> List.copyOf(e.getValue())))),
                    List.copyOf(productIds),
                    List.copyOf(rules));
        }
    }
}
