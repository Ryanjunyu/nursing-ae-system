package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 不良事件类型字典表 */
@Data
@TableName("dict_event_type")
public class DictEventType {
    @TableId(type = IdType.AUTO)
    private Integer typeId;
    private String typeCode;
    private String typeName;
    private String category;
    private Integer sortNo;
}
