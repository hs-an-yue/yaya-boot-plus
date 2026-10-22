package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.annotation.DataScope;
import com.yaya.system.domain.entity.SysFile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件管理表 Mapper 接口
 * <p>
 * 自定义 SQL 统一写在 resources/mapper/system/SysFileMapper.xml, 接口中不允许写注解 SQL
 *
 * @author yaya
 */
public interface SysFileMapper extends BaseMapper<SysFile> {

    /** 文件分页 **/
    @DataScope(userAlias = "f",userIdColumnName = "create_by",deptAlias = "f",deptIdColumnName = "dept_id")
    IPage<SysFile> selectFilePage(Page<SysFile> page,@Param(Constants.WRAPPER) Wrapper<SysFile> queryWrapper);

    /** 未删除公告(包含草稿)是否仍将文件作为附件使用 */
    boolean hasNoticeAttachmentReference(@Param("fileId") Long fileId);

    /** 查询可能引用文件的未删除用户头像, 文件名中的 LIKE 通配符由调用方转义 */
    List<String> selectAvatarReferenceCandidates(@Param("fileNamePattern") String fileNamePattern);

    /** 查询可能引用文件的未删除公告正文, 再由调用方解析实际 HTML 属性 */
    List<String> selectNoticeContentReferenceCandidates(@Param("fileNamePattern") String fileNamePattern);
}
