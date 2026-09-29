package com.docqa.framework.tokenizer;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文分词器：HanLP 应用层分词，空格拼接写入 tsvector（simple 配置），免 DB zhparser 依赖
 */
public class Tokenizer {

    public static String tokenize(String text) {
        if (text == null || text.isBlank()) return "";
        List<Term> terms = HanLP.segment(text);
        return terms.stream()
                .map(t -> t.word.trim())
                .filter(w -> w.length() > 1 || isCjkPunctFree(w))
                .filter(w -> !STOP_WORDS.contains(w))
                .distinct()
                .collect(Collectors.joining(" "));
    }


    private static boolean isCjkPunctFree(String w) {
        return w.matches("[\\u4e00-\\u9fa5a-zA-Z0-9]+");
    }

    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "那", "他", "她", "它", "们", "什么", "怎么", "如何", "请", "吗",
            "呢", "吧", "啊", "与", "及", "或", "等", "为", "对", "被", "把", "让", "向",
            "从", "以", "其", "并", "可能", "可以", "应该", "需要", "the", "a", "an", "of", "and", "to", "is"
    );
}
