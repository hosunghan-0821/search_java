package search.order.gnb.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenEvaluator {
    private final IndexHolder indexHolder;

    /* 문자열 1건을 평가해 ‘product Id를 ’ 반환 */
    public List<Long> evaluate(String s) {

        //무조건 대문자로 비교 // 토큰 입력도 대문자로 되어 있음.
        s = s.toUpperCase();

        Trie trie = indexHolder.getMain().get();
        if (trie == null) {
            log.error("Trie not started");
            return List.of();
        }
        List<Rule> ruleSets = indexHolder.getRules();
        Map<String, List<Integer>> tokenToRules = indexHolder.getTokenToRules();
        Map<Integer, Long> indexToProductId = indexHolder.getIndexToProductId();

        int ruleCount = ruleSets.size();
        int[] remain = new int[ruleCount];
        BitSet touched = new BitSet(ruleCount);

        for (int i = 0; i < ruleCount; i++) {
            remain[i] = ruleSets.get(i).tokens().size();
        }

        Set<String> processedKeywords = new HashSet<>();

        Collection<Emit> emits = trie.parseText(s);  // 문자열 한 번 스캔
        for (Emit e : emits) {
            String keyword = e.getKeyword();

            // 이미 처리한 키워드라면 건너뛴다
            if (processedKeywords.contains(keyword)) {
                continue;
            }
            // 아니라면 이번에 처음 보는 키워드이므로 Set에 추가
            processedKeywords.add(keyword);

            // 기존 로직: 해당 키워드가 속한 룰 리스트 꺼내서 remain, touched 처리
            List<Integer> rulesForThisToken = tokenToRules.get(keyword);
            if (rulesForThisToken == null) {
                continue;
            }

            for (int rid : rulesForThisToken) {
                if (remain[rid] > 0) {
                    remain[rid]--;
                    touched.set(rid);
                }
            }
        }

        /* ② 남은 토큰이 0이면 매칭 성공 */
        List<Integer> hit = new ArrayList<>();
        for (int rid = touched.nextSetBit(0); rid >= 0; rid = touched.nextSetBit(rid + 1)) {
            if (remain[rid] == 0) hit.add(rid);
        }


        return hit.stream().map(indexToProductId::get).collect(Collectors.toList());
    }
}
