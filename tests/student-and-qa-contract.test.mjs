import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('student identity registration waits for the server before returning', () => {
  const source = read('src/pages/common/edit-profile.vue')
  assert.match(source, /await studentApi\.studentRegister\(/)
  // 手机号校验：统一正则（旧断言 phone.length !== 11 已升级为正则校验）
  assert.match(source, /phone && !\/\^1\\d\{10\}\$\/\.test\(phone\)/)
  assert.match(source, /!form\.highSchoolId/)
})

test('student home blocks the flow when the backend reports no student identity', () => {
  const source = read('src/pages/student/home.vue')
  // 身份拦截：hasBoundIdentity + ensureBoundIdentity 引导完善信息
  assert.match(source, /hasBoundIdentity/)
  assert.match(source, /请先完善信息/)
  assert.match(source, /if \(!userStore\.hasBoundIdentity\)/)
})

test('login failures do not create a fake identity', () => {
  const source = read('src/pages/common/login.vue')
  assert.doesNotMatch(source, /token:\s*'wx_token_'/)
})

test('switching a QA category reloads the filtered server result', () => {
  const source = read('src/pages/volunteer/qa-center.vue')
  assert.match(source, /await qaApi\.getForumPosts\(params\)/)
  assert.match(source, /params\.category = category/)
  assert.match(source, /async function switchTag\(tag\)[\s\S]*await loadPosts\(tag\)/)
})
