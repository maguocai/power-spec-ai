import request from '../utils/request'

/**
 * 上传文档到知识库（系统自动分类）
 * 扫描件 OCR + 向量化耗时较长，单独放宽超时时间至 5 分钟，
 * 不使用 request.js 全局的 30s 超时。
 *
 * @param {File} file 文件对象
 * @param {AbortSignal} [signal] 可选，用于中途取消请求
 * @returns {Promise<string>} 后端返回的提示消息
 */
export function uploadDocToKnowledge(file, signal) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/spec-qa/upload',
    method: 'post',
    data: formData,
    timeout: 300000, // 5 分钟，覆盖全局 30s
    signal
  })
}
