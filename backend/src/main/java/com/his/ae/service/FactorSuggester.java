package com.his.ae.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RCA 五维诱因辅助建议生成器（纯逻辑，无 Spring 依赖，便于单元测试）。
 * 维度：人 / 机 / 料 / 法 / 环。
 * 策略：基础通用提示 + 按事件类型补充 + 按描述关键词扫描，三者合并去重。
 */
public final class FactorSuggester {

    public static final String DIM_HUMAN = "人";
    public static final String DIM_MACHINE = "机";
    public static final String DIM_MATERIAL = "料";
    public static final String DIM_METHOD = "法";
    public static final String DIM_ENV = "环";

    public static final List<String> DIMENSIONS = Arrays.asList(
            DIM_HUMAN, DIM_MACHINE, DIM_MATERIAL, DIM_METHOD, DIM_ENV);

    // 每个维度的基础通用提示
    private static final Map<String, List<String>> BASE = new LinkedHashMap<>();
    static {
        BASE.put(DIM_HUMAN, Arrays.asList(
                "评估/宣教不到位", "护士巡视或陪护不足", "人员配置或培训不足"));
        BASE.put(DIM_MACHINE, Arrays.asList(
                "设备设施老化或故障", "报警装置失灵", "辅助器具缺失"));
        BASE.put(DIM_MATERIAL, Arrays.asList(
                "耗材/药品准备不足", "标识不清或物料不合格", "物料供应不及时"));
        BASE.put(DIM_METHOD, Arrays.asList(
                "制度/流程未落实", "查对制度执行缺失", "操作规范未被遵守"));
        BASE.put(DIM_ENV, Arrays.asList(
                "环境布局不合理", "地面/照明/温湿度隐患", "嘈杂或干扰因素多"));
    }

    // 按事件类型补充的维度诱因
    private static final Map<String, Map<String, List<String>>> BY_TYPE = new LinkedHashMap<>();
    static {
        // 跌倒 / 坠床
        Map<String, List<String>> fall = dim(
                Arrays.asList("患者跌倒/平衡风险评估缺失", "服用易致跌倒药物未告知", "患者及家属防跌倒意识薄弱"),
                Arrays.asList("床栏损坏未及时维修", "呼叫器故障或不在手边", "移位/转运辅具不足"),
                Arrays.asList("防滑鞋/约束带配备不足", "警示标识缺失"),
                Arrays.asList("跌倒防范管理制度未落实", "高风险患者转运流程不规范"),
                Arrays.asList("地面湿滑无警示", "病区光线不足", "床单元间距过窄"));
        BY_TYPE.put("FALL", fall);
        BY_TYPE.put("FALL_BED", fall);

        // 压疮
        Map<String, List<String>> pressure = dim(
                Arrays.asList("翻身/变更体位间隔过长", "营养风险评估不足", "患者及家属皮肤护理宣教不足"),
                Arrays.asList("气垫床/减压设备不足", "监护设备长时间压迫"),
                Arrays.asList("减压敷料/保湿敷料短缺", "失禁护理用品不足"),
                Arrays.asList("Braden 评分未执行", "压疮预防流程执行差"),
                Arrays.asList("病房温度低致末梢循环差", "床单位潮湿不平整"));
        BY_TYPE.put("PRESSURE", pressure);

        // 输液外渗
        Map<String, List<String>> infil = dim(
                Arrays.asList("穿刺/固定技术不熟练", "巡视观察不到位", "患者自我观察宣教不足"),
                Arrays.asList("输液泵/注射泵参数异常", "穿刺器具质量缺陷"),
                Arrays.asList("刺激性药物溶媒选择不当", "敷贴/固定材料过敏"),
                Arrays.asList("外渗观察与处理流程缺失", "高危药物使用规范未落实"),
                Arrays.asList("光线不足致观察困难", "输液环境嘈杂易分心"));
        BY_TYPE.put("INFIL", infil);

        // 给药错误
        Map<String, List<String>> drug = dim(
                Arrays.asList("双人核对未执行", "药理知识/识别能力不足", "工作疲劳或注意力分散"),
                Arrays.asList("输液泵/给药装置故障", "药品包装/标签相似易混淆"),
                Arrays.asList("药品储存/摆放混乱", "高警示药品标识不清"),
                Arrays.asList("医嘱录入/转抄错误", "查对与发药流程缺陷"),
                Arrays.asList("夜间照明不足", "干扰多致沟通失误"));
        BY_TYPE.put("WRONG_DRUG", drug);

        // 管路滑脱
        Map<String, List<String>> tube = dim(
                Arrays.asList("置管/固定宣教不足", "患者躁动未有效约束", "翻身时牵拉疏忽"),
                Arrays.asList("固定装置松动/老化", "引流装置连接不牢"),
                Arrays.asList("合适规格导管/固定贴缺乏", "约束带不足"),
                Arrays.asList("管路滑脱应急预案未演练", "交接班评估缺失"),
                Arrays.asList("床旁管路摆放混乱", "夜间照明不足"));
        BY_TYPE.put("TUBE", tube);
    }

