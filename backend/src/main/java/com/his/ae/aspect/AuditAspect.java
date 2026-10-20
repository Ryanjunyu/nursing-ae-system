package com.his.ae.aspect;

import com.his.ae.annotation.Audit;
import com.his.ae.entity.SysAuditLog;
import com.his.ae.mapper.SysAuditLogMapper;
import com.his.ae.security.CurrentUserHolder;
import com.his.ae.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** 审计切面：方法成功返回后写入 sys_audit_log（全操作留痕） */
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
        LoginUser u = CurrentUserHolder.get();

        // 登录接口由拦截器放行，上下文为空；从入参取账号记录真实登录人
        if (u == null && "LOGIN".equals(audit.actionType())) {
            Object[] args = jp.getArgs();
            if (args.length > 0 && args[0] instanceof com.his.ae.vo.LoginVO lv) {
                log.setUsername(lv.getUsername());
                log.setRole("LOGIN");
                log.setUserId(0);
            }
        }

        if (u != null) {
            log.setUserId(u.getUserId());
            log.setUsername(u.getUsername());
            log.setRole(u.getRole());
            log.setDeptId(u.getDeptId());
        }

        // 匿名上报：审计记 ANONYMOUS，无法回溯到具体上报人（符合隐私要求）
        if (u != null && "REPORT".equals(audit.actionType())) {
            Object[] args = jp.getArgs();
            if (args.length > 0 && args[0] instanceof com.his.ae.vo.EventReportVO ev
                    && ev.getIsAnonymous() != null && ev.getIsAnonymous() == 1) {
                log.setUserId(0);
                log.setUsername("ANONYMOUS");
                log.setRole("ANONYMOUS");
                log.setDeptId(null);
            }
        } else {
            // 匿名操作（如匿名上报）记 ANONYMOUS，无法回溯到具体人
            log.setUserId(0);
            log.setUsername("ANONYMOUS");
        }

        log.setActionType(audit.actionType());
        log.setTargetType(audit.targetType());

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
