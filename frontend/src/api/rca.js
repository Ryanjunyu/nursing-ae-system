import request from './request'

// 五维诱因
export const getFactors = (eventId) => request.get(`/rca/factors/${eventId}`)
export const saveFactors = (data) => request.post('/rca/factors', data)
// 根因分析记录
export const getRca = (eventId) => request.get(`/rca/${eventId}`)
export const saveRca = (data) => request.post('/rca', data)
// 辅助分析（完成度 + 五维建议）
export const getAssist = (eventId) => request.get(`/rca/assist/${eventId}`)
