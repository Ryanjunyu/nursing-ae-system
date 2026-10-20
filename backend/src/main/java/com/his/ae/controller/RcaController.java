package com.his.ae.controller;

import com.his.ae.common.Result;
import com.his.ae.entity.EventFactor;
import com.his.ae.entity.RcaRecord;
import com.his.ae.service.RcaService;
import com.his.ae.vo.RcaAssistVO;
import com.his.ae.vo.RcaFactorBatchVO;
import com.his.ae.vo.RcaSaveVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rca")
@RequiredArgsConstructor
public class RcaController {

    private final RcaService rcaService;

    /** 保存五维诱因（先删后插，整体替换） */
    @PostMapping("/factors")
    public Result<Void> saveFactors(@Valid @RequestBody RcaFactorBatchVO vo) {
        rcaService.saveFactors(vo);
        return Result.ok();
    }

    /** 查询某事件五维诱因 */
    @GetMapping("/factors/{eventId}")
    public Result<List<EventFactor>> listFactors(@PathVariable Integer eventId) {
        return Result.ok(rcaService.listFactors(eventId));
    }

    /** 保存/更新根因分析记录 */
    @PostMapping
    public Result<Integer> saveRca(@Valid @RequestBody RcaSaveVO vo) {
        return Result.ok(rcaService.saveRca(vo));
    }

    /** 查询某事件根因分析记录 */
    @GetMapping("/{eventId}")
    public Result<RcaRecord> getRca(@PathVariable Integer eventId) {
        return Result.ok(rcaService.getRca(eventId));
    }

    /** 辅助分析：完成度 + 五维智能建议 */
    @GetMapping("/assist/{eventId}")
    public Result<RcaAssistVO> assist(@PathVariable Integer eventId) {
        return Result.ok(rcaService.assist(eventId));
    }
}
