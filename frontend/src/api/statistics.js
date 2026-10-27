import request from './request'

export const getSummary = () => request.get('/statistics/summary')
export const getByDept = () => request.get('/statistics/by-dept')
export const getByType = () => request.get('/statistics/by-type')
export const getByLevel = () => request.get('/statistics/by-level')
export const getTrend = (months = 6) => request.get('/statistics/trend', { params: { months } })
