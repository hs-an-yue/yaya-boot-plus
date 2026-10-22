package com.yaya.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页统计出参
 */
@Data
@Schema(description = "首页统计")
public class SysDashboardVo implements Serializable {

    @Schema(description = "未删除用户总数")
    private Long totalUsers;

    @Schema(description = "今年新增用户数")
    private Long thisYearNewUsers;

    @Schema(description = "本周新增用户数")
    private Long weekNewUsers;

    @Schema(description = "本周新增较上周同期百分比, 基数为0时为null")
    private Double weekCompareRate;

    @Schema(description = "本月新增用户数")
    private Long monthNewUsers;

    @Schema(description = "本月新增较上月同期百分比, 基数为0时为null")
    private Double monthCompareRate;

    @Schema(description = "当前在线登录会话数")
    private Long onlineUsers;

    @Schema(description = "在线数据更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime onlineUpdatedAt;

    @Schema(description = "近7天每日新增用户")
    private List<TrendPoint> userGrowth7d;

    @Schema(description = "近12个月每月新增用户")
    private List<TrendPoint> userGrowth12m;

    /**
     * 趋势图数据点
     */
    @Data
    @Schema(description = "趋势数据点")
    public static class TrendPoint implements Serializable {

        @Schema(description = "展示标签, 例如周一/10月")
        private String label;

        @Schema(description = "日期或月份标识")
        private String key;

        @Schema(description = "新增用户数")
        private Long value;

        public TrendPoint() {
        }

        public TrendPoint(String label, String key, Long value) {
            this.label = label;
            this.key = key;
            this.value = value;
        }
    }
}
