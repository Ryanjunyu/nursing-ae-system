package com.his.ae.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 模拟患者信息表（脱敏存储，仅教学模拟） */
@Data
@TableName("patient")
public class Patient {
    @TableId(type = IdType.AUTO)
    private Integer patientId;
    private String maskedName;     // 脱敏姓名 如 张*
    private String gender;
    private Integer age;
    private String bedNo;
    private String admissionNo;    // 住院号（脱敏）
    private Integer deptId;
    private String diagnosis;
    private String idNoMasked;     // 身份证号脱敏
}
