package com.his.ae.service;

import com.his.ae.entity.EventFactor;
import com.his.ae.entity.RcaRecord;
import com.his.ae.vo.RcaAssistVO;
import com.his.ae.vo.RcaFactorBatchVO;
import com.his.ae.vo.RcaSaveVO;

import java.util.List;

public interface RcaService {

    /** 保存某事件的全部五维诱因（先删后插，保证整体替换） */
    void saveFactors(RcaFactorBatchVO vo);

    /** 查询某事件的五维诱因 */
    List<EventFactor> listFactors(Integer eventId);

    /** 保存/更新某事件的根因分析记录，返回 rcaId */
    Integer saveRca(RcaSaveVO vo);

    /** 查询某事件的根因分析记录（无则返回 null） */
    RcaRecord getRca(Integer eventId);

    /** 辅助分析：已有诱因 + 完成度 + 五维智能建议 */
    RcaAssistVO assist(Integer eventId);
}
