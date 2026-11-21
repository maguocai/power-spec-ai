import axios from 'axios'

const request = axios.create({
    baseURL: '/api',
    timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(config => {
    // 如果数据是 FormData，删除 Content-Type，让 axios 自动处理
    if (config.data instanceof FormData) {
        delete config.headers['Content-Type']
    } else {
        // 普通请求才设为 JSON
        config.headers['Content-Type'] = 'application/json'
    }
    return config
})

// 响应拦截器
request.interceptors.response.use(
    response => response.data,
    error => {
        console.error('请求失败：', error)
        return Promise.reject(error)
    }
)

export default request
