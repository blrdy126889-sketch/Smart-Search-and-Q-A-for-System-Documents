package com.docqa.framework.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储：按年月分目录（可替换 MinIO/OSS）
 */
public class StorageClient {

    private final Path baseDir;

    public StorageClient(String baseDir) {
        this.baseDir = Path.of(baseDir);
        try {
            Files.createDirectories(this.baseDir);
        } catch (Exception e) {
            throw new IllegalStateException("存储目录初始化失败: " + baseDir, e);
        }
    }

    public String save(InputStream in, String originalFilename) {
        try {
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path dir = baseDir.resolve(month);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return month + "/" + filename;
        } catch (Exception e) {
            throw new IllegalStateException("文件保存失败: " + e.getMessage(), e);
        }
    }

    public String saveText(String content) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String filename = UUID.randomUUID().toString().replace("-", "") + ".txt";
        try {
            Path dir = baseDir.resolve(month);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(filename), content == null ? "" : content);
            return month + "/" + filename;
        } catch (Exception e) {
            throw new IllegalStateException("文本保存失败: " + e.getMessage(), e);
        }
    }

    public InputStream read(String relativePath) {
        try {
            return Files.newInputStream(baseDir.resolve(relativePath));
        } catch (Exception e) {
            throw new IllegalStateException("文件读取失败: " + relativePath, e);
        }
    }

    public String readText(String relativePath) {
        try {
            return Files.readString(baseDir.resolve(relativePath));
        } catch (Exception e) {
            return "";
        }
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(baseDir.resolve(relativePath));
        } catch (Exception ignore) { }
    }
}
