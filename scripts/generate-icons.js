/**
 * TabBar 图标生成脚本
 * 运行: node scripts/generate-icons.js
 *
 * 此脚本会生成 TabBar 所需的占位图标
 * 实际项目中请替换为设计稿图标
 */

const fs = require('fs')
const path = require('path')

// 简单的 SVG 图标模板
const icons = {
  'tab-home': {
    normal: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 10L8 35v36h24V51h17v20h24V35L40.5 10z" fill="#999"/>
    </svg>`,
    active: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 10L8 35v36h24V51h17v20h24V35L40.5 10z" fill="#4A90D9"/>
    </svg>`
  },
  'tab-university': {
    normal: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 8L6 24v8h72v-8L40.5 8zM12 36v24h8V36H12zm16 0v24h8V36H28zm16 0v24h8V36h-8zm16 0v24h8V36h-8zM6 64v8h72v-8H6z" fill="#999"/>
    </svg>`,
    active: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 8L6 24v8h72v-8L40.5 8zM12 36v24h8V36H12zm16 0v24h8V36H28zm16 0v24h8V36h-8zm16 0v24h8V36h-8zM6 64v8h72v-8H6z" fill="#4A90D9"/>
    </svg>`
  },
  'tab-qa': {
    normal: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 8C21.4 8 6 21.4 6 38c0 9.4 4.6 17.8 11.8 23.2L12 72l14.6-7.6C30.4 65.8 35.2 67 40.5 67c19.1 0 34.5-13.4 34.5-29S59.6 8 40.5 8zm0 48c-3.2 0-6-2.8-6-6s2.8-6 6-6 6 2.8 6 6-2.8 6-6 6zm6-16H34.5v-4h12v-8h-8v-4h8v-8h4v8h8v4h-8v8z" fill="#999"/>
    </svg>`,
    active: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 8C21.4 8 6 21.4 6 38c0 9.4 4.6 17.8 11.8 23.2L12 72l14.6-7.6C30.4 65.8 35.2 67 40.5 67c19.1 0 34.5-13.4 34.5-29S59.6 8 40.5 8zm-4 36v4h8v-4h-8zm0-8v4h8v-4h-8zm0-8v4h8v-4h-8z" fill="#4A90D9"/>
    </svg>`
  },
  'tab-profile': {
    normal: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 8c-8.8 0-16 7.2-16 16s7.2 16 16 16 16-7.2 16-16-7.2-16-16-16zm0 48c-17.6 0-32 8-32 16v8h64v-8c0-8-14.4-16-32-16z" fill="#999"/>
    </svg>`,
    active: `<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">
      <path d="M40.5 8c-8.8 0-16 7.2-16 16s7.2 16 16 16 16-7.2 16-16-7.2-16-16-16zm0 48c-17.6 0-32 8-32 16v8h64v-8c0-8-14.4-16-32-16z" fill="#4A90D9"/>
    </svg>`
  }
}

const outputDir = path.join(__dirname, '..', 'src', 'static', 'images')

// 确保目录存在
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true })
}

// 生成 SVG 文件（可转换为 PNG）
Object.entries(icons).forEach(([name, svg]) => {
  fs.writeFileSync(path.join(outputDir, `${name}.svg`), svg.normal)
  fs.writeFileSync(path.join(outputDir, `${name}-active.svg`), svg.active)
  console.log(`Generated: ${name}.svg and ${name}-active.svg`)
})

console.log('\n图标已生成为 SVG 格式')
console.log('请使用工具将 SVG 转换为 PNG 格式，或直接使用设计稿提供的图标')
console.log(`输出目录: ${outputDir}`)
