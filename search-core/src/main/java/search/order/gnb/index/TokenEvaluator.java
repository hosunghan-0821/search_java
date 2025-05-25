package search.order.gnb.index;

import lombok.RequiredArgsConstructor;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TokenEvaluator {
    private final IndexHolder indexHolder;

    /* 문자열 1건을 평가해 ‘product Id를 ’ 반환 */
    public List<Long> evaluate(String s) {

        //무조건 대문자로 비교 // 토큰 입력도 대문자로 되어 있음.
        s = s.toUpperCase();

        Trie trie = indexHolder.getMain().get();
        List<Rule> ruleSets = indexHolder.getRules();
        Map<String, List<Integer>> tokenToRules = indexHolder.getTokenToRules();
        Map<Integer, Long> indexToProductId = indexHolder.getIndexToProductId();

        int ruleCount = ruleSets.size();
        int[] remain = new int[ruleCount];
        BitSet touched = new BitSet(ruleCount);

        for (int i = 0; i < ruleCount; i++) {
            remain[i] = ruleSets.get(i).tokens().size();
        }

        /* ① 문자열 한 번 스캔 */
        for (Emit e : trie.parseText(s)) {
            List<Integer> list = tokenToRules.get(e.getKeyword());
            if (list == null) {
                continue;
            }

            for (int rid : list) {
                if (remain[rid] > 0) {
                    remain[rid]--;
                    touched.set(rid);
                }
            }
        }

        /* ② 남은 토큰이 0이면 매칭 성공 */
        List<Integer> hit = new ArrayList<>();
        for (int rid = touched.nextSetBit(0);
             rid >= 0;
             rid = touched.nextSetBit(rid + 1)) {
            if (remain[rid] == 0) hit.add(rid);
        }

        return hit.stream().map(indexToProductId::get).collect(Collectors.toList());
    }
}
