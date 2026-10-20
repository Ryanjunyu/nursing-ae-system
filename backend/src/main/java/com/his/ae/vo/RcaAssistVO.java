package com.his.ae.vo;

import com.his.ae.entity.EventFactor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 辅助分析返回：已有诱因（按维度）+ 完成度 + 智能建议 */
@Data
public class RcaAssistVO {
    private Integer eventId;
    private String eventTypeName;
    private Map<String, List<String>> suggestions;                 // 五维建议诱因（可一键填入）
    private Map<String, Integer> factorCountByDimension;           // 各维度已填诱因数
    private Map<String, List<EventFactor>> existingByDimension;    // 各维度已有诱因
    private int completeness;                                      // 分析完成度 0~100（已填维度占比）
}
