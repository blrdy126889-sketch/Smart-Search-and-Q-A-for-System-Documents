package com.docqa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docqa.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    @Insert("INSERT INTO sys_operation_log(user_id, username, module, operation, request_method, request_path, " +
            "params, result_code, error_msg, ip, cost_ms) " +
            "VALUES(#{userId}, #{username}, #{module}, #{operation}, #{requestMethod}, #{requestPath}, " +
            "#{params}::jsonb, #{resultCode}, #{errorMsg}, #{ip}, #{costMs})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLog(SysOperationLog log);
}
