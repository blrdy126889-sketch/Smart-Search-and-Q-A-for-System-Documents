package com.docqa.common.log;

import cn.dev33.satoken.stp.StpUtil;
import com.docqa.system.entity.SysOperationLog;
import com.docqa.system.mapper.SysOperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面：上传/审核/删除等关键操作自动落库 sys_operation_log
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        int resultCode = 0;
        String errorMsg = null;
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            resultCode = 1;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                saveLog(pjp, opLog, resultCode, errorMsg, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("操作日志保存失败: {}", ex.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, OpLog opLog, int resultCode, String errorMsg, long costMs) {
        SysOperationLog entity = new SysOperationLog();
        entity.setModule(opLog.module());
        entity.setOperation(opLog.operation());
        entity.setResultCode(resultCode);
        entity.setErrorMsg(errorMsg != null && errorMsg.length() > 900 ? errorMsg.substring(0, 900) : errorMsg);
        entity.setCostMs((int) costMs);

        try {
            long userId = StpUtil.getLoginIdAsLong();
            entity.setUserId(userId);
            Object username = StpUtil.getSession().get("username");
            entity.setUsername(username == null ? null : String.valueOf(username));
        } catch (Exception ignore) {
            // 未登录场景（如登录接口本身）
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            entity.setRequestMethod(req.getMethod());
            entity.setRequestPath(req.getRequestURI());
            entity.setIp(req.getRemoteAddr());
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < pjp.getArgs().length; i++) {
                Object arg = pjp.getArgs()[i];
                if (arg == null) continue;
                if (arg instanceof MultipartFile || arg instanceof HttpServletRequest || arg instanceof byte[]) {
                    params.put("arg" + i, arg.getClass().getSimpleName());
                } else {
                    try {
                        String s = objectMapper.writeValueAsString(arg);
                        params.put("arg" + i, s.length() > 480 ? s.substring(0, 480) : s);
                    } catch (Exception ignore) { }
                }
            }
            try {
                entity.setParams(objectMapper.writeValueAsString(params));
            } catch (Exception ignore) { }
        }
        operationLogMapper.insertLog(entity);
    }
}
