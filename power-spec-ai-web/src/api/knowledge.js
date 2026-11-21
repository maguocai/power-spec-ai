import request from '../utils/request'

/**
 * 上传文档到知识库（系统自动分类）
 * @param {File} file 文件对象
 */
export function uploadDocToKnowledge(file) {
    const formData = new FormData()
    formData.append('file', file)

    return request({
        url: '/spec-qa/upload',
        method: 'post',
        data: formData
    })
}
