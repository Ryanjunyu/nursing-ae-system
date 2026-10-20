package com.his.ae.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** RCA 根因分析记录保存入参 */
@Data
public class RcaSaveVO {
    @NotNull(message = "事件ID不能为空")
    private Integer eventId;
    private String timeline;        // 时间序列表（还原经过）
    private String rootHuman;
    private String rootMachine;
    private String rootMaterial;
    private String rootMethod;
    private String rootEnv;
    private String improvement;     // 改进措施
    private String analysisStatus;  // NOT_STARTED / ANALYZING / DONE
}
