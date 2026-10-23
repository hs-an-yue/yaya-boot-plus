package com.yaya.monitor.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.DesensitizeUtils;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.monitor.domain.entity.SysLog;
import com.yaya.monitor.domain.query.SysLogQuery;
import com.yaya.monitor.domain.vo.SysLogVo;
import com.yaya.monitor.mapper.SysLogMapper;
import com.yaya.monitor.service.SysLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
public class SysLogServiceImpl implements SysLogService {

    /** 数据库日志默认保留天数(配置缺省时的兜底值) */
    private static final int DEFAULT_LOG_RETENTION_DAYS = 30;

    @Resource
    private SysLogMapper sysLogMapper;

    @Resource
    private YaYaConfig yaYaConfig;

    @Async("logExecutor") //配置自定义线程池 com.yaya.framework.config.AsyncConfig
    @Override
    public void saveSysLog(SysLog sysLog) {
        sysLogMapper.insert(sysLog);
    }

    @Override
    public PageDTO<SysLogVo> getSysLogPage(SysLogQuery query) {
        Page<SysLogVo> sysLogPage = sysLogMapper.getSysLogPage(new Page<SysLogVo>(query.getPageNum(), query.getPageSize()), query.getLogType(), query.getOperName(), query.getUsername(), query.getRequestUrl(), query.getStatus(), query.getIp(), query.getTrackId(), query.getStartTime(), query.getEndTime());
        PageDTO<SysLogVo> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) sysLogPage.getCurrent());
        pageDTO.setPageSize((int) sysLogPage.getSize());
        pageDTO.setTotal(sysLogPage.getTotal());
        pageDTO.setRecords(sysLogPage.getRecords().stream().peek(x->{
            String ip = x.getIp();
            if(StringUtils.isNotEmpty(ip)){
                //IP脱敏
                x.setIp(DesensitizeUtils.desensitizeIp(ip));
            }
        }).toList());
        return pageDTO;
    }

    @Override
    public SysLogVo getSysLogDetail(Long logId) {
        if (logId == null) {
            throw new GlobalCommonException("日志ID不能为空");
        }
        SysLogVo sysLogVo = sysLogMapper.getSysLogDetail(logId);
        if (sysLogVo == null) {
            throw new GlobalCommonException("日志不存在");
        }
        return sysLogVo;
    }

    /**
     * 清空日志表(TRUNCATE 物理清空)
     * 访问权限由控制器层的 @PreAuthorize 控制(仅ROOT和运营管理员)
     */
    @Override
    public int clearSysLog() {
        // TRUNCATE 不返回影响行数, 先查当前行数作为清空数量返回
        Long count = sysLogMapper.selectCount(null);
        sysLogMapper.clearSysLog();
        int deleted = count == null ? 0 : count.intValue();
        log.info("日志清空-操作人={}, 删除行数={}", SecurityUtils.getUsername(), deleted);
        return deleted;
    }

    /**
     * 清理超过保留天数的日志, 保留天数复用 yaya.log-retention-days(与日志文件保留天数同一个配置)
     */
    @Override
    public int cleanExpiredLogs() {
        //获取日志保留天数
        Integer retentionDays = yaYaConfig.getLogRetentionDays();
        int days = Objects.requireNonNullElse(retentionDays, DEFAULT_LOG_RETENTION_DAYS);
        if (days <= 0) {
            log.warn("日志清理-保留天数配置不合法({}), 跳过清理", days);
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = sysLogMapper.deleteBefore(cutoff);
        log.info("日志清理定时任务启动-保留天数={}, 截止时间={}, 删除行数={}", days, cutoff, deleted);
        return deleted;
    }
}
