package search.order.gnb.index;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class TokenEvaluatorTest {

    private final List<Rule> rules = new ArrayList<>();
    private TokenEvaluator tokenEvaluator;

    /** 모든 규칙 보관 */

    /* ─── 초기화 블록 ───────────────────────────── */ {
        rules.add(new Rule(
                1001L,
                Set.of("ABC", "DEF", "F0002")               // 예) 제품 1001번의 필수 토큰
        ));

        rules.add(new Rule(
                1002L,
                Set.of("ASD", "21", "F0001")                // 제품 1002
        ));

        rules.add(new Rule(
                1003L,
                Set.of("TOMATO", "FRUIT")
        ));

        rules.add(new Rule(
                1004L,
                Set.of("ATOM", "SCIENCE", "2025")
        ));

        rules.add(new Rule(
                1005L,
                Set.of("XYZ", "9999", "PINK")
        ));
    }

    @BeforeEach
    public void beforeAll() {
        IndexHolder indexHolder = new IndexHolder();
//        indexHolder.loadInitial(rules);

        this.tokenEvaluator = new TokenEvaluator(indexHolder);
    }

    @Test
    void evaluateTest() {
        assertEquals(tokenEvaluator.evaluate("XYZ").size(), 0);
        assertEquals(tokenEvaluator.evaluate("SCIENCE1231232025azxc123ATOM").get(0), 1004L);
        assertEquals(tokenEvaluator.evaluate("PINK9999XYZ").get(0), 1005L);
        assertEquals(tokenEvaluator.evaluate("TOMATO:1231azxcqweFRUIt").get(0), 1003L);
    }

}