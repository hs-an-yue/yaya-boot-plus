package com.yaya.system.mapper;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件引用 Mapper 集成测试。
 * 使用独立测试数据验证真实 SQL，事务结束后自动回滚，不修改已有业务记录。
 */
@SpringBootTest
@Transactional
class SysFileMapperTest {

    @Resource
    private SysFileMapper sysFileMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("附件引用：未删除的草稿和已发布公告均受保护")
    void hasNoticeAttachmentReferenceIncludesDraftAndPublishedNotices() {
        Long draftFileId = insertFile(uniqueMarker() + ".txt");
        Long publishedFileId = insertFile(uniqueMarker() + ".txt");
        insertAttachment(insertNotice(0, 0, "<p>草稿</p>"), draftFileId);
        insertAttachment(insertNotice(1, 0, "<p>已发布</p>"), publishedFileId);

        assertTrue(sysFileMapper.hasNoticeAttachmentReference(draftFileId));
        assertTrue(sysFileMapper.hasNoticeAttachmentReference(publishedFileId));
    }

    @Test
    @DisplayName("附件引用：已删除公告和孤立关联不阻止删除")
    void hasNoticeAttachmentReferenceIgnoresDeletedAndMissingNotices() {
        Long deletedNoticeFileId = insertFile(uniqueMarker() + ".txt");
        Long orphanFileId = insertFile(uniqueMarker() + ".txt");
        Long unreferencedFileId = insertFile(uniqueMarker() + ".txt");
        insertAttachment(insertNotice(1, 1, "<p>已删除</p>"), deletedNoticeFileId);
        insertAttachment(IdWorker.getId(), orphanFileId);

        assertFalse(sysFileMapper.hasNoticeAttachmentReference(deletedNoticeFileId));
        assertFalse(sysFileMapper.hasNoticeAttachmentReference(orphanFileId));
        assertFalse(sysFileMapper.hasNoticeAttachmentReference(unreferencedFileId));
    }

    @Test
    @DisplayName("头像候选：包含停用用户，排除已删除用户和无关地址")
    void selectAvatarReferenceCandidatesIncludesDisabledButNotDeletedUsers() {
        String marker = uniqueMarker();
        String fileName = marker + ".png";
        String activeAvatar = "file/2026/10/21/" + fileName;
        String disabledAvatar = "/api/file/2026/10/21/" + fileName;
        String deletedAvatar = "https://old.example/file/2026/10/21/" + fileName;
        insertUser(activeAvatar, 1, 0);
        insertUser(disabledAvatar, 0, 0);
        insertUser(deletedAvatar, 1, 1);
        insertUser("file/2026/10/21/" + marker + ".jpg", 1, 0);

        List<String> candidates = testCandidates(
                sysFileMapper.selectAvatarReferenceCandidates(fileName), marker);

        assertEquals(2, candidates.size());
        assertEquals(Set.of(activeAvatar, disabledAvatar), new HashSet<>(candidates));
    }

    @Test
    @DisplayName("富文本候选：包含草稿和已发布公告，排除已删除公告和无关内容")
    void selectNoticeContentReferenceCandidatesIncludesOnlyUndeletedNotices() {
        String marker = uniqueMarker();
        String fileName = marker + ".png";
        String draftContent = "<img src=\"file/2026/10/21/" + fileName + "\">";
        String publishedContent = "<video poster=\"/api/file/2026/10/21/"
                + fileName + "\"></video>";
        String deletedContent = "<a href=\"https://old.example/file/2026/10/21/"
                + fileName + "\">附件</a>";
        insertNotice(0, 0, draftContent);
        insertNotice(1, 0, publishedContent);
        insertNotice(1, 1, deletedContent);
        insertNotice(1, 0, "<p>" + marker + ".jpg</p>");

        List<String> candidates = testCandidates(
                sysFileMapper.selectNoticeContentReferenceCandidates(fileName), marker);

        assertEquals(2, candidates.size());
        assertEquals(Set.of(draftContent, publishedContent), new HashSet<>(candidates));
    }

    @Test
    @DisplayName("候选匹配：感叹号和下划线按字面值匹配，不匹配相似文件名")
    void referenceCandidateQueriesHonorEscapedLikeWildcards() {
        String marker = uniqueMarker();
        String fileName = marker + "!_file.png";
        // ESCAPE '!'：原始 !、_ 分别编码为 !!、!_。
        String escapedPattern = marker + "!!!_file.png";
        String avatar = "file/2026/10/21/" + fileName;
        String content = "<img src=\"/api/file/2026/10/21/" + fileName + "\">";
        insertUser(avatar, 1, 0);
        insertNotice(0, 0, content);
        for (String similarName : List.of(
                marker + "!XXfile.png",
                marker + "!afile.png",
                marker + "_file.png")) {
            insertUser("file/2026/10/21/" + similarName, 1, 0);
            insertNotice(1, 0, "<img src=\"/api/file/2026/10/21/" + similarName + "\">");
        }

        assertEquals(List.of(avatar), testCandidates(
                sysFileMapper.selectAvatarReferenceCandidates(escapedPattern), marker));
        assertEquals(List.of(content),
                testCandidates(sysFileMapper.selectNoticeContentReferenceCandidates(escapedPattern), marker));
    }

