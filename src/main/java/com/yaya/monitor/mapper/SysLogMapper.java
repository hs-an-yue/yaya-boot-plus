package com.yaya.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.monitor.domain.entity.SysLog;
import com.yaya.monitor.domain.vo.SysLogVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 日志记录表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/monitor/SysLogMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysLogMapper extends BaseMapper<SysLog> {

    /**
     * 日志分页
     * @param page 分页信息
     * @param logType 日志类型(1-登陆日志 2-其它操作日志)
     * @param operName 操作人
     * @param username 账号
     * @param requestUrl 请求地址
     * @param status 请求状态(1-成功 0-失败)
     * @param ip 客户端IP
     * @param trackId  日志链路ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    @DataScope(userAlias = "l",userIdColumnName = "oper_id",deptAlias = "l",deptIdColumnName = "dept_id")
    Page<SysLogVo> getSysLogPage(Page<SysLogVo> page,
                                 @Param("logType") Integer logType,
                                 @Param("operName") String operName,
                                 @Param("username") String username,
                                 @Param("requestUrl") String requestUrl,
                                 @Param("status") Integer status,
                                 @Param("ip") String ip,
                                 @Param("trackId") String trackId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 日志详情
     *
     * @param logId 日志ID
     * @return 日志详情, 不存在返回 null
     */
    SysLogVo getSysLogDetail(@Param("logId") Long logId);

    /**
     * 清空日志表(TRUNCATE, 不受防全表删除插件拦截; 不返回影响行数, 调用方需自行统计)
     */
    int clearSysLog();

    /**
     * 删除操作时间早于截止时间的日志
     * @param cutoff 截止时间(早于该时间的记录被删除)
     * @return 删除行数
     */
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);

}
