package search.order.common.index;


import org.ahocorasick.trie.Trie;

import java.util.Collection;

public final class AcBuilder {
    private AcBuilder() {
    }

    public static org.ahocorasick.trie.Trie build(Collection<String> tokens) {
        return Trie.builder()
                .addKeywords(tokens)
                .build();         // 대소문자 이미 통일됐다고 가정
    }
}
