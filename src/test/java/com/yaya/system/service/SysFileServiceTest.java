package com.yaya.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.dto.PageDTO;
import com.yaya.framework.exception.GlobalCommonException;
import com.yaya.framework.security.LoginUserDetails;
import com.yaya.system.domain.entity.SysDept;
import com.yaya.system.domain.entity.SysFile;
import com.yaya.system.domain.entity.SysUser;
import com.yaya.system.domain.query.SysFileQuery;
import com.yaya.system.mapper.SysFileMapper;
import com.yaya.system.service.impl.SysFileServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件 Service 单元测试
 * <p>
 * 使用 @TempDir 提供真实磁盘目录, Mapper 使用 Mockito 模拟, 重点验证:
 * 文件落盘与落库、审计字段、文件名清洗、路径穿越防护、删除时磁盘文件同步清理。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysFileServiceTest {

    @Mock
    private SysFileMapper sysFileMapper;

    @Mock
    private YaYaConfig yaYaConfig;

    @InjectMocks
    private SysFileServiceImpl sysFileService;

    @TempDir
    private Path tempDir;

    private Path baseDir;

    @BeforeEach
    void setUp() {
        baseDir = tempDir.toAbsolutePath().normalize();
        when(yaYaConfig.getFilePath()).thenReturn(baseDir.toString());
        when(sysFileMapper.insert(any(SysFile.class))).thenReturn(1);

        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUsername("root");
        user.setDeptId(10L);
        LoginUserDetails loginUser = new LoginUserDetails(
                user, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new SysDept());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("上传文件：落盘位置正确、落库字段完整")
    void uploadFileSavesFileAndRecord() throws IOException {
        byte[] content = "hello yaya".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "说明文档.txt", "text/plain", content);

        SysFile result = sysFileService.uploadFile(file);

        ArgumentCaptor<SysFile> captor = ArgumentCaptor.forClass(SysFile.class);
        verify(sysFileMapper).insert(captor.capture());
        SysFile record = captor.getValue();

        assertNotNull(record.getFileId());
        assertEquals(record.getFileId(), result.getFileId());
        assertEquals("说明文档.txt", record.getFileName());
        assertEquals("txt", record.getFileExt());
        Path storedPath = Paths.get(record.getFileLocalUrl());
        assertTrue(storedPath.startsWith(baseDir), "文件必须保存在配置的上传目录内");
        assertTrue(storedPath.startsWith(baseDir.resolve("file")), "文件必须存放到 file/ 子目录");
        String expectedRelativeUrl = baseDir.relativize(storedPath).toString().replace('\\', '/');
        assertEquals(expectedRelativeUrl, record.getFileServerUrl());
        assertFalse(record.getFileServerUrl().startsWith("http://"));
        assertFalse(record.getFileServerUrl().startsWith("https://"));
        assertTrue(record.getFileServerUrl().startsWith("file/"));
        assertEquals(1L, record.getCreateBy());
        assertEquals(1L, record.getUpdateBy());
        assertEquals(10L, record.getDeptId());
        assertNotNull(record.getCreateTime());
        assertNotNull(record.getUpdateTime());
        assertTrue(Files.isRegularFile(storedPath), "磁盘文件必须存在");
        assertArrayEqualsBytes(content, Files.readAllBytes(storedPath));
    }

    @Test
    @DisplayName("上传文件：空文件拒绝且不落库")
    void uploadFileRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.uploadFile(file));

        assertEquals("上传文件不能为空", exception.getMessage());
        verify(sysFileMapper, never()).insert(any(SysFile.class));
    }

    @Test
    @DisplayName("上传文件：原始文件名中的路径被剥离, 不能穿越上传目录")
    void uploadFileStripsPathFromOriginalName() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "..\\..\\evil.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        SysFile result = sysFileService.uploadFile(file);

        assertEquals("evil.txt", result.getFileName());
        Path storedPath = Paths.get(result.getFileLocalUrl());
        assertTrue(storedPath.startsWith(baseDir));
        assertTrue(Files.isRegularFile(storedPath));
    }

    @Test
    @DisplayName("上传文件：文件名超过200字符时截断")
    void uploadFileTruncatesLongFileName() {
        String longName = "a".repeat(220) + ".txt";
        MockMultipartFile file = new MockMultipartFile(
                "file", longName, "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        SysFile result = sysFileService.uploadFile(file);

        assertEquals(200, result.getFileName().codePointCount(0, result.getFileName().length()));
    }

    @Test
    @DisplayName("上传文件：无后缀时 fileExt 为空且磁盘文件无扩展名")
    void uploadFileWithoutExtensionStoresEmptyFileExt() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "README", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        SysFile result = sysFileService.uploadFile(file);

        assertEquals("", result.getFileExt());
        assertFalse(Paths.get(result.getFileLocalUrl()).getFileName().toString().contains("."));
    }

    @Test
    @DisplayName("上传文件：后缀统一转小写并落库")
    void uploadFileStoresLowerCaseExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "Photo.PNG", "image/png", "x".getBytes(StandardCharsets.UTF_8));

        SysFile result = sysFileService.uploadFile(file);

        assertEquals("png", result.getFileExt());
        assertTrue(result.getFileLocalUrl().endsWith(".png"));
    }

    @Test
    @DisplayName("上传文件：非法后缀不落库且磁盘文件无扩展名")
    void uploadFileIgnoresIllegalExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.file@name", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        SysFile result = sysFileService.uploadFile(file);

        assertEquals("", result.getFileExt());
        assertFalse(Paths.get(result.getFileLocalUrl()).getFileName().toString().contains("."));
    }

    @Test
    @DisplayName("上传文件：文件名为空时使用默认名称")
    void uploadFileUsesDefaultNameWhenOriginalNameBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        SysFile result = sysFileService.uploadFile(file);

        assertEquals("未命名文件", result.getFileName());
        assertTrue(Files.isRegularFile(Paths.get(result.getFileLocalUrl())));
    }

    @Test
    @DisplayName("上传文件：落库失败时清理已保存的磁盘文件")
    void uploadFileDeletesStoredFileWhenInsertFails() throws IOException {
        when(sysFileMapper.insert(any(SysFile.class)))
                .thenThrow(new RuntimeException("数据库异常"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "cleanup.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class, () -> sysFileService.uploadFile(file));

        assertEquals(0, countRegularFiles(baseDir), "落库失败后不能残留磁盘文件");
    }

    @Test
    @DisplayName("下载文件：读取磁盘内容")
    void downloadFileReturnsResource() throws IOException {
        Path localFile = baseDir.resolve("2026/10/19/download.txt");
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "download content", StandardCharsets.UTF_8);
        SysFile record = fileRecord(100L, "下载.txt", localFile);
        when(sysFileMapper.selectById(100L)).thenReturn(record);

        FileSystemResource resource = sysFileService.downloadFile(100L);

        assertTrue(resource.exists());
        assertEquals("download content",
                new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("下载文件：记录不存在时拒绝")
    void downloadFileRejectsMissingRecord() {
        when(sysFileMapper.selectById(404L)).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.downloadFile(404L));

        assertEquals("文件不存在", exception.getMessage());
    }

    @Test
    @DisplayName("下载文件：数据库路径指向上传目录外时拒绝")
    void downloadFileRejectsPathOutsideRoot() {
        Path outsideFile = baseDir.getParent().resolve("outside.txt");
        SysFile record = fileRecord(101L, "越权.txt", outsideFile);
        when(sysFileMapper.selectById(101L)).thenReturn(record);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.downloadFile(101L));

        assertEquals("非法的文件路径", exception.getMessage());
    }

    @Test
    @DisplayName("下载文件：磁盘文件缺失时拒绝")
    void downloadFileRejectsMissingPhysicalFile() {
        Path missingFile = baseDir.resolve("2026/10/19/missing.txt");
        SysFile record = fileRecord(102L, "缺失.txt", missingFile);
        when(sysFileMapper.selectById(102L)).thenReturn(record);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.downloadFile(102L));

        assertEquals("文件不存在或已被删除", exception.getMessage());
    }

    @Test
    @DisplayName("删除文件：同时删除数据库记录与磁盘文件")
    void deleteFileDeletesRecordAndPhysicalFile() throws IOException {
        Path localFile = baseDir.resolve("2026/10/19/delete.txt");
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "to delete", StandardCharsets.UTF_8);
        SysFile record = fileRecord(103L, "删除.txt", localFile);
        when(sysFileMapper.selectById(103L)).thenReturn(record);
        when(sysFileMapper.deleteById(103L)).thenReturn(1);

        boolean result = sysFileService.deleteFile(103L);

        assertTrue(result);
        verify(sysFileMapper).deleteById(103L);
        assertFalse(Files.exists(localFile), "数据库记录删除后磁盘文件也必须删除");
    }

    @Test
    @DisplayName("删除文件：记录不存在时拒绝")
    void deleteFileRejectsMissingRecord() {
        when(sysFileMapper.selectById(404L)).thenReturn(null);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.deleteFile(404L));

        assertEquals("文件不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除文件：数据库路径非法时拒绝删除")
    void deleteFileRejectsIllegalPath() {
        Path outsideFile = baseDir.getParent().resolve("outside-delete.txt");
        SysFile record = fileRecord(104L, "越权删除.txt", outsideFile);
        when(sysFileMapper.selectById(104L)).thenReturn(record);

        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.deleteFile(104L));

        assertEquals("非法的文件路径", exception.getMessage());
        verify(sysFileMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("删除文件：公告附件仍在使用时保留数据库记录和磁盘文件")
    void deleteFileRejectsNoticeAttachmentReference() throws IOException {
        Path localFile = referencedFile();
        when(sysFileMapper.hasNoticeAttachmentReference(110L)).thenReturn(true);

        assertDeleteRejected(localFile, "公告附件");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file/2026/10/21/used.png",
            "/file/2026/10/21/used.png",
            "/api/file/2026/10/21/used.png",
            "./file/2026/10/21/used.png",
            "../../file/2026/10/21/used.png",
            "https://old.example/file/2026/10/21/used.png?size=100#avatar",
            "http://old.example:8081/api/file/2026/10/21/used.png",
            "//old.example/file/2026/10/21/used.png",
            "/api/file/2026/10/21/%75sed.png"
    })
    @DisplayName("删除文件：相对、代理、历史绝对及编码头像地址均受保护")
    void deleteFileRejectsAvatarReference(String avatar) throws IOException {
        Path localFile = referencedFile();
        when(sysFileMapper.selectAvatarReferenceCandidates("used.png")).thenReturn(List.of(avatar));

        assertDeleteRejected(localFile, "头像");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<img src='/api/file/2026/10/21/used.png'>",
            "<a href='https://old.example/file/2026/10/21/used.png?x=1&amp;y=2'>下载</a>",
            "<video src='file/2026/10/21/used.png' controls></video>",
            "<video poster='/file/2026/10/21/used.png'></video>",
            "<video><source src='/file/2026/10/21/%75sed.png'></video>",
            "<img src='/file/2026/10/21/&#117;sed.png'>"
    })
    @DisplayName("删除文件：公告正文的图片、视频、链接和封面引用均受保护")
    void deleteFileRejectsNoticeContentReference(String content) throws IOException {
        Path localFile = referencedFile();
        when(sysFileMapper.selectNoticeContentReferenceCandidates("used.png")).thenReturn(List.of(content));

        assertDeleteRejected(localFile, "公告正文");
    }

    @Test
    @DisplayName("删除文件：普通文本、相似文件名和其他目录的同名文件不算引用")
    void deleteFileDoesNotRejectSimilarUrlsOrPlainText() throws IOException {
        Path localFile = referencedFile();
        when(sysFileMapper.selectAvatarReferenceCandidates("used.png")).thenReturn(List.of(
                "/file/other/used.png", "/file/2026/10/21/unused.png"));
        when(sysFileMapper.selectNoticeContentReferenceCandidates("used.png")).thenReturn(List.of(
                "<p>示例地址 /file/2026/10/21/used.png</p>",
                "<img src='/file/2026/10/21/used.png.backup'><img src='/file/other/used.png'>"));
        when(sysFileMapper.deleteById(110L)).thenReturn(1);

        assertTrue(sysFileService.deleteFile(110L));
        assertFalse(Files.exists(localFile));
    }

    @Test
    @DisplayName("删除文件：解除头像引用后可以正常删除")
    void deleteFileSucceedsAfterReferenceRemoved() throws IOException {
        Path localFile = referencedFile();
        when(sysFileMapper.selectAvatarReferenceCandidates("used.png"))
                .thenReturn(List.of("file/2026/10/21/used.png"), List.of());
        assertDeleteRejected(localFile, "头像");
        when(sysFileMapper.deleteById(110L)).thenReturn(1);

        assertTrue(sysFileService.deleteFile(110L));
        assertFalse(Files.exists(localFile));
    }

    @Test
    @DisplayName("删除文件：URL 中的加号不被当作空格")
    void deleteFilePreservesPlusInUrlPath() throws IOException {
        Path localFile = baseDir.resolve("file/2026/10/21/a+b.png");
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "image");
        SysFile record = fileRecord(110L, "图片.png", localFile);
        record.setFileServerUrl("file/2026/10/21/a+b.png");
        when(sysFileMapper.selectById(110L)).thenReturn(record);
        when(sysFileMapper.selectAvatarReferenceCandidates("a+b.png"))
                .thenReturn(List.of("/api/file/2026/10/21/a%2Bb.png"));

        assertDeleteRejected(localFile, "头像");
    }

    @Test
    @DisplayName("上传文件：外层业务事务回滚时清理新文件")
    void uploadFileRemovesNewFileOnTransactionRollback() {
        TransactionSynchronizationManager.initSynchronization();
        SysFile uploaded = sysFileService.uploadFile(new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}));
        Path localFile = Paths.get(uploaded.getFileLocalUrl());
        assertTrue(Files.exists(localFile));

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        assertFalse(Files.exists(localFile));
    }

    @Test
    @DisplayName("上传文件：事务提交后保留新文件")
    void uploadFileKeepsNewFileOnTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        SysFile uploaded = sysFileService.uploadFile(new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}));
        Path localFile = Paths.get(uploaded.getFileLocalUrl());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }

        assertTrue(Files.exists(localFile));
    }

    private Path referencedFile() throws IOException {
        Path localFile = baseDir.resolve("file/2026/10/21/used.png");
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "image");
        SysFile record = fileRecord(110L, "图片.png", localFile);
        record.setFileServerUrl("file/2026/10/21/used.png");
        when(sysFileMapper.selectById(110L)).thenReturn(record);
        return localFile;
    }

    private void assertDeleteRejected(Path localFile, String referenceType) {
        GlobalCommonException exception = assertThrows(GlobalCommonException.class,
                () -> sysFileService.deleteFile(110L));
        assertTrue(exception.getMessage().contains(referenceType));
        verify(sysFileMapper, never()).deleteById(any(Long.class));
        assertTrue(Files.exists(localFile), "引用中的文件必须保留");
    }

    @Test
    @DisplayName("文件分页：返回总数和当前页数据")
    void getFilePageReturnsPageData() {
        SysFile record = fileRecord(105L, "分页.txt", baseDir.resolve("2026/10/19/page.txt"));
        when(sysFileMapper.selectFilePage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<SysFile> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });
        SysFileQuery query = new SysFileQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setFileName("分页");

        PageDTO<SysFile> pageDTO = sysFileService.getFilePage(query);

        assertEquals(1, pageDTO.getPageNum());
        assertEquals(10, pageDTO.getPageSize());
        assertEquals(1, pageDTO.getTotal());
        assertEquals(List.of(record), pageDTO.getRecords());
    }

    private SysFile fileRecord(Long fileId, String fileName, Path localPath) {
        SysFile record = new SysFile();
        record.setFileId(fileId);
        record.setFileName(fileName);
        record.setFileLocalUrl(localPath.toString().replace('\\', '/'));
        record.setFileServerUrl("http://127.0.0.1:8081/" + localPath.getFileName());
        return record;
    }

    private long countRegularFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    private void assertArrayEqualsBytes(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
}
