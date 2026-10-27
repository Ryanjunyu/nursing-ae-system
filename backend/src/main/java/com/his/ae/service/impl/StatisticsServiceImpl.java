package com.his.ae.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.his.ae.common.BizException;
import com.his.ae.entity.AdverseEvent;
import com.his.ae.entity.DictEventType;
import com.his.ae.entity.DictInjuryLevel;
import com.his.ae.entity.SysDepartment;
import com.his.ae.mapper.AdverseEventMapper;
import com.his.ae.mapper.DictEventTypeMapper;
import com.his.ae.mapper.DictInjuryLevelMapper;
import com.his.ae.mapper.SysDepartmentMapper;
import com.his.ae.security.CurrentUserHolder;
import com.his.ae.security.LoginUser;
import com.his.ae.service.StatisticsService;
import com.his.ae.vo.StatItemVO;
import com.his.ae.vo.StatisticsSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final AdverseEventMapper eventMapper;
    private final SysDepartmentMapper deptMapper;
    private final DictEventTypeMapper typeMapper;
    private final DictInjuryLevelMapper levelMapper;

    /** 数据范围：管理员全部；护士长/护士仅本科室 */
    private List<AdverseEvent> scopedEvents() {
        LoginUser lu = CurrentUserHolder.get();
        if (lu == null) {
            throw new BizException(401, "未登录");
        }
        QueryWrapper<AdverseEvent> w = new QueryWrapper<>();
        if (!"ADMIN".equals(lu.getRole()) && lu.getDeptId() != null) {
            w.eq("dept_id", lu.getDeptId());
        }
        return eventMapper.selectList(w);
    }

    private String typeName(String code) {
        if (code == null) return "未知";
        DictEventType t = typeMapper.selectOne(new QueryWrapper<DictEventType>().eq("type_code", code));
        return t != null ? t.getTypeName() : code;
    }

    private String levelName(String code) {
        if (code == null) return "未知";
        DictInjuryLevel l = levelMapper.selectById(code);
        return l != null ? l.getLevelName() : code;
    }

    private String deptName(Integer deptId) {
        if (deptId == null) return "未分配";
        SysDepartment d = deptMapper.selectById(deptId);
        return d != null ? d.getDeptName() : ("科室" + deptId);
    }

    private List<StatItemVO> sortedDesc(Map<String, Long> m) {
        return m.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> new StatItemVO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public StatisticsSummaryVO summary() {
        List<AdverseEvent> list = scopedEvents();
        StatisticsSummaryVO s = new StatisticsSummaryVO();
        s.setTotal(list.size());
        s.setReported(list.stream().filter(e -> "REPORTED".equals(e.getStatus())).count());
        s.setAnalyzed(list.stream().filter(e -> "ANALYZED".equals(e.getStatus())).count());
        s.setAnonymous(list.stream().filter(e -> e.getIsAnonymous() != null && e.getIsAnonymous() == 1).count());
        List<StatItemVO> types = byType();
        List<StatItemVO> depts = byDept();
        if (!types.isEmpty()) {
            s.setTopType(types.get(0).getName());
            s.setTopTypeCount(types.get(0).getValue());
        }
        if (!depts.isEmpty()) {
            s.setTopDept(depts.get(0).getName());
            s.setTopDeptCount(depts.get(0).getValue());
        }
        return s;
    }

    @Override
    public List<StatItemVO> byDept() {
        Map<String, Long> m = new HashMap<>();
        for (AdverseEvent e : scopedEvents()) {
            m.merge(deptName(e.getDeptId()), 1L, Long::sum);
        }
        return sortedDesc(m);
    }

    @Override
    public List<StatItemVO> byType() {
        Map<String, Long> m = new HashMap<>();
        for (AdverseEvent e : scopedEvents()) {
            m.merge(typeName(e.getEventType()), 1L, Long::sum);
        }
        return sortedDesc(m);
    }

    @Override
    public List<StatItemVO> byLevel() {
        Map<String, Long> m = new HashMap<>();
        for (AdverseEvent e : scopedEvents()) {
            m.merge(levelName(e.getInjuryLevel()), 1L, Long::sum);
        }
        return sortedDesc(m);
    }

    @Override
    public List<StatItemVO> trend(int months) {
        int n = months <= 0 ? 6 : Math.min(months, 24);
        Map<String, Long> counter = new HashMap<>();
        for (AdverseEvent e : scopedEvents()) {
            if (e.getOccurTime() == null) continue;
            counter.merge(YearMonth.from(e.getOccurTime()).toString(), 1L, Long::sum);
        }
        List<StatItemVO> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = n - 1; i >= 0; i--) {
            String key = now.minusMonths(i).toString();
            out.add(new StatItemVO(key, counter.getOrDefault(key, 0L)));
        }
        return out;
    }
}
