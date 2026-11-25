const assert = require('assert')
const fs = require('fs')

const source = fs.readFileSync('src/api/index.js', 'utf8')

assert.match(
  source,
  /const errorMessage = err\?\.errMsg \|\| '网络连接失败'/,
  '网络失败回调必须兼容微信开发者工具传入 undefined 的情况'
)
assert.doesNotMatch(
  source,
  /message: err\.errMsg|new Error\(err\.errMsg/,
  '网络失败回调不能直接读取 err.errMsg'
)

const reportSource = fs.readFileSync('src/utils/report.js', 'utf8')
assert.match(
  reportSource,
  /platform === 'devtools'/,
  '微信开发者工具不应注册可能触发 SDK 兼容问题的全局错误监听'
)

console.log('Request failure contract passed')
