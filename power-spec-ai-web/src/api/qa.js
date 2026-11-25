import request from '../utils/request'

// 电力规范问答接口（自动分类，无需手动指定类型）
// 分类 + 检索 + 生成全链路耗时可能超过 30s，单独放宽到 2 分钟
export function askSpecQuestion(data) {
  return request({
    url: '/spec-qa/ask',
    method: 'post',
    data,
    timeout: 120000
  })
}
