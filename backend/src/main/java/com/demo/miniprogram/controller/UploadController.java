package com.demo.miniprogram.controller;

import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.common.UploadConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口 - 支持头像等图片上传
 *
 * 存储方案：文件保存到 {user.dir}/uploads/ 目录（运行时自动创建），
 * 由 SecurityConfig.addResourceHandlers 将 /uploads/** 映射到该目录，
 * JwtAuthFilter 白名单放行 /uploads/（小程序 <image> 不带 token）。
 *
 * @author demo
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    /** 允许的图片扩展名（小写，不含点） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 上传大小上限 2MB，与 application.yml 的 spring.servlet.multipart.max-file-size 一致，代码再兜底校验一次 */
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024L;

    /**
     * 上传图片
     * POST /api/upload/image (multipart/form-data, 参数名 file)
     * 返回相对路径 "/uploads/xxx.jpg"，前端拼接 BASE_URL 后可直接用于 <image>
     */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过2MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp 格式的图片");
        }
        // 魔数校验：仅凭扩展名可被伪装文件绕过（如把可执行文件改名为 .jpg 上传）
        byte[] head = new byte[12];
        int read = file.getInputStream().read(head);
        if (read < 12 || !isValidImageMagic(head)) {
            throw new BusinessException("文件内容不是有效的图片");
        }

        Path uploadDir = UploadConstants.UPLOAD_DIR;
        Files.createDirectories(uploadDir);

        // 文件名用 UUID 生成，杜绝路径穿越/覆盖攻击
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Files.copy(file.getInputStream(), uploadDir.resolve(fileName));

        log.info("图片上传成功: {}, size={}B", fileName, file.getSize());
        return Result.ok("上传成功", UploadConstants.URL_PREFIX + fileName);
    }

    /** 校验图片魔数（JPEG/PNG/GIF/WEBP） */
    private boolean isValidImageMagic(byte[] h) {
        // JPEG: FF D8 FF
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return true;
        // GIF: 47 49 46 38
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return true;
        // WEBP: RIFF xxxx WEBP
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return true;
        return false;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
