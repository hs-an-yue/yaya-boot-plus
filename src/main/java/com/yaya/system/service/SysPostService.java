package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.entity.SysPost;
import com.yaya.system.domain.form.SysPostForm;
import com.yaya.system.domain.query.SysPostQuery;
import com.yaya.system.domain.vo.SysPostVo;

import java.util.List;

/**
 * 岗位管理 Service
 *
 * @author yaya
 */
public interface SysPostService {

    /**
     * 岗位分页(含部门名称、创建人、关联用户数)
     *
     * @param query 查询条件
     * @return 分页数据
     */
    PageDTO<SysPostVo> getPostPage(SysPostQuery query);

    /**
     * 岗位列表(下拉选择用, 包含停用岗位, 前端按 status 过滤)
     *
     * @return 未删除的岗位列表
     */
    List<SysPost> getPostList();

    /**
     * 岗位详情
     *
     * @param postId 岗位ID
     * @return 岗位详情
     */
    SysPostVo getPostDetail(Long postId);

    /**
     * 新增岗位
     * 岗位编号全局唯一(含已逻辑删除记录); 部门可选, 传入时校验存在
     *
     * @param form 岗位表单
     * @return 是否成功
     */
    boolean addPost(SysPostForm form);

    /**
     * 更新岗位
     * 岗位已关联未删除用户时, 不允许停用
     *
     * @param form 岗位表单
     * @return 是否成功
     */
    boolean updatePost(SysPostForm form);

    /**
     * 删除岗位(逻辑删除)
     * 岗位已关联未删除用户时, 不允许删除
     *
     * @param postId 岗位ID
     * @return 是否成功
     */
    boolean deletePost(Long postId);

    /**
     * 启用/停用岗位
     * 岗位已关联未删除用户时, 不允许停用
     *
     * @param postId 岗位ID
     * @param status 状态(1-正常,0-停用)
     * @return 是否成功
     */
    boolean updatePostStatus(Long postId, Integer status);
}
