import request from '../utils/request'

// 电力规范问答接口（自动分类，无需手动指定类型）
export function askSpecQuestion(data) {
    return request({
        url: '/spec-qa/ask',
        method: 'post',
        data
    })
}

