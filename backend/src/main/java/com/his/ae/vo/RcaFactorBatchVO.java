package com.his.ae.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 五维诱因批量保存入参 */
@Data
public class RcaFactorBatchVO {
    @NotNull(message = "事件ID不能为空")
    private Integer eventId;
    @Valid
    private List<RcaFactorVO> factors = List.of();
}
