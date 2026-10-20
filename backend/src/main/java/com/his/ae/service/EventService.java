package com.his.ae.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.his.ae.vo.EventDetailVO;
import com.his.ae.vo.EventQueryVO;
import com.his.ae.vo.EventReportVO;
import com.his.ae.vo.EventListVO;

public interface EventService {

    /** 填报不良事件（含匿名/脱敏/入库），返回事件ID */
    Integer report(EventReportVO vo);

    /** 检索列表（数据范围隔离 + 筛选 + 脱敏 + 分页） */
    IPage<EventListVO> list(EventQueryVO q);

    /** 事件明细（按权限 + 患者脱敏） */
    EventDetailVO detail(Integer id);
}
