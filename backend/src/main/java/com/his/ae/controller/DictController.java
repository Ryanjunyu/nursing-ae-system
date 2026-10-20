package com.his.ae.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.his.ae.common.Result;
import com.his.ae.entity.DictEventType;
import com.his.ae.entity.DictInjuryLevel;
import com.his.ae.entity.SysDepartment;
import com.his.ae.mapper.DictEventTypeMapper;
import com.his.ae.mapper.DictInjuryLevelMapper;
import com.his.ae.mapper.SysDepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 字典查询（科室 / 事件类型 / 损伤分级），供前端表单下拉联动 */
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final SysDepartmentMapper deptMapper;
    private final DictEventTypeMapper typeMapper;
    private final DictInjuryLevelMapper levelMapper;

    @GetMapping("/departments")
    public Result<List<SysDepartment>> departments() {
        return Result.ok(deptMapper.selectList(new QueryWrapper<SysDepartment>().orderByAsc("dept_id")));
    }

    @GetMapping("/event-types")
    public Result<List<DictEventType>> eventTypes() {
        return Result.ok(typeMapper.selectList(new QueryWrapper<DictEventType>().orderByAsc("sort_no")));
    }

    @GetMapping("/injury-levels")
    public Result<List<DictInjuryLevel>> injuryLevels() {
        return Result.ok(levelMapper.selectList(new QueryWrapper<DictInjuryLevel>().orderByAsc("sort_no")));
    }
}
