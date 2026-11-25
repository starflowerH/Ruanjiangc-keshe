import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

test('approved or pending volunteer applications do not reopen registration', () => {
  const register = read('src/pages/volunteer/register.vue')
  const home = read('src/pages/volunteer/home.vue')
  const profile = read('src/pages/volunteer/profile.vue')

  assert.match(register, /async function guardExistingApply\(\)/)
  assert.match(register, /!\['APPROVED', 'PENDING'\]\.includes\(latestApply\.status\)/)
  assert.match(register, /无需重复填写/)
  assert.match(register, /无需重复提交/)
  assert.match(home, /v-if="!\['APPROVED', 'PENDING'\]\.includes\(userStore\.userInfo\?\.status\)"/)
  assert.match(profile, /v-if="!\['APPROVED', 'PENDING'\]\.includes\(userStore\.userInfo\?\.status\)"/)
})
