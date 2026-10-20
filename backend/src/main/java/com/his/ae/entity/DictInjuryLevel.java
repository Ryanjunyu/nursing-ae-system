package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 事件损伤分级字典表（中国医院协会四级） */
@Data
@TableName("dict_injury_level")
public class DictInjuryLevel {
    /** 分级代码 I/II/III/IV（字符主键，手动赋值） */
    @TableId(type = IdType.INPUT)
    private String levelCode;
    private String levelName;
    private String levelDesc;
    private Integer sortNo;
}
