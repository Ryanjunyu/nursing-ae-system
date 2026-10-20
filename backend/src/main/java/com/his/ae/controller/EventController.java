package com.his.ae.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.his.ae.common.Result;
import com.his.ae.service.EventService;
import com.his.ae.vo.EventDetailVO;
import com.his.ae.vo.EventListVO;
import com.his.ae.vo.EventQueryVO;
import com.his.ae.vo.EventReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /** 填报不良事件 */
    @PostMapping
    public Result<Integer> report(@Valid @RequestBody EventReportVO vo) {
        return Result.ok(eventService.report(vo));
    }

    /** 检索列表（数据范围隔离 + 筛选 + 脱敏） */
    @GetMapping
    public Result<IPage<EventListVO>> list(EventQueryVO q) {
        return Result.ok(eventService.list(q));
    }

    /** 事件明细 */
    @GetMapping("/{id}")
    public Result<EventDetailVO> detail(@PathVariable Integer id) {
        return Result.ok(eventService.detail(id));
    }
}
