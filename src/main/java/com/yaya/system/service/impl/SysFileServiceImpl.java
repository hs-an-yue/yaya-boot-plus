package com.yaya.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.utils.SecurityUtils;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.query.SysFileQuery;
import com.yaya.system.mapper.SysFileMapper;
import com.yaya.system.service.SysFileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文件 Service 实现
 * <p>
 * 文件保存到 yaya.file-path 配置的本地磁盘目录, 按 yyyy/MM/dd 分目录存放;
 * 数据库只保存记录, 磁盘文件名使用 UUID, 原始文件名仅用于展示与下载.
 *
 * @author yaya
 */
@Slf4j
@Service
@Transactional
public class SysFileServiceImpl implements SysFileService {

    /**
     * 磁盘存储的日期子目录格式
     */
    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 文件名称最大长度(与 sys_file.file_name 字段长度一致)
     */
    private static final int FILE_NAME_MAX_LENGTH = 200;

    /**
     * 保留的扩展名最大长度, 超长或不合法时忽略扩展名
     */
    private static final int EXTENSION_MAX_LENGTH = 20;

    @Resource
    private SysFileMapper sysFileMapper;

    @Resource
    private YaYaConfig yaYaConfig;

    @Override
    public PageDTO<SysFile> getFilePage(SysFileQuery query) {
        QueryWrapper<SysFile> wrapper = new QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getFileName()), "file_name", query.getFileName())
                .ge(query.getStartTime() != null, "create_time", query.getStartTime())
                .le(query.getEndTime() != null, "create_time", query.getEndTime())
                .orderByDesc("create_time")
                .orderByDesc("file_id");
        IPage<SysFile> page = sysFileMapper.selectFilePage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        PageDTO<SysFile> pageDTO = new PageDTO<>();
        pageDTO.setPageNum((int) page.getCurrent());
        pageDTO.setPageSize((int) page.getSize());
        pageDTO.setTotal(page.getTotal());
        pageDTO.setRecords(page.getRecords());
        return pageDTO;
    }

    @Override
    public SysFile getFileDetail(Long fileId) {
        SysFile file = sysFileMapper.selectById(fileId);
        if (file == null) {
            throw new GlobalCommonException("文件不存在");
        }
        return file;
    }

    @Override
    public SysFile uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GlobalCommonException("上传文件不能为空");
        }
        // 原始文件名仅用于展示与下载, 必须去掉客户端携带的路径部分
        String fileName = sanitizeFileName(file.getOriginalFilename());
        // 文件后缀入库用于前端类型图标展示, 无后缀或非法后缀保存为空串
        String fileExt = extractFileExtension(fileName);
        Path baseDir = getBaseDir();
        // 统一存放在 file/ 子目录下, 对应静态资源 /file/** 的免鉴权访问路径
        Path relativeDir = Paths.get("file", LocalDate.now().format(DATE_DIR_FORMATTER));
        Path targetDir = baseDir.resolve(relativeDir).normalize();
        String storedName = IdUtil.fastSimpleUUID() + (fileExt.isEmpty() ? "" : "." + fileExt);
        Path targetPath = targetDir.resolve(storedName).normalize();
        // 保存目录必须在配置的上传根目录内, 防止路径穿越
        if (!targetDir.startsWith(baseDir) || !targetPath.startsWith(baseDir)) {
            throw new GlobalCommonException("非法的文件路径");
        }

        // 1. 先落盘, 保存失败直接中断
        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("文件保存失败, 文件名={}", fileName, e);
            deleteQuietly(targetPath);
            throw new GlobalCommonException("文件保存失败");
        }

        // 2. 再落库, 落库失败时清理已保存的磁盘文件
        Long fileId = IdWorker.getId();
        LocalDateTime now = LocalDateTime.now();
        SysFile sysFile = new SysFile();
        sysFile.setFileId(fileId);
        sysFile.setFileName(fileName);
        sysFile.setFileExt(fileExt);
        sysFile.setFileLocalUrl(targetPath.toAbsolutePath().toString().replace('\\', '/'));
        // 只保存相对上传根目录的访问路径, 由前端按当前文件服务地址拼 /api, 避免迁移服务器时改库
        sysFile.setFileServerUrl(buildFileRelativePath(relativeDir.resolve(storedName)));
        sysFile.setDeptId(SecurityUtils.getDeptId());
        sysFile.setCreateBy(SecurityUtils.getUserId());
        sysFile.setUpdateBy(SecurityUtils.getUserId());
        sysFile.setCreateTime(now);
        sysFile.setUpdateTime(now);
        try {
            if (sysFileMapper.insert(sysFile) <= 0) {
                throw new GlobalCommonException("文件记录保存失败");
            }
        } catch (RuntimeException e) {
            deleteQuietly(targetPath);
            throw e;
        }
        // 头像等业务可能在上传后失败; 外层事务回滚时同步移除新文件。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(targetPath);
                    }
                }
            });
        }
        return sysFile;
    }

    @Override
    public FileSystemResource downloadFile(Long fileId) {
        SysFile file = getFileDetail(fileId);
        Path path = resolveLocalPath(file.getFileLocalUrl());
        if (!Files.isRegularFile(path)) {
            throw new GlobalCommonException("文件不存在或已被删除");
        }
        return new FileSystemResource(path);
    }

    @Override
    public boolean deleteFile(Long fileId) {
        SysFile file = getFileDetail(fileId);
        // 先校验路径, 非法路径的记录不允许通过接口删除
        Path path = resolveLocalPath(file.getFileLocalUrl());
        assertFileNotReferenced(file, path);
        if (sysFileMapper.deleteById(fileId) <= 0) {
            return false;
        }
        // 数据库记录已删除, 磁盘文件删除失败只记录日志, 避免出现无法重试的"删不掉"状态
        deleteQuietly(path);
        return true;
    }

    /** 删除前保护现有业务引用, 已逻辑删除的公告/用户不再占用文件。 */
    private void assertFileNotReferenced(SysFile file, Path localPath) {
        if (sysFileMapper.hasNoticeAttachmentReference(file.getFileId())) {
            throw new GlobalCommonException("文件已被公告附件引用，请先移除引用后再删除");
        }
        String filePath = normalizeFileUrl(file.getFileServerUrl());
        if (filePath == null) {
            filePath = normalizeFileUrl(buildFileRelativePath(getBaseDir().relativize(localPath)));
        }
        if (filePath == null) {
            throw new GlobalCommonException("文件访问路径无效，无法检查业务引用");
        }
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String pattern = fileName.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        for (String avatar : sysFileMapper.selectAvatarReferenceCandidates(pattern)) {
            if (filePath.equals(normalizeFileUrl(avatar))) {
                throw new GlobalCommonException("文件正在用作用户头像，请先更换头像后再删除");
            }
        }
        for (String content : sysFileMapper.selectNoticeContentReferenceCandidates(pattern)) {
            if (content == null) {
                continue;
            }
            for (Element element : Jsoup.parseBodyFragment(content).select("[src],[href],[poster]")) {
                for (String attribute : List.of("src", "href", "poster")) {
                    if (filePath.equals(normalizeFileUrl(element.attr(attribute)))) {
                        throw new GlobalCommonException("文件已被公告正文引用，请先移除引用后再删除");
                    }
                }
            }
        }
    }

    /** 与前端文件地址解析一致, 历史域名和代理前缀不影响 /file/** 的归属。 */
    private String normalizeFileUrl(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            URI uri = new URI(value.trim().replace(" ", "%20"));
            String scheme = uri.getScheme();
            if (scheme != null && !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            String path = uri.getPath();
            if (StrUtil.isBlank(path)) {
                return null;
            }
            // URI 解码保留 + 字符, 并在解码后归一化点路径。
            path = new URI(null, null, path, null).normalize().getPath();
            int fileIndex = path.indexOf("/file/");
            if (fileIndex >= 0) {
                return path.substring(fileIndex);
            }
            return path.startsWith("/") ? path : "/" + path;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 获取上传根目录(绝对路径, 已规范化)
     */
    private Path getBaseDir() {
        String configuredPath = yaYaConfig.getFilePath();
        if (StrUtil.isBlank(configuredPath)) {
            throw new GlobalCommonException("文件上传目录未配置");
        }
        return Paths.get(configuredPath).toAbsolutePath().normalize();
    }

    /**
     * 把数据库中的文件路径解析为磁盘绝对路径, 并校验必须位于上传根目录内
     */
    private Path resolveLocalPath(String localUrl) {
        if (StrUtil.isBlank(localUrl)) {
            throw new GlobalCommonException("文件路径不能为空");
        }
        Path baseDir = getBaseDir();
        Path path = Paths.get(localUrl);
        if (!path.isAbsolute()) {
            path = baseDir.resolve(path);
        }
        path = path.normalize();
        if (!path.startsWith(baseDir)) {
            throw new GlobalCommonException("非法的文件路径");
        }
        return path;
    }

    /**
     * 生成文件相对上传根目录的访问路径, 不包含服务器地址
     *
     * @param relativePath 文件相对上传根目录的路径, 如 file/2026/10/19/xxx.txt
     */
    private String buildFileRelativePath(Path relativePath) {
        return relativePath.toString().replace('\\', '/');
    }

    /**
     * 清洗原始文件名: 去掉客户端携带的路径与控制字符, 并限制长度
     */
    private String sanitizeFileName(String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return "未命名文件";
        }
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        // 去除控制字符, 防止下载时响应头注入
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (StrUtil.isBlank(name) || ".".equals(name) || "..".equals(name)) {
            return "未命名文件";
        }
        if (name.codePointCount(0, name.length()) > FILE_NAME_MAX_LENGTH) {
            name = name.substring(0, name.offsetByCodePoints(0, FILE_NAME_MAX_LENGTH));
        }
        return name;
    }

    /**
     * 提取文件后缀: 小写、不含点、只保留字母数字; 无法提取时返回空串
     */
    private String extractFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(index + 1);
        if (extension.length() > EXTENSION_MAX_LENGTH || !extension.matches("[A-Za-z0-9]+")) {
            return "";
        }
        return extension.toLowerCase();
    }

    /**
     * 删除磁盘文件, 失败只记录日志
     */
    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("文件删除失败, 路径={}", path, e);
        }
    }
}
