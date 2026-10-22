package com.yaya.monitor.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.monitor.domain.entity.SysLog;
import com.yaya.monitor.domain.query.SysLogQuery;
import com.yaya.monitor.domain.vo.SysLogVo;

public interface SysLogService {
    /**
     * 添加
     * @param sysLog 日志信息
     */
    void saveSysLog(SysLog sysLog);

    /**
     * 日志分页
     * @param  query 搜索条件
     * @return 日志列表
     */
    PageDTO<SysLogVo> getSysLogPage(SysLogQuery query);

    /**
     * 日志详情
     *
     * @param logId 日志ID
     * @return 日志详情
     */
    SysLogVo getSysLogDetail(Long logId);

    /**
     * 清空日志表(TRUNCATE 物理清空)
     * 访问权限由控制器层的 @PreAuthorize 控制(仅ROOT和运营管理员)
     * @return 删除行数
     */
    int clearSysLog();

    /**
     * 清理超过保留天数(yaya.log-retention-days, 默认30天)的日志, 供定时任务调用
     * @return 删除行数
     */
    int cleanExpiredLogs();
}
