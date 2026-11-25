import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('formatIdentityName appends -高中生 for students and -大学生 otherwise', () => {
  const source = read('src/utils/format.js')
  assert.match(source, /export function formatIdentityName\(name, type\)/)
  assert.match(source, /type === 'student' \? '-高中生' : '-大学生'/)
})

test('forum post author names use identity label', () => {
  const forum = read('src/pages/student/qa-forum.vue')
  assert.match(forum, /formatIdentityName\(item\.askerName/)
  const center = read('src/pages/volunteer/qa-center.vue')
  assert.match(center, /formatIdentityName\(post\.askerName, post\.authorType\)/)
})

test('volunteer qa-center supports authorType filter', () => {
  const source = read('src/pages/volunteer/qa-center.vue')
  assert.match(source, /authorTypes/)
  assert.match(source, /activeAuthorType/)
  assert.match(source, /if \(authorType !== 'all'\) params\.authorType = authorType/)
})

test('qa detail labels post/answer/comment authors', () => {
  const source = read('src/pages/common/qa-detail.vue')
  assert.match(source, /formatIdentityName\(post\.authorName, post\.authorRole\)/)
  assert.match(source, /formatIdentityName\(answer\.authorName, 'volunteer'\)/)
  assert.match(source, /formatIdentityName\(comment\.userName, comment\.userType\)/)
})
