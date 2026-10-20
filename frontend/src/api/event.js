import request from './request'

export const reportEvent = (data) => request.post('/events', data)
export const listEvents = (params) => request.get('/events', { params })
export const getEvent = (id) => request.get('/events/' + id)