    // 关键词扫描：描述中出现关键词 -> 对应维度补充诱因
    private static final Map<String, String[]> KEYWORD_DIM = new LinkedHashMap<>();
    static {
        KEYWORD_DIM.put("跌倒|坠床|滑倒", new String[]{
                DIM_HUMAN, "患者活动未陪护", DIM_ENV, "地面湿滑或无防滑措施"});
        KEYWORD_DIM.put("压疮|褥疮|破溃", new String[]{
                DIM_HUMAN, "长期卧床翻身不到位", DIM_METHOD, "未按时进行皮肤评估"});
        KEYWORD_DIM.put("给药|用药|发药|输液|注射", new String[]{
                DIM_METHOD, "未执行双人核对", DIM_MACHINE, "给药设备未校准"});
        KEYWORD_DIM.put("管路|导管|引流", new String[]{
                DIM_HUMAN, "导管固定与宣教不足", DIM_MATERIAL, "固定材料不合适"});
        KEYWORD_DIM.put("夜间|值班|忙|交班", new String[]{
                DIM_METHOD, "高峰时段人力调配不足", DIM_ENV, "交接班信息遗漏"});
        KEYWORD_DIM.put("新护士|实习|培训|不熟", new String[]{
                DIM_HUMAN, "低年资人员培训不足", DIM_METHOD, "带教与查对不到位"});
    }

    private FactorSuggester() {}

    /**
     * 生成五维诱因建议。
     * @param eventType 事件类型代码（如 FALL）
     * @param eventDesc 事件描述
     * @return 维度 -> 建议诱因列表（已去重，保持顺序）
     */
    public static Map<String, List<String>> suggest(String eventType, String eventDesc) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String dim : DIMENSIONS) {
            List<String> items = new ArrayList<>(BASE.get(dim));
            Map<String, List<String>> typeMap = BY_TYPE.get(eventType);
            if (typeMap != null && typeMap.get(dim) != null) {
                items.addAll(typeMap.get(dim));
            }
            result.put(dim, items);
        }
        // 关键词扫描补充
        if (eventDesc != null && !eventDesc.isBlank()) {
            for (Map.Entry<String, String[]> entry : KEYWORD_DIM.entrySet()) {
                if (eventDesc.matches(".*(" + entry.getKey() + ").*")) {
                    String[] kv = entry.getValue();
                    for (int i = 0; i + 1 < kv.length; i += 2) {
                        result.get(kv[i]).add(kv[i + 1]);
                    }
                }
            }
        }
        // 去重
        for (String dim : DIMENSIONS) {
            result.put(dim, dedupe(result.get(dim)));
        }
        return result;
    }

    private static Map<String, List<String>> dim(List<String> human, List<String> machine,
                                                List<String> material, List<String> method, List<String> env) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(DIM_HUMAN, human);
        m.put(DIM_MACHINE, machine);
        m.put(DIM_MATERIAL, material);
        m.put(DIM_METHOD, method);
        m.put(DIM_ENV, env);
        return m;
    }

    private static List<String> dedupe(List<String> list) {
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }
}
