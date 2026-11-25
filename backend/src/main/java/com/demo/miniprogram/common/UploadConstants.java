package com.demo.miniprogram.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传文件统一配置 - 固定存储目录，避免随进程 cwd / 重启 / 部署目录变化而丢失。
 *
 * 默认存到后端仓库下固定的 uploads 目录；可通过环境变量 UPLOAD_DIR 覆盖。
 * UploadController 负责写文件，SecurityConfig 负责 /uploads/** 静态映射，两者必须用同一路径。
 */
public final class UploadConstants {

    private UploadConstants() {
    }

    private static final String DEFAULT_UPLOAD_DIR = "/home/xiaoyuyu/work/zhaosheng/backend/uploads";

    /** 上传文件根目录（绝对路径）。默认固定为后端仓库 uploads 目录，可由环境变量 UPLOAD_DIR 覆盖。 */
    public static final Path UPLOAD_DIR = Paths.get(System.getenv().getOrDefault(
            "UPLOAD_DIR",
            DEFAULT_UPLOAD_DIR));

    /** 对外暴露的 URL 前缀（相对路径，前端拼 BASE_URL 后访问） */
    public static final String URL_PREFIX = "/uploads/";
}