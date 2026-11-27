import request from '../utils/request'

/**
 * 上传文档到知识库（系统自动分类）
 * @param {File} file 文件对象
 * @param {AbortSignal} signal 取消信号
 */
export function uploadDocToKnowledge(file, signal) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/spec-qa/upload',
    method: 'post',
    data: formData,
    timeout: 300000, // 5分钟超时，适配OCR长任务
    signal: signal,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
