package com.yaya.system.service;

import com.yaya.framework.dto.PageDTO;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.query.SysFileQuery;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件 Service 接口
 *
 * @author yaya
 */
public interface SysFileService {

    /**
     * 分页查询文件
     */
    PageDTO<SysFile> getFilePage(SysFileQuery query);

    /**
     * 查询文件详情
     *
     * @param fileId 文件ID
     */
    SysFile getFileDetail(Long fileId);

    /**
     * 上传文件(保存到本地磁盘)
     *
     * @param file 上传的文件
     * @return 文件记录(含访问地址)
     */
    SysFile uploadFile(MultipartFile file);

    /**
     * 读取文件内容(下载用)
     *
     * @param fileId 文件ID
     * @return 磁盘文件资源
     */
    FileSystemResource downloadFile(Long fileId);

    /**
     * 删除文件(仍被公告附件、正文或用户头像引用时拒绝删除)
     *
     * @param fileId 文件ID
     */
    boolean deleteFile(Long fileId);
}
