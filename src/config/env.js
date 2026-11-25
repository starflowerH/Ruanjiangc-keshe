/**
 * 运行环境配置
 *
 * 注意：不要在这里使用 import.meta.env / process.env 等构建环境变量——
 * vite 会为 import.meta 注入 require("url") shim，微信小程序运行时没有
 * node 的 url 模块，模块加载会直接崩溃（2026-08-12 线上事故根因）。
 *
 * 切换环境（开发/测试/生产）只需修改下面的 BASE_URL。
 */
export const BASE_URL = 'https://hynu.site'
