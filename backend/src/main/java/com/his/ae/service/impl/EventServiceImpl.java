package com.his.ae.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.his.ae.common.BizException;
import com.his.ae.entity.AdverseEvent;
import com.his.ae.entity.DictEventType;
import com.his.ae.entity.DictInjuryLevel;
import com.his.ae.entity.Patient;
import com.his.ae.entity.SysDepartment;
import com.his.ae.entity.SysUser;
import com.his.ae.mapper.AdverseEventMapper;
import com.his.ae.mapper.DictEventTypeMapper;
import com.his.ae.mapper.DictInjuryLevelMapper;
import com.his.ae.mapper.PatientMapper;
import com.his.ae.mapper.SysDepartmentMapper;
import com.his.ae.mapper.SysUserMapper;
import com.his.ae.security.CurrentUserHolder;
import com.his.ae.security.LoginUser;
import com.his.ae.service.EventService;
import com.his.ae.vo.EventDetailVO;
import com.his.ae.vo.EventListVO;
import com.his.ae.vo.EventQueryVO;
import com.his.ae.vo.EventReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final AdverseEventMapper eventMapper;
    private final PatientMapper patientMapper;
    private final SysDepartmentMapper deptMapper;
    private final DictEventTypeMapper typeMapper;
    private final DictInjuryLevelMapper levelMapper;
    private final SysUserMapper userMapper;

    @Override
    @com.his.ae.annotation.Audit(actionType = "REPORT", targetType = "事件")
    public Integer report(EventReportVO vo) {
        LoginUser lu = CurrentUserHolder.get();

        // 患者信息脱敏入库（仅教学模拟，不存明文）
        Integer patientId = null;
        if (vo.getPatientMaskedName() != null && !vo.getPatientMaskedName().isBlank()) {
            Patient p = new Patient();
            p.setMaskedName(vo.getPatientMaskedName());
            p.setGender(vo.getPatientGender());
            p.setAge(vo.getPatientAge());
            p.setBedNo(vo.getPatientBedNo());
            p.setAdmissionNo(vo.getPatientAdmissionNo());
            p.setIdNoMasked(vo.getPatientIdNoMasked());
            p.setDiagnosis(vo.getPatientDiagnosis());
            p.setDeptId(vo.getDeptId());
            patientMapper.insert(p);
            patientId = p.getPatientId();
        }

        AdverseEvent e = new AdverseEvent();
        e.setEventNo("AE" + System.currentTimeMillis());
        e.setDeptId(vo.getDeptId());
        e.setEventType(vo.getEventType());
        e.setInjuryLevel(vo.getInjuryLevel());
        e.setOccurTime(vo.getOccurTime());
        e.setEventDesc(vo.getEventDesc());
        e.setSceneDesc(vo.getSceneDesc());
        e.setStatus("REPORTED");

        // 匿名：reporter_id 置空，无身份关联
        if (vo.getIsAnonymous() != null && vo.getIsAnonymous() == 1) {
            e.setIsAnonymous(1);
            e.setReporterId(null);
            e.setReporterRole("ANONYMOUS");
        } else {
            e.setIsAnonymous(0);
            e.setReporterId(lu.getUserId());
            e.setReporterRole(lu.getRole());
        }
        eventMapper.insert(e);
        return e.getEventId();
    }

    @Override
    public IPage<EventListVO> list(EventQueryVO q) {
        LoginUser lu = CurrentUserHolder.get();
        QueryWrapper<AdverseEvent> w = new QueryWrapper<>();
        // 数据范围隔离：非管理员仅本科室
        if (!"ADMIN".equals(lu.getRole()) && lu.getDeptId() != null) {
            w.eq("dept_id", lu.getDeptId());
        } else if (!"ADMIN".equals(lu.getRole())) {
            w.eq("1", "0");
        }
        if (q.getDeptId() != null) {
            w.eq("dept_id", q.getDeptId());
        }
        if (q.getEventType() != null && !q.getEventType().isBlank()) {
            w.eq("event_type", q.getEventType());
        }
        if (q.getInjuryLevel() != null && !q.getInjuryLevel().isBlank()) {
            w.eq("injury_level", q.getInjuryLevel());
        }
        if (q.getStartTime() != null && !q.getStartTime().isBlank()) {
            w.ge("occur_time", q.getStartTime());
        }
        if (q.getEndTime() != null && !q.getEndTime().isBlank()) {
            w.le("occur_time", q.getEndTime());
        }
        w.orderByDesc("occur_time");

        Page<AdverseEvent> page = new Page<>(q.getPage(), q.getSize());
        IPage<AdverseEvent> res = eventMapper.selectPage(page, w);
        return res.convert(this::toListVO);
    }

    private EventListVO toListVO(AdverseEvent e) {
        EventListVO v = new EventListVO();
        v.setEventId(e.getEventId());
        v.setEventNo(e.getEventNo());
        SysDepartment d = e.getDeptId() != null ? deptMapper.selectById(e.getDeptId()) : null;
        if (d != null) {
            v.setDeptName(d.getDeptName());
        }
        DictEventType t = e.getEventType() != null
                ? typeMapper.selectOne(new QueryWrapper<DictEventType>().eq("type_code", e.getEventType())) : null;
        if (t != null) {
            v.setEventTypeName(t.getTypeName());
        }
        DictInjuryLevel l = e.getInjuryLevel() != null ? levelMapper.selectById(e.getInjuryLevel()) : null;
        if (l != null) {
            v.setInjuryLevelName(l.getLevelName());
        }
        v.setOccurTime(e.getOccurTime());
        v.setEventDesc(e.getEventDesc());
        v.setIsAnonymous(e.getIsAnonymous());
        v.setStatus(e.getStatus());
        v.setReporterDisplay(reporterDisplay(e, CurrentUserHolder.get()));
        if (e.getPatientId() != null) {
            Patient p = patientMapper.selectById(e.getPatientId());
            if (p != null) {
                v.setPatientMaskedName(p.getMaskedName());
            }
        }
        return v;
    }

    private String reporterDisplay(AdverseEvent e, LoginUser lu) {
        if (e.getIsAnonymous() != null && e.getIsAnonymous() == 1) {
            return "匿名";
        }
        if (lu != null && ("ADMIN".equals(lu.getRole())
                || (e.getReporterId() != null && lu.getUserId().equals(e.getReporterId())))) {
            SysUser u = userMapper.selectById(e.getReporterId());
            return u != null ? u.getRealName() : "—";
        }
        return "（保密）";
    }

    @Override
    @com.his.ae.annotation.Audit(actionType = "VIEW", targetType = "事件")
    public EventDetailVO detail(Integer id) {
        LoginUser lu = CurrentUserHolder.get();
        AdverseEvent e = eventMapper.selectById(id);
        if (e == null) {
            throw new BizException(404, "事件不存在");
        }
        // 权限：非管理员仅可看本科室事件
        if (!"ADMIN".equals(lu.getRole()) && !lu.getDeptId().equals(e.getDeptId())) {
            throw new BizException(403, "无权查看本科室以外的事件");
        }
        EventDetailVO d = new EventDetailVO();
        d.setEventId(e.getEventId());
        d.setEventNo(e.getEventNo());
        SysDepartment dep = e.getDeptId() != null ? deptMapper.selectById(e.getDeptId()) : null;
        if (dep != null) {
            d.setDeptName(dep.getDeptName());
        }
        DictEventType t = e.getEventType() != null
                ? typeMapper.selectOne(new QueryWrapper<DictEventType>().eq("type_code", e.getEventType())) : null;
        if (t != null) {
            d.setEventTypeName(t.getTypeName());
        }
        DictInjuryLevel l = e.getInjuryLevel() != null ? levelMapper.selectById(e.getInjuryLevel()) : null;
        if (l != null) {
            d.setInjuryLevelName(l.getLevelName());
        }
        d.setOccurTime(e.getOccurTime());
        d.setEventDesc(e.getEventDesc());
        d.setSceneDesc(e.getSceneDesc());
        d.setIsAnonymous(e.getIsAnonymous());
        d.setReporterDisplay(reporterDisplay(e, lu));
        d.setStatus(e.getStatus());
        if (e.getPatientId() != null) {
            Patient p = patientMapper.selectById(e.getPatientId());
            if (p != null) {
                d.setPatientMaskedName(p.getMaskedName());
                d.setPatientGender(p.getGender());
                d.setPatientAge(p.getAge());
                d.setPatientBedNo(p.getBedNo());
                d.setPatientAdmissionNo(p.getAdmissionNo());
                d.setPatientDiagnosis(p.getDiagnosis());
            }
        }
        return d;
    }
}
