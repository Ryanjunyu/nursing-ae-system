package com.his.ae.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.his.ae.common.BizException;
import com.his.ae.entity.AdverseEvent;
import com.his.ae.entity.DictEventType;
import com.his.ae.entity.EventFactor;
import com.his.ae.entity.RcaRecord;
import com.his.ae.mapper.AdverseEventMapper;
import com.his.ae.mapper.DictEventTypeMapper;
import com.his.ae.mapper.EventFactorMapper;
import com.his.ae.mapper.RcaRecordMapper;
import com.his.ae.security.CurrentUserHolder;
import com.his.ae.security.LoginUser;
import com.his.ae.service.FactorSuggester;
import com.his.ae.service.RcaService;
import com.his.ae.vo.RcaAssistVO;
import com.his.ae.vo.RcaFactorBatchVO;
import com.his.ae.vo.RcaFactorVO;
import com.his.ae.vo.RcaSaveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RcaServiceImpl implements RcaService {

    private static final List<String> DIMENSIONS = FactorSuggester.DIMENSIONS;

    private final EventFactorMapper factorMapper;
    private final RcaRecordMapper rcaMapper;
    private final AdverseEventMapper eventMapper;
    private final DictEventTypeMapper typeMapper;

    @Override
    @com.his.ae.annotation.Audit(actionType = "ANALYZE", targetType = "诱因")
    public void saveFactors(RcaFactorBatchVO vo) {
        LoginUser lu = CurrentUserHolder.get();
        assertAnalyst(lu, vo.getEventId());

        // 维度合法性校验
        for (RcaFactorVO it : vo.getFactors()) {
            if (!DIMENSIONS.contains(it.getDimension())) {
                throw new BizException(400, "非法维度：" + it.getDimension() + "（应为 人/机/料/法/环）");
            }
        }
        // 先删后插，整体替换该事件的诱因
        factorMapper.delete(new QueryWrapper<EventFactor>().eq("event_id", vo.getEventId()));
        for (RcaFactorVO it : vo.getFactors()) {
            EventFactor f = new EventFactor();
            f.setEventId(vo.getEventId());
            f.setDimension(it.getDimension());
            f.setFactorType(it.getFactorType() != null ? it.getFactorType() : "PROXIMAL");
            f.setFactorContent(it.getFactorContent());
            f.setCreateTime(LocalDateTime.now());
            factorMapper.insert(f);
        }
    }

    @Override
    public List<EventFactor> listFactors(Integer eventId) {
        LoginUser lu = CurrentUserHolder.get();
        assertViewable(lu, eventId);
        return factorMapper.selectList(
                new QueryWrapper<EventFactor>().eq("event_id", eventId).orderByAsc("factor_id"));
    }

    @Override
    @com.his.ae.annotation.Audit(actionType = "ANALYZE", targetType = "根因")
    public Integer saveRca(RcaSaveVO vo) {
        LoginUser lu = CurrentUserHolder.get();
        AdverseEvent event = assertAnalyst(lu, vo.getEventId());

        RcaRecord exist = rcaMapper.selectOne(
                new QueryWrapper<RcaRecord>().eq("event_id", vo.getEventId()));
        RcaRecord r = exist != null ? exist : new RcaRecord();
        r.setEventId(vo.getEventId());
        r.setAnalystId(lu.getUserId());
        r.setTimeline(vo.getTimeline());
        r.setRootHuman(vo.getRootHuman());
        r.setRootMachine(vo.getRootMachine());
        r.setRootMaterial(vo.getRootMaterial());
        r.setRootMethod(vo.getRootMethod());
        r.setRootEnv(vo.getRootEnv());
        r.setImprovement(vo.getImprovement());
        r.setAnalysisStatus(vo.getAnalysisStatus() != null
                ? vo.getAnalysisStatus() : (exist != null ? "DONE" : "ANALYZING"));

        if (exist == null) {
            rcaMapper.insert(r);
        } else {
            rcaMapper.updateById(r);
        }
        return r.getRcaId();
    }

    @Override
    public RcaRecord getRca(Integer eventId) {
        LoginUser lu = CurrentUserHolder.get();
        assertViewable(lu, eventId);
        return rcaMapper.selectOne(new QueryWrapper<RcaRecord>().eq("event_id", eventId));
    }

    @Override
    public RcaAssistVO assist(Integer eventId) {
        LoginUser lu = CurrentUserHolder.get();
        AdverseEvent event = assertViewable(lu, eventId);

        List<EventFactor> factors = factorMapper.selectList(
                new QueryWrapper<EventFactor>().eq("event_id", eventId).orderByAsc("factor_id"));

        // 按维度分组已有诱因
        Map<String, List<EventFactor>> existingByDimension = new LinkedHashMap<>();
        DIMENSIONS.forEach(d -> existingByDimension.put(d, new ArrayList<>()));
        for (EventFactor f : factors) {
            existingByDimension.computeIfAbsent(f.getDimension(), k -> new ArrayList<>()).add(f);
        }

        // 各维度已填数量
        Map<String, Integer> countByDimension = existingByDimension.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));

        // 完成度：已填维度占比
        long filled = DIMENSIONS.stream().filter(d -> countByDimension.getOrDefault(d, 0) > 0).count();
        int completeness = (int) (filled * 100 / DIMENSIONS.size());

        // 智能建议
        String typeName = null;
        DictEventType t = event.getEventType() != null
                ? typeMapper.selectOne(new QueryWrapper<DictEventType>().eq("type_code", event.getEventType())) : null;
        if (t != null) typeName = t.getTypeName();
        Map<String, List<String>> suggestions = FactorSuggester.suggest(event.getEventType(), event.getEventDesc());

        RcaAssistVO vo = new RcaAssistVO();
        vo.setEventId(eventId);
        vo.setEventTypeName(typeName);
        vo.setSuggestions(suggestions);
        vo.setFactorCountByDimension(countByDimension);
        vo.setExistingByDimension(existingByDimension);
        vo.setCompleteness(completeness);
        return vo;
    }

    // ---------- 权限与校验 ----------

    /** 分析权限：仅护士长/管理员可分析，且受本科室数据范围限制 */
    private AdverseEvent assertAnalyst(LoginUser lu, Integer eventId) {
        if (lu == null) throw new BizException(401, "未登录");
        if (!"ADMIN".equals(lu.getRole()) && !"HEAD_NURSE".equals(lu.getRole())) {
            throw new BizException(403, "仅护士长/管理员可进行根因分析");
        }
        AdverseEvent e = eventMapper.selectById(eventId);
        if (e == null) throw new BizException(404, "事件不存在");
        if (!"ADMIN".equals(lu.getRole()) && !lu.getDeptId().equals(e.getDeptId())) {
            throw new BizException(403, "无权分析本科室以外的事件");
        }
        return e;
    }

    /** 查看权限：全员可看，但受本科室数据范围限制 */
    private AdverseEvent assertViewable(LoginUser lu, Integer eventId) {
        if (lu == null) throw new BizException(401, "未登录");
        AdverseEvent e = eventMapper.selectById(eventId);
        if (e == null) throw new BizException(404, "事件不存在");
        if (!"ADMIN".equals(lu.getRole()) && !lu.getDeptId().equals(e.getDeptId())) {
            throw new BizException(403, "无权查看本科室以外的事件");
        }
        return e;
    }
}
