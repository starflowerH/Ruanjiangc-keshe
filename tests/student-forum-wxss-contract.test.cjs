const assert = require('assert')
const fs = require('fs')

const source = fs.readFileSync('src/components/StudentForumPrototype.vue', 'utf8')

assert.match(source, /:class="roleClass\(post\.role\)"/, '身份展示必须映射为 ASCII 样式类名')
assert.doesNotMatch(source, /\.高中生\{|\.大学生\{|\.大学论坛\{/, 'WXSS 不能使用中文类名')
assert.match(source, /\.role-student\{|\.role-university\{|\.role-campus\{/, '身份样式必须使用 ASCII 类名')

console.log('Student forum WXSS contract passed')
