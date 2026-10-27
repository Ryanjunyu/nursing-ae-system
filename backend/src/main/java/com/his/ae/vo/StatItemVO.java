package com.his.ae.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 通用统计项：名称 + 数量 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatItemVO {
    private String name;
    private long value;
}