    @Test
    @DisplayName("百分号编码地址：未出现明文文件名时仍纳入候选，已删除记录仍排除")
    void referenceCandidateQueriesIncludeEncodedUrlsWithoutPlainFileName() {
        String fileName = uniqueMarker() + ".png";
        String encodedFileName = "%69" + fileName.substring(1);
        String encodedAvatar = "file/2026/10/21/" + encodedFileName;
        String deletedAvatar = "https://old.example/file/2026/10/21/" + encodedFileName;
        String encodedContent = "<img src=\"/api/file/2026/10/21/" + encodedFileName + "\">";
        String deletedContent = "<video poster=\"/file/2026/10/21/" + encodedFileName + "\"></video>";
        insertUser(encodedAvatar, 0, 0);
        insertUser(deletedAvatar, 1, 1);
        insertNotice(0, 0, encodedContent);
        insertNotice(1, 1, deletedContent);

        List<String> avatars = sysFileMapper.selectAvatarReferenceCandidates(fileName);
        List<String> contents = sysFileMapper.selectNoticeContentReferenceCandidates(fileName);

        assertFalse(encodedAvatar.contains(fileName));
        assertTrue(avatars.contains(encodedAvatar));
        assertFalse(avatars.contains(deletedAvatar));
        assertTrue(contents.contains(encodedContent));
        assertFalse(contents.contains(deletedContent));
    }

    @Test
    @DisplayName("富文本候选：HTML实体编码的文件名也进入候选集")
    void noticeContentCandidatesIncludeHtmlEntityEncodedFileNames() {
        String fileName = uniqueMarker() + ".png";
        String encodedFileName = "&#105;" + fileName.substring(1);
        String content = "<img src='/file/2026/10/21/" + encodedFileName + "'>";
        String deletedContent = "<a href='/file/2026/10/21/" + encodedFileName + "'>已删除</a>";
        insertNotice(0, 0, content);
        insertNotice(1, 1, deletedContent);

        List<String> candidates = sysFileMapper.selectNoticeContentReferenceCandidates(fileName);

        assertFalse(content.contains(fileName));
        assertTrue(candidates.contains(content));
        assertFalse(candidates.contains(deletedContent));
    }

    private Long insertFile(String fileName) {
        Long fileId = IdWorker.getId();
        jdbcTemplate.update("INSERT INTO sys_file "
                        + "(file_id, file_name, file_ext, file_local_url, file_server_url) "
                        + "VALUES (?, ?, ?, ?, ?)",
                fileId, fileName, "txt", "/tmp/" + fileName, "file/2026/10/21/" + fileName);
        return fileId;
    }

    private Long insertNotice(int status, int isDeleted, String content) {
        Long typeId = IdWorker.getId();
        jdbcTemplate.update("INSERT INTO sys_notice_type "
                        + "(notice_type_id, notice_type_name, status, is_deleted) VALUES (?, ?, ?, ?)",
                typeId, uniqueMarker(), 1, 0);
        Long noticeId = IdWorker.getId();
        jdbcTemplate.update("INSERT INTO sys_notice "
                        + "(notice_id, notice_title, notice_content, notice_level, notice_type_id, "
                        + "status, publish_scope, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                noticeId, uniqueMarker(), content, 0, typeId, status, 1, isDeleted);
        return noticeId;
    }

    private void insertAttachment(Long noticeId, Long fileId) {
        jdbcTemplate.update("INSERT INTO sys_notice_file (notice_id, file_id) VALUES (?, ?)",
                noticeId, fileId);
    }

    private void insertUser(String avatar, int isEnabled, int isDeleted) {
        jdbcTemplate.update("INSERT INTO sys_user "
                        + "(user_id, username, password, nickname, avatar, is_enabled, is_locked, is_deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                IdWorker.getId(), uniqueMarker(), "unused-test-password", "文件引用测试",
                avatar, isEnabled, 0, isDeleted);
    }

    private String uniqueMarker() {
        return "itfileref" + UUID.randomUUID().toString().replace("-", "");
    }

    private List<String> testCandidates(List<String> candidates, String marker) {
        // 百分号兜底分支可能返回已有编码 URL；只比较本测试插入的数据。
        return candidates.stream().filter(value -> value.contains(marker)).toList();
    }
}
