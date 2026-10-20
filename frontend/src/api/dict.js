import request from './request'

export const getDepartments = () => request.get('/dict/departments')
export const getEventTypes = () => request.get('/dict/event-types')
export const getInjuryLevels = () => request.get('/dict/injury-levels')
