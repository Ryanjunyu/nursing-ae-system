package com.his.ae.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 单条五维诱因输入项 */
@Data
public class RcaFactorVO {
    @NotBlank(message = "维度不能为空")
    private String dimension;       // 人/机/料/法/环
    private String factorType = "PROXIMAL"; // PROXIMAL 近端 / ROOT 根本
    @NotBlank(message = "诱因内容不能为空")
    private String factorContent;
}
