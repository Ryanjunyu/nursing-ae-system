package com.his.ae.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FactorSuggester 纯逻辑单元测试（无 Spring 依赖） */
class FactorSuggesterTest {

    @Test
    void suggest_returns_five_dimensions() {
        Map<String, List<String>> m = FactorSuggester.suggest("FALL", "患者跌倒");
        assertEquals(5, m.size());
        assertTrue(m.containsKey(FactorSuggester.DIM_HUMAN));
        assertTrue(m.containsKey(FactorSuggester.DIM_MACHINE));
        assertTrue(m.containsKey(FactorSuggester.DIM_MATERIAL));
        assertTrue(m.containsKey(FactorSuggester.DIM_METHOD));
        assertTrue(m.containsKey(FactorSuggester.DIM_ENV));
    }

    @Test
    void suggest_includes_type_specific_factors_for_fall() {
        Map<String, List<String>> m = FactorSuggester.suggest("FALL", "");
        // 跌倒类型应在"机"维度补充床栏相关诱因
        assertTrue(m.get(FactorSuggester.DIM_MACHINE).stream()
                .anyMatch(s -> s.contains("床栏")), "应含床栏相关诱因");
    }

    @Test
    void suggest_keyword_scan_adds_dimension_factor() {
        Map<String, List<String>> m = FactorSuggester.suggest("OTHER", "患者夜间跌倒，地面湿滑");
        assertTrue(m.get(FactorSuggester.DIM_HUMAN).contains("患者活动未陪护"),
                "关键词'跌倒'应补充 人-患者活动未陪护");
        assertTrue(m.get(FactorSuggester.DIM_ENV).contains("地面湿滑或无防滑措施"),
                "关键词'跌倒'应补充 环-地面湿滑");
    }

    @Test
    void suggest_dedupes_and_has_no_blank() {
        Map<String, List<String>> m = FactorSuggester.suggest("FALL", "跌倒 跌倒");
        for (List<String> list : m.values()) {
            for (String s : list) {
                assertFalse(s.isBlank(), "诱因不应为空");
            }
            // 去重：无重复元素
            assertEquals(list.size(), list.stream().distinct().count());
        }
    }

    @Test
    void suggest_unknown_type_falls_back_to_base() {
        Map<String, List<String>> m = FactorSuggester.suggest("UNKNOWN_X", "随便描述");
        // 未知类型仍至少有基础通用提示
        assertTrue(m.get(FactorSuggester.DIM_HUMAN).size() >= 3);
    }
}
