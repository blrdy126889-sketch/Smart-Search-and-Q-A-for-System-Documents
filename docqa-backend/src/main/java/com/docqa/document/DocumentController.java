package com.docqa.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.docqa.category.mapper.BizCategoryMapper;
import com.docqa.common.api.PageResult;
import com.docqa.common.api.R;
import com.docqa.common.exception.BizException;
import com.docqa.common.util.CamelUtil;
import com.docqa.common.log.OpLog;
import com.docqa.common.util.SecurityUtils;
import com.docqa.document.entity.BizDocument;
import com.docqa.document.entity.BizDocVersion;
import com.docqa.document.mapper.BizDocChunkMapper;
import com.docqa.document.mapper.BizDocumentMapper;
import com.docqa.document.mapper.BizDocSocialMapper;
import com.docqa.document.mapper.BizDocVersionMapper;
import com.docqa.framework.storage.StorageClient;
import com.docqa.ingest.IngestPipeline;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * 制度文档管理：上传 / 分页检索 / 详情 / 审核 / 版本 / 收藏订阅 / diff / 重建索引
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final BizDocumentMapper documentMapper;
    private final BizDocVersionMapper versionMapper;
    private final BizDocChunkMapper chunkMapper;
    private final BizDocSocialMapper socialMapper;
    private final BizCategoryMapper categoryMapper;
    private final StorageClient storageClient;
    private final IngestPipeline ingestPipeline;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final com.docqa.framework.vector.ChromaVectorStore chroma;

    private static final Set<String> ALLOWED_EXT = Set.of("txt", "doc", "docx", "pdf");
    private static final long MAX_SIZE = 50L * 1024 * 1024;

    /* ==================== 上传（T1 事务） ==================== */

    @PostMapping("/documents/upload")
    @OpLog(module = "文档管理", operation = "上传文档")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam Long categoryId,
                                         @RequestParam(required = false) String docCode,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) String changeLog) {
        SecurityUtils.checkPerm("doc:upload");
        validateFile(file);
        String sourceType = normalizeType(extOf(file.getOriginalFilename()));

        String filePath;
        try (InputStream in = file.getInputStream()) {
            filePath = storageClient.save(in, file.getOriginalFilename());
        } catch (Exception e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }

        BizDocument doc = new BizDocument();
        doc.setDocCode(docCode != null && !docCode.isBlank() ? docCode : "DOC" + System.currentTimeMillis());
        doc.setTitle(title != null && !title.isBlank() ? title : stripExt(file.getOriginalFilename()));
        doc.setCategoryId(categoryId);
        doc.setStatus("DRAFT");
        doc.setSourceType(sourceType);
        doc.setFileSize(file.getSize());
        doc.setOwnerId(SecurityUtils.userId());
        // 编程式短事务：提交后才触发异步入库（避免异步线程读不到未提交记录）
        BizDocVersion version = transactionTemplate.execute(status -> {
            documentMapper.insert(doc);
            return insertVersion(doc, filePath, file, changeLog);
        });
        ingestPipeline.ingestAsync(version, sourceType);
        categoryMapper.refreshDocCount(categoryId);

        Map<String, Object> data = new HashMap<>();
        data.put("docId", doc.getId());
        data.put("versionId", version.getId());
        data.put("versionNo", version.getVersionNo());
        return R.ok(data);
    }

    /* ==================== 新版本上传 ==================== */

    @PostMapping("/documents/{id}/versions")
    @OpLog(module = "文档管理", operation = "上传新版本")
    public R<Map<String, Object>> uploadVersion(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam(required = false) String changeLog) {
        SecurityUtils.checkPerm("doc:upload");
        BizDocument doc = requireDoc(id);
        validateFile(file);
        String sourceType = normalizeType(extOf(file.getOriginalFilename()));
        String filePath;
        try (InputStream in = file.getInputStream()) {
            filePath = storageClient.save(in, file.getOriginalFilename());
        } catch (Exception e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        BizDocVersion version = transactionTemplate.execute(status -> insertVersion(doc, filePath, file, changeLog));
        ingestPipeline.ingestAsync(version, sourceType);

        Map<String, Object> data = new HashMap<>();
        data.put("versionId", version.getId());
        data.put("versionNo", version.getVersionNo());
        return R.ok(data);
    }

    /* ==================== 列表 / 详情 / 编辑 / 删除 ==================== */

    @GetMapping("/documents")
    public R<PageResult<Map<String, Object>>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) String status) {
        SecurityUtils.checkPerm("doc:list");
        return R.ok(PageResult.of(
                CamelUtil.camel(documentMapper.selectDocPage(keyword, categoryId, status, (page - 1) * size, size)),
                documentMapper.countDocPage(keyword, categoryId, status), page, size));
    }

    @GetMapping("/documents/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest req) {
        BizDocument doc = requireDoc(id);
        Map<String, Object> data = toMap(doc);
        data.put("versions", CamelUtil.camel(versionMapper.selectVersionsByDocId(id)));
        long userId = SecurityUtils.userId();
        data.put("favorited", socialMapper.isFavorited(userId, id));
        data.put("subscribed", false);
        data.put("subscriptionId", null);
        for (Map<String, Object> s : socialMapper.selectSubscriptions(userId)) {
            if ("DOC".equals(s.get("sub_type")) && id.equals(((Number) s.get("target_id")).longValue())) {
                data.put("subscribed", true);
                data.put("subscriptionId", s.get("id"));
            }
        }
        documentMapper.increaseViewCount(id);
        socialMapper.insertAccessLog(userId, id, "VIEW", null, req.getRemoteAddr());
        return R.ok(data);
    }

    @PutMapping("/documents/{id}")
    @OpLog(module = "文档管理", operation = "编辑文档")
    public R<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("doc:edit");
        requireDoc(id);
        var wrapper = new LambdaUpdateWrapper<BizDocument>().eq(BizDocument::getId, id);
        if (body.containsKey("title")) wrapper.set(BizDocument::getTitle, body.get("title"));
        if (body.containsKey("categoryId")) wrapper.set(BizDocument::getCategoryId, ((Number) body.get("categoryId")).longValue());
        if (body.containsKey("effectiveDate")) wrapper.set(BizDocument::getEffectiveDate, LocalDate.parse((String) body.get("effectiveDate")));
        documentMapper.update(null, wrapper);
        if (body.containsKey("categoryId")) {
            categoryMapper.refreshDocCount(((Number) body.get("categoryId")).longValue());
        }
        return R.ok();
    }

    @DeleteMapping("/documents/{id}")
    @OpLog(module = "文档管理", operation = "删除文档")
    @Transactional
    public R<Void> delete(@PathVariable Long id) {
        SecurityUtils.checkPerm("doc:delete");
        BizDocument doc = requireDoc(id);
        documentMapper.deleteById(id);
        chunkMapper.deactivateOtherVersions(id, doc.getCurrentVersionId() == null ? -1L : doc.getCurrentVersionId());
        chromaDeleteQuietly(id);
        if (doc.getCategoryId() != null) categoryMapper.refreshDocCount(doc.getCategoryId());
        return R.ok();
    }

    /* ==================== 审核流 ==================== */

    @PostMapping("/documents/{id}/submit")
    @OpLog(module = "文档管理", operation = "提交审核")
    public R<Void> submit(@PathVariable Long id) {
        SecurityUtils.checkPerm("doc:edit");
        BizDocument doc = requireDoc(id);
        if (!List.of("DRAFT", "REJECTED").contains(doc.getStatus())) {
            throw new BizException("当前状态不允许提交审核");
        }
        documentMapper.update(null, new LambdaUpdateWrapper<BizDocument>()
                .eq(BizDocument::getId, id).set(BizDocument::getStatus, "PENDING_AUDIT"));
        return R.ok();
    }

    @PostMapping("/documents/{id}/audit")
    @OpLog(module = "审核中心", operation = "文档审核")
    @Transactional
    public R<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SecurityUtils.checkPerm("doc:audit");
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String remark = String.valueOf(body.getOrDefault("remark", ""));
        BizDocument doc = requireDoc(id);
        if (!"PENDING_AUDIT".equals(doc.getStatus())) {
            throw new BizException("文档不处于待审核状态");
        }
        if (!pass && remark.isBlank()) throw new BizException("驳回时必须填写审核意见");

        long auditorId = SecurityUtils.userId();
        if (pass) {
            publishLatestVersion(doc, auditorId);
            documentMapper.update(null, new LambdaUpdateWrapper<BizDocument>()
                    .eq(BizDocument::getId, id)
                    .set(BizDocument::getStatus, "PUBLISHED")
                    .set(BizDocument::getAuditedBy, auditorId)
                    .set(BizDocument::getAuditedAt, OffsetDateTime.now())
                    .set(BizDocument::getAuditRemark, remark));
        } else {
            documentMapper.update(null, new LambdaUpdateWrapper<BizDocument>()
                    .eq(BizDocument::getId, id)
                    .set(BizDocument::getStatus, "REJECTED")
                    .set(BizDocument::getAuditedBy, auditorId)
                    .set(BizDocument::getAuditedAt, OffsetDateTime.now())
                    .set(BizDocument::getAuditRemark, remark));
        }
        return R.ok();
    }

    @PostMapping("/documents/{id}/offline")
    @OpLog(module = "审核中心", operation = "文档下线")
    @Transactional
    public R<Void> offline(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        SecurityUtils.checkPerm("doc:audit");
        BizDocument doc = requireDoc(id);
        if (!"PUBLISHED".equals(doc.getStatus())) throw new BizException("仅已发布文档可下线");
        String reason = body == null ? "管理员下线" : String.valueOf(body.getOrDefault("reason", "管理员下线"));
        documentMapper.update(null, new LambdaUpdateWrapper<BizDocument>()
                .eq(BizDocument::getId, id).set(BizDocument::getStatus, "OFFLINE")
                .set(BizDocument::getAuditRemark, reason));
        chunkMapper.deactivateOtherVersions(id, -1L);
        chromaDeleteQuietly(id);
        return R.ok();
    }

    @PutMapping("/versions/{versionId}/publish")
    @OpLog(module = "审核中心", operation = "版本发布")
    @Transactional
    public R<Void> publishVersion(@PathVariable Long versionId) {
        SecurityUtils.checkPerm("doc:audit");
        BizDocVersion version = versionMapper.selectById(versionId);
        if (version == null) throw new BizException("版本不存在");
        if (!"READY".equals(version.getIndexStatus())) throw new BizException("版本索引未就绪，无法发布");
        BizDocument doc = requireDoc(version.getDocId());
        publishLatestVersion(doc, SecurityUtils.userId());
        chromaResync(doc.getId());
        documentMapper.update(null, new LambdaUpdateWrapper<BizDocument>()
                .eq(BizDocument::getId, doc.getId())
                .set(BizDocument::getStatus, "PUBLISHED"));
        return R.ok();
    }

    /* ==================== 版本 diff ==================== */

    @GetMapping("/documents/{id}/diff")
    public R<Map<String, Object>> diff(@PathVariable Long id,
                                       @RequestParam Long fromVersionId,
                                       @RequestParam Long toVersionId) {
        requireDoc(id);
        String fromText = loadPlainText(fromVersionId);
        String toText = loadPlainText(toVersionId);
        List<String> fromLines = Arrays.asList(fromText.split("\\n"));
        List<String> toLines = Arrays.asList(toText.split("\\n"));

        List<Map<String, Object>> segments = new ArrayList<>();
        Patch<String> patch = DiffUtils.diff(fromLines, toLines);
        int cursor = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int sourcePos = delta.getSource().getPosition();
            if (sourcePos > cursor) {
                segments.add(seg("UNCHANGED", String.join("\n", fromLines.subList(cursor, sourcePos)), null));
            }
            switch (delta.getType()) {
                case INSERT -> segments.add(seg("ADDED", null, String.join("\n", delta.getTarget().getLines())));
                case DELETE -> segments.add(seg("REMOVED", String.join("\n", delta.getSource().getLines()), null));
                case CHANGE -> segments.add(seg("CHANGED",
                        String.join("\n", delta.getSource().getLines()),
                        String.join("\n", delta.getTarget().getLines())));
            }
            cursor = sourcePos + delta.getSource().size();
        }
        if (cursor < fromLines.size()) {
            segments.add(seg("UNCHANGED", String.join("\n", fromLines.subList(cursor, fromLines.size())), null));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("fromTitle", versionTitle(fromVersionId));
        data.put("toTitle", versionTitle(toVersionId));
        data.put("segments", segments);
        return R.ok(data);
    }

    /* ==================== 收藏 / 订阅 / 通知 ==================== */

    @PostMapping("/documents/{id}/favorite")
    public R<Void> favorite(@PathVariable Long id) {
        requireDoc(id);
        long uid = SecurityUtils.userId();
        if (socialMapper.favorite(uid, id) > 0) {
            documentMapper.adjustFavoriteCount(id, 1);
        }
        return R.ok();
    }

    @DeleteMapping("/documents/{id}/favorite")
    public R<Void> unfavorite(@PathVariable Long id) {
        requireDoc(id);
        long uid = SecurityUtils.userId();
        if (socialMapper.unfavorite(uid, id) > 0) {
            documentMapper.adjustFavoriteCount(id, -1);
        }
        return R.ok();
    }

    @GetMapping("/favorites")
    public R<PageResult<Map<String, Object>>> favorites(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        long uid = SecurityUtils.userId();
        return R.ok(PageResult.of(
                CamelUtil.camel(socialMapper.selectFavorites(uid, (page - 1) * size, size)),
                socialMapper.countFavorites(uid), page, size));
    }

    @PostMapping("/subscriptions")
    public R<Void> subscribe(@RequestBody Map<String, Object> body) {
        String subType = String.valueOf(body.get("subType"));
        if (!List.of("CATEGORY", "DOC").contains(subType)) throw new BizException("订阅类型不合法");
        Long targetId = ((Number) body.get("targetId")).longValue();
        socialMapper.subscribe(SecurityUtils.userId(), subType, targetId);
        return R.ok();
    }

    @DeleteMapping("/subscriptions/{id}")
    public R<Void> unsubscribe(@PathVariable Long id) {
        socialMapper.unsubscribe(id, SecurityUtils.userId());
        return R.ok();
    }

    @GetMapping("/subscriptions")
    public R<List<Map<String, Object>>> subscriptions() {
        return R.ok(CamelUtil.camel(socialMapper.selectSubscriptions(SecurityUtils.userId())));
    }

    @GetMapping("/notifies")
    public R<PageResult<Map<String, Object>>> notifies(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        long uid = SecurityUtils.userId();
        return R.ok(PageResult.of(
                CamelUtil.camel(socialMapper.selectNotifies(uid, (page - 1) * size, size)),
                socialMapper.countNotifies(uid), page, size));
    }

    @PutMapping("/notifies/{id}/read")
    public R<Void> readNotify(@PathVariable Long id) {
        socialMapper.markNotifyRead(id, SecurityUtils.userId());
        return R.ok();
    }

    /* ==================== 重建索引 ==================== */

    @PostMapping("/documents/{id}/reindex")
    @OpLog(module = "文档管理", operation = "重建索引")
    public R<Void> reindex(@PathVariable Long id) {
        SecurityUtils.checkPerm("doc:edit");
        BizDocument doc = requireDoc(id);
        BizDocVersion version = versionMapper.selectList(new LambdaQueryWrapper<BizDocVersion>()
                .eq(BizDocVersion::getDocId, id).orderByDesc(BizDocVersion::getId).last("LIMIT 1"))
                .stream().findFirst().orElseThrow(() -> new BizException("文档暂无版本"));
        versionMapper.casIndexStatus(version.getId(), version.getIndexStatus(), "PENDING", null, 0);
        version.setIndexStatus("PENDING");
        ingestPipeline.ingestAsync(version, doc.getSourceType());
        return R.ok();
    }

    /* ==================== 私有 ==================== */

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("请选择文件");
        if (file.getSize() > MAX_SIZE) throw new BizException("文件大小超出限制（50MB）");
        String ext = extOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) throw new BizException("仅支持 TXT / Word / PDF 格式");
    }

    private String extOf(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    private String normalizeType(String ext) {
        return "DOC".equals(ext.toUpperCase()) ? "DOCX" : ext.toUpperCase();
    }

    private String stripExt(String name) {
        if (name == null) return "未命名文档";
        return name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
    }

    private BizDocVersion insertVersion(BizDocument doc, String filePath, MultipartFile file, String changeLog) {
        BizDocVersion version = new BizDocVersion();
        version.setDocId(doc.getId());
        version.setVersionNo(versionMapper.nextVersionNo(doc.getId()));
        version.setTitleSnapshot(doc.getTitle());
        version.setFilePath(filePath);
        version.setFileHash(sha256(file));
        version.setIndexStatus("PENDING");
        version.setChangeLog(changeLog);
        version.setCreatedBy(SecurityUtils.userId());
        versionMapper.insert(version);
        return version;
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private BizDocument requireDoc(Long id) {
        BizDocument doc = documentMapper.selectById(id);
        if (doc == null) throw new BizException("文档不存在");
        return doc;
    }

    private Map<String, Object> toMap(BizDocument doc) {
        Map<String, Object> m = new HashMap<>();
        m.put("docId", doc.getId());
        m.put("docCode", doc.getDocCode());
        m.put("title", doc.getTitle());
        m.put("categoryId", doc.getCategoryId());
        m.put("status", doc.getStatus());
        m.put("summary", doc.getSummary());
        m.put("sourceType", doc.getSourceType());
        m.put("viewCount", doc.getViewCount());
        m.put("quoteCount", doc.getQuoteCount());
        m.put("favoriteCount", doc.getFavoriteCount());
        m.put("effectiveDate", doc.getEffectiveDate());
        m.put("updatedAt", doc.getUpdatedAt());
        return m;
    }

    private void chromaDeleteQuietly(long docId) {
        if (!chroma.isAvailable()) return;
        try {
            chroma.deleteByDocId(docId);
        } catch (Exception ignore) { }
    }

    /** 发布后按当前版本回灌 Chroma：从 MySQL 切片（含 embedding TEXT）重建该文档全部向量 */
    private void chromaResync(long docId) {
        if (!chroma.isAvailable()) return;
        try {
            List<BizDocument> docs = documentMapper.selectList(
                    new LambdaQueryWrapper<BizDocument>().eq(BizDocument::getId, docId));
            if (docs.isEmpty()) return;
            Long versionId = docs.get(0).getCurrentVersionId();
            if (versionId == null) return;
            List<com.docqa.document.entity.BizDocChunk> chunks = chunkMapper.selectList(
                    new LambdaQueryWrapper<com.docqa.document.entity.BizDocChunk>()
                            .eq(com.docqa.document.entity.BizDocChunk::getVersionId, versionId)
                            .eq(com.docqa.document.entity.BizDocChunk::getIsActive, true));
            if (chunks.isEmpty()) return;
            List<String> ids = new ArrayList<>();
            List<List<Double>> vectors = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<Map<String, Object>> metas = new ArrayList<>();
            for (var c : chunks) {
                if (c.getEmbedding() == null || c.getEmbedding().isEmpty()) continue;
                ids.add(String.valueOf(c.getId()));
                vectors.add(c.getEmbedding());
                contents.add(c.getContent());
                Map<String, Object> m = new HashMap<>();
                m.put("doc_id", c.getDocId());
                m.put("version_id", c.getVersionId());
                m.put("chunk_index", c.getChunkIndex());
                m.put("heading_path", c.getHeadingPath() == null ? "" : c.getHeadingPath());
                metas.add(m);
            }
            if (!ids.isEmpty()) {
                chroma.upsertBatch(ids, vectors, contents, metas);
            }
        } catch (Exception ignore) { }
    }

    /** 版本发布核心事务 T4：置当前版本 + 旧切片置灰 + 订阅通知 + 冗余摘要 */
    private void publishLatestVersion(BizDocument doc, long auditorId) {
        BizDocVersion latest = versionMapper.selectList(new LambdaQueryWrapper<BizDocVersion>()
                        .eq(BizDocVersion::getDocId, doc.getId())
                        .eq(BizDocVersion::getIndexStatus, "READY")
                        .orderByDesc(BizDocVersion::getId).last("LIMIT 1"))
                .stream().findFirst().orElseThrow(() -> new BizException("无可发布的就绪版本（索引未完成）"));
        documentMapper.update(null, new LambdaUpdateWrapper<BizDocument>()
                .eq(BizDocument::getId, doc.getId())
                .set(BizDocument::getCurrentVersionId, latest.getId())
                .set(BizDocument::getSummary, latest.getSummary()));
        chunkMapper.deactivateOtherVersions(doc.getId(), latest.getId());
        chromaDeleteQuietly(doc.getId());
        versionMapper.update(null, new LambdaUpdateWrapper<BizDocVersion>()
                .eq(BizDocVersion::getId, latest.getId())
                .set(BizDocVersion::getPublishedAt, OffsetDateTime.now()));
        notifySubscribers(doc, latest);
    }

    private void notifySubscribers(BizDocument doc, BizDocVersion version) {
        String msg = "《" + doc.getTitle() + "》已发布新版本 " + version.getVersionNo();
        for (Long uid : socialMapper.selectSubscriberIds("DOC", doc.getId())) {
            socialMapper.insertNotify(uid, "文档更新通知", msg, doc.getId());
        }
        if (doc.getCategoryId() != null) {
            for (Long uid : socialMapper.selectSubscriberIds("CATEGORY", doc.getCategoryId())) {
                socialMapper.insertNotify(uid, "分类更新通知", "您订阅的分类下 " + msg, doc.getId());
            }
        }
    }

    private String loadPlainText(Long versionId) {
        BizDocVersion v = versionMapper.selectById(versionId);
        if (v == null) throw new BizException("版本不存在");
        if (v.getPlainTextPath() == null) return "";
        return storageClient.readText(v.getPlainTextPath());
    }

    private String versionTitle(Long versionId) {
        BizDocVersion v = versionMapper.selectById(versionId);
        return v == null ? "" : v.getVersionNo() + " " + v.getTitleSnapshot();
    }

    private Map<String, Object> seg(String type, String from, String to) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("fromText", from);
        m.put("toText", to);
        return m;
    }

}
