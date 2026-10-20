package com.his.ae.service.impl;

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
import com.his.ae.vo.RcaAssistVO;
import com.his.ae.vo.RcaFactorBatchVO;
import com.his.ae.vo.RcaFactorVO;
import com.his.ae.vo.RcaSaveVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RcaServiceImpl 单元测试（Mockito，不依赖数据库/Spring） */
@ExtendWith(MockitoExtension.class)
class RcaServiceImplTest {

    @Mock private EventFactorMapper factorMapper;
    @Mock private RcaRecordMapper rcaMapper;
    @Mock private AdverseEventMapper eventMapper;
    @Mock private DictEventTypeMapper typeMapper;

    @InjectMocks private RcaServiceImpl rcaService;

    private LoginUser nurse;
    private LoginUser head1;   // 内科一病区
    private LoginUser admin;

    @BeforeEach
    void setUp() {
        nurse = lu(10, "NURSE", 1);
        head1 = lu(20, "HEAD_NURSE", 1);
        admin = lu(1, "ADMIN", 5);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    private LoginUser lu(Integer userId, String role, Integer deptId) {
        LoginUser u = new LoginUser();
        u.setUserId(userId);
        u.setUsername("u" + userId);
        u.setRole(role);
        u.setDeptId(deptId);
        return u;
    }

    private AdverseEvent eventInDept(int deptId) {
        AdverseEvent e = new AdverseEvent();
        e.setEventId(100);
        e.setDeptId(deptId);
        e.setEventType("FALL");
        e.setEventDesc("患者跌倒");
        return e;
    }

    // ---------------- 权限 ----------------

    @Test
    void saveFactors_denied_for_nurse() {
        CurrentUserHolder.set(nurse); // 角色校验先于查询，护士直接被拒
        RcaFactorBatchVO vo = new RcaFactorBatchVO();
        vo.setEventId(100);
        vo.setFactors(List.of(factor("人", "评估不足")));
        BizException ex = assertThrows(BizException.class, () -> rcaService.saveFactors(vo));
        assertEquals(403, ex.getCode());
        verify(factorMapper, never()).delete(any());
        verify(factorMapper, never()).insert(any(EventFactor.class));
    }

    @Test
    void saveFactors_denied_cross_dept_head_nurse() {
        CurrentUserHolder.set(head1); // dept 1
        when(eventMapper.selectById(100)).thenReturn(eventInDept(2)); // 事件在 dept 2
        RcaFactorBatchVO vo = new RcaFactorBatchVO();
        vo.setEventId(100);
        vo.setFactors(List.of(factor("人", "x")));
        BizException ex = assertThrows(BizException.class, () -> rcaService.saveFactors(vo));
        assertEquals(403, ex.getCode());
    }

    @Test
    void saveFactors_invalid_dimension() {
        CurrentUserHolder.set(head1);
        when(eventMapper.selectById(100)).thenReturn(eventInDept(1));
        RcaFactorBatchVO vo = new RcaFactorBatchVO();
        vo.setEventId(100);
        vo.setFactors(List.of(factor("火星", "x")));
        BizException ex = assertThrows(BizException.class, () -> rcaService.saveFactors(vo));
        assertEquals(400, ex.getCode());
    }

    // ---------------- 诱因保存（先删后插） ----------------

    @Test
    void saveFactors_delete_then_insert() {
        CurrentUserHolder.set(head1);
        when(eventMapper.selectById(100)).thenReturn(eventInDept(1));
        when(factorMapper.delete(any())).thenReturn(2);
        when(factorMapper.insert(any(EventFactor.class))).thenReturn(1);

        RcaFactorBatchVO vo = new RcaFactorBatchVO();
        vo.setEventId(100);
        vo.setFactors(List.of(factor("人", "评估不足"), factor("机", "床栏损坏")));
        rcaService.saveFactors(vo);

        verify(factorMapper, times(1)).delete(any());
        verify(factorMapper, times(2)).insert(any(EventFactor.class));
    }

    @Test
    void saveFactors_empty_clears_all() {
        CurrentUserHolder.set(head1);
        when(eventMapper.selectById(100)).thenReturn(eventInDept(1));
        when(factorMapper.delete(any())).thenReturn(3);

        RcaFactorBatchVO vo = new RcaFactorBatchVO();
        vo.setEventId(100);
        vo.setFactors(List.of());
        rcaService.saveFactors(vo);

        verify(factorMapper, times(1)).delete(any());
        verify(factorMapper, never()).insert(any(EventFactor.class));
    }

    // ---------------- 根因 upsert ----------------

    @Test
    void saveRca_insert_when_no_existing() {
        CurrentUserHolder.set(head1);
        when(eventMapper.selectById(100)).thenReturn(eventInDept(1));
        when(rcaMapper.selectOne(any())).thenReturn(null);
        when(rcaMapper.insert(any(RcaRecord.class))).thenReturn(1);

        RcaSaveVO vo = new RcaSaveVO();
        vo.setEventId(100);
        vo.setRootHuman("培训不足");
        vo.setImprovement("加强培训");
        rcaService.saveRca(vo);

        verify(rcaMapper, times(1)).insert(any(RcaRecord.class));
        verify(rcaMapper, never()).updateById(any(RcaRecord.class));
        assertEquals("ANALYZING", capturedStatus());
    }

    @Test
    void saveRca_update_when_existing() {
        CurrentUserHolder.set(head1);
        when(eventMapper.selectById(100)).thenReturn(eventInDept(1));
        RcaRecord exist = new RcaRecord();
        exist.setRcaId(7);
        when(rcaMapper.selectOne(any())).thenReturn(exist);
        when(rcaMapper.updateById(any(RcaRecord.class))).thenReturn(1);

        RcaSaveVO vo = new RcaSaveVO();
        vo.setEventId(100);
        vo.setRootHuman("根本人因");
        vo.setAnalysisStatus("DONE");
        Integer id = rcaService.saveRca(vo);

        verify(rcaMapper, never()).insert(any(RcaRecord.class));
        verify(rcaMapper, times(1)).updateById(any(RcaRecord.class));
        assertEquals(7, id);
    }

    // ---------------- 辅助分析 ----------------

    @Test
    void assist_computes_completeness_and_suggestions() {
        CurrentUserHolder.set(head1);
        AdverseEvent e = eventInDept(1);
        when(eventMapper.selectById(100)).thenReturn(e);
        DictEventType t = new DictEventType();
        t.setTypeName("跌倒");
        when(typeMapper.selectOne(any())).thenReturn(t);

        EventFactor f = new EventFactor();
        f.setDimension("人");
        f.setFactorContent("评估不足");
        when(factorMapper.selectList(any())).thenReturn(List.of(f));

        RcaAssistVO vo = rcaService.assist(100);

        assertEquals("跌倒", vo.getEventTypeName());
        assertEquals(20, vo.getCompleteness()); // 仅"人"维度有数据 -> 1/5
        assertEquals(Integer.valueOf(1), vo.getFactorCountByDimension().get("人"));
        // 五维均应有建议
        assertEquals(5, vo.getSuggestions().size());
        vo.getSuggestions().forEach((k, v) -> assertTrue(!v.isEmpty(), "维度 " + k + " 应给出建议"));
    }

    @Test
    void getRca_view_denied_cross_dept_nurse() {
        CurrentUserHolder.set(nurse); // dept 1
        when(eventMapper.selectById(100)).thenReturn(eventInDept(2)); // dept 2
        BizException ex = assertThrows(BizException.class, () -> rcaService.getRca(100));
        assertEquals(403, ex.getCode());
    }

    // ---------------- helpers ----------------

    private RcaFactorVO factor(String dim, String content) {
        RcaFactorVO f = new RcaFactorVO();
        f.setDimension(dim);
        f.setFactorContent(content);
        return f;
    }

    private String capturedStatus() {
        ArgumentCaptor<RcaRecord> cap = ArgumentCaptor.forClass(RcaRecord.class);
        verify(rcaMapper).insert(cap.capture());
        return cap.getValue().getAnalysisStatus();
    }
}
