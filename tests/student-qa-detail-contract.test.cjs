const assert = require('assert')
const fs = require('fs')

// 页面已重构为 StudentForumPrototype 包装器（与其他学生端页面一致），
// 详情结构断言落在真实承载组件上。
const page = fs.readFileSync('src/pages/student/qa-forum.vue', 'utf8')
const source = fs.readFileSync('src/components/StudentForumPrototype.vue', 'utf8')

assert.match(page, /<StudentForumPrototype \/>/, '论坛页必须是 StudentForumPrototype 包装器')
assert.doesNotMatch(page, /<<<<<<<|=======|>>>>>>>/, '页面不得残留 git 冲突标记')
assert.match(source, /class="discussion-page"/, '高中生端应在原论坛页内展示大学生端同款讨论详情')
assert.match(source, /commentApi\.getComments\(/, '讨论详情必须加载真实评论')
assert.match(source, /commentApi\.addComment\(\{[\s\S]*parentId/, '评论回复必须带 parentId')
assert.match(source, /@longpress="openCommentReport\(comment\)"/, '评论举报必须由长按触发')
assert.match(source, /<TabBar v-if="!detailPost"/, '详情页打开时不得渲染列表页底部导航')
assert.doesNotMatch(source, /uni\.navigateTo\(\{ url: `\/pages\/common\/qa-detail/, '帖子详情不应再跳转到旧详情页')

console.log('Student QA detail contract passed')
