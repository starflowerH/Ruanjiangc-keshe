#!/usr/bin/env node
/**
 * 微信小程序代码上传脚本（miniprogram-ci）
 *
 * 用法:
 *   node scripts/upload-mp.js [密钥路径] [版本号] [描述] [robot编号]
 *   node scripts/upload-mp.js                      # 默认读 ./private.wxa1ddf4f487f71ff8.key, 版本 1.0.0, robot=1
 *   node scripts/upload-mp.js ~/keys/my.key 1.1.0 "修复问答显示" 2
 *
 * robot 编号(1-30): 在微信公众平台「开发→开发管理→开发设置→小程序代码上传→机器人」中
 * 可给每个机器人设置自定义名称(上传记录会显示该名称, 如"小鱼")
 *
 * 前置:
 *   1. 微信公众平台 → 开发 → 开发管理 → 开发设置 → 小程序代码上传 → 生成密钥
 *      （密钥只下载一次；下载时 IP 白名单可留空）
 *   2. 密钥文件放到项目根目录 private.wxa1ddf4f487f71ff8.key
 *   3. 先执行 npm run build:mp-weixin 生成 dist/build/mp-weixin
 */
const ci = require('miniprogram-ci')
const path = require('path')
const fs = require('fs')

const APPID = 'wxa1ddf4f487f71ff8'
const PROJECT_PATH = path.join(__dirname, '..', 'dist', 'build', 'mp-weixin')
const DEFAULT_KEY = path.join(__dirname, '..', 'private.wxa1ddf4f487f71ff8.key')

const keyPath = process.argv[2] || DEFAULT_KEY
const version = process.argv[3] || '1.0.0'
const desc = process.argv[4] || '自动构建上传'
const robot = parseInt(process.argv[5], 10) || 1

if (!fs.existsSync(PROJECT_PATH)) {
  console.error(`❌ 构建产物不存在: ${PROJECT_PATH}\n请先执行 npm run build:mp-weixin`)
  process.exit(1)
}
if (!fs.existsSync(keyPath)) {
  console.error(`❌ 上传密钥不存在: ${keyPath}\n请在微信公众平台下载代码上传密钥后放到该路径`)
  process.exit(1)
}

async function main() {
  const project = new ci.Project({
    appid: APPID,
    type: 'miniProgram',
    projectPath: PROJECT_PATH,
    privateKeyPath: keyPath,
    ignores: ['node_modules/**/*'],
  })

  console.log(`⬆️  开始上传: appid=${APPID} version=${version} robot=${robot} desc=${desc}`)
  const result = await ci.upload({
    project,
    version,
    desc,
    robot,
    setting: {
      es6: true,
      es7: true,
      minify: true,
      autoPrefixWXSS: true,
    },
    onProgressUpdate: (task) => {
      if (task._msg) console.log(`   [${task._id || ''}] ${task._msg}`)
    },
  })
  console.log('✅ 上传成功:', JSON.stringify(result || {}, null, 2))
}

main().catch((e) => {
  console.error('❌ 上传失败:', e.message || e)
  process.exit(1)
})
