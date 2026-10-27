package com.his.ae.aspect;

import com.his.ae.annotation.Audit;
import com.his.ae.entity.SysAuditLog;
import com.his.ae.mapper.SysAuditLogMapper;
import com.his.ae.security.CurrentUserHolder;
import com.his.ae.security.LoginUser;
import com.his.ae.vo.EventReportVO;
import com.his.ae.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计切面：方法成功返回后写入 sys_audit_log（全操作留痕）。
 * 规则：
 *  - 正常操作记录真实操作人（userId/username/role/deptId）；
 *  - 匿名上报（isAnonymous=1）记 ANONYMOUS，不关联身份；
 *  - 登录接口记录登录账号。
 */
@Aspect
@Component
public class AuditAspect {

    private final SysAuditLogMapper auditLogMapper;

    public AuditAspect(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @AfterReturning(pointcut = "@annotation(audit)")
    public void record(JoinPoint jp, Audit audit) {
        SysAuditLog log = new SysAuditLog();
        log.setActionType(audit.actionType());
        log.setTargetType(audit.targetType());

        Object[] args = jp.getArgs();
        LoginUser u = CurrentUserHolder.get();

        // 是否匿名上报
        boolean anonymous = false;
        if ("REPORT".equals(audit.actionType()) && args.length > 0
                && args[0] instanceof EventReportVO ev
                && ev.getIsAnonymous() != null && ev.getIsAnonymous() == 1) {
            anonymous = true;
        }

        if (anonymous) {
            // 匿名上报：无法回溯到具体上报人
            log.setUserId(0);
            log.setUsername("ANONYMOUS");
            log.setRole("ANONYMOUS");
            log.setDeptId(null);
        } else if (u != null) {
            // 正常操作：记录真实操作人（含 VIEW/ANALYZE/UPDATE/CREATE 等）
            log.setUserId(u.getUserId());
            log.setUsername(u.getUsername());
            log.setRole(u.getRole());
            log.setDeptId(u.getDeptId());
        } else if ("LOGIN".equals(audit.actionType())) {
            // 登录：从入参取账号
            if (args.length > 0 && args[0] instanceof LoginVO lv) {
                log.setUserId(0);
                log.setUsername(lv.getUsername());
                log.setRole("LOGIN");
            }
        }

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                log.setIpAddr(req.getRemoteAddr());
                log.setReqParam(req.getRequestURI());
            }
        } catch (Exception ignored) {
            // 非 Web 上下文忽略
        }
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }
}
