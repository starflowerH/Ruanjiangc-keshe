import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('teacher home no longer exposes port preview', () => {
  const source = read('src/pages/teacher/home.vue')
  assert.doesNotMatch(source, /端口预览/)
  assert.doesNotMatch(source, /switchToStudent/)
  assert.doesNotMatch(source, /switchToVolunteer/)
  assert.doesNotMatch(source, /预览高中生端/)
  assert.doesNotMatch(source, /预览大学生端/)
})
