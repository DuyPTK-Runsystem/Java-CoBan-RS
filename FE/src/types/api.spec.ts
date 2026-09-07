import { describe, expect, it } from 'vitest'

import {
  ApiError,
  extractApiError,
  extractApiErrorMessage,
  extractApiErrorMessages,
  isApiError,
  isGenericErrorMessage,
} from './api'

describe('api error utilities', () => {
  describe('isApiError', () => {
    it('correctly identifies ApiError and matching status', () => {
      const error = new ApiError(409, 'Conflict')
      expect(isApiError(error)).toBe(true)
      expect(isApiError(error, 409)).toBe(true)
      expect(isApiError(error, 400)).toBe(false)
      expect(isApiError(new Error('fail'))).toBe(false)
      expect(isApiError(null)).toBe(false)
    })
  })

  describe('isGenericErrorMessage', () => {
    it('returns true for undefined, null, empty or whitespace strings', () => {
      expect(isGenericErrorMessage(undefined)).toBe(true)
      expect(isGenericErrorMessage(null)).toBe(true)
      expect(isGenericErrorMessage('')).toBe(true)
      expect(isGenericErrorMessage('   ')).toBe(true)
    })

    it('returns true for generic HTTP phrases and fallback messages regardless of punctuation and casing', () => {
      expect(isGenericErrorMessage('Conflict')).toBe(true)
      expect(isGenericErrorMessage('conflict')).toBe(true)
      expect(isGenericErrorMessage('Conflict.')).toBe(true)
      expect(isGenericErrorMessage('409 Conflict')).toBe(true)
      expect(isGenericErrorMessage('409 - Conflict.')).toBe(true)
      expect(isGenericErrorMessage('409')).toBe(true)
      expect(isGenericErrorMessage('The request conflicts with existing data.')).toBe(true)
      expect(isGenericErrorMessage('The request conflicts with existing data')).toBe(true)
      expect(isGenericErrorMessage('Unable to reach the server.')).toBe(true)
      expect(isGenericErrorMessage('Unable to reach the server')).toBe(true)
      expect(isGenericErrorMessage('Forbidden')).toBe(true)
      expect(isGenericErrorMessage('403 Forbidden')).toBe(true)
      expect(isGenericErrorMessage('Not Found')).toBe(true)
      expect(isGenericErrorMessage('Bad Request')).toBe(true)
      expect(isGenericErrorMessage('Internal Server Error.')).toBe(true)
      expect(isGenericErrorMessage('500 Internal Server Error')).toBe(true)
      expect(isGenericErrorMessage('[object Object]')).toBe(true)
      expect(isGenericErrorMessage('undefined')).toBe(true)
      expect(isGenericErrorMessage('null')).toBe(true)
    })

    it('returns false for meaningful custom backend error messages', () => {
      expect(isGenericErrorMessage('Chưa có điểm tổng kết thường (regular_dtbmh_cn)...')).toBe(false)
      expect(isGenericErrorMessage('Record cùng student/year/subject đã tồn tại')).toBe(false)
      expect(isGenericErrorMessage('Điểm thi lại phải từ 0.0 đến 10.0')).toBe(false)
      expect(isGenericErrorMessage('Network disconnected')).toBe(false)
      expect(isGenericErrorMessage('Học sinh chưa có điểm thi môn này')).toBe(false)
    })
  })

  describe('extractApiErrorMessages', () => {
    it('extracts non-generic messages as array from ApiError rawMessages', () => {
      const error = new ApiError(409, 'Conflict', {
        rawMessages: ['Lỗi điểm', 'Lỗi môn'],
      })
      expect(extractApiErrorMessages(error)).toEqual(['Lỗi điểm', 'Lỗi môn'])
    })

    it('extracts messages from RFC 7807 detail', () => {
      const error = { detail: 'Chưa có điểm tổng kết thường' }
      expect(extractApiErrorMessages(error)).toEqual(['Chưa có điểm tổng kết thường'])
    })

    it('extracts messages from plain object with errors array', () => {
      const error = { errors: ['Lỗi 1', 'Lỗi 2'] }
      expect(extractApiErrorMessages(error)).toEqual(['Lỗi 1', 'Lỗi 2'])
    })

    it('extracts messages from plain object with errors map', () => {
      const error = { errors: { studentId: ['Không tìm thấy học sinh'], score: ['Điểm không hợp lệ'] } }
      expect(extractApiErrorMessages(error)).toEqual([
        'studentId: Không tìm thấy học sinh',
        'score: Điểm không hợp lệ',
      ])
    })

    it('falls back to fallback array on generic or empty error', () => {
      const error = new ApiError(409, 'Conflict')
      expect(extractApiErrorMessages(error, 'Fallback message')).toEqual(['Fallback message'])
    })
  })

  describe('extractApiError', () => {
    it('returns single string when only 1 message exists', () => {
      const error = new ApiError(409, 'Chưa có điểm tổng kết thường', {
        rawMessages: ['Chưa có điểm tổng kết thường'],
      })
      expect(extractApiError(error)).toBe('Chưa có điểm tổng kết thường')
    })

    it('returns string array when multiple messages exist', () => {
      const error = new ApiError(409, 'Multiple errors', {
        rawMessages: ['Lỗi 1', 'Lỗi 2'],
      })
      expect(extractApiError(error)).toEqual(['Lỗi 1', 'Lỗi 2'])
    })

    it('returns fallback string when generic', () => {
      const error = new ApiError(409, 'Conflict')
      expect(extractApiError(error, 'Record đã tồn tại')).toBe('Record đã tồn tại')
    })
  })

  describe('extractApiErrorMessage', () => {
    it('prioritizes exact backend string message from ApiError', () => {
      const backendMessage = 'Chưa có điểm tổng kết thường (regular_dtbmh_cn)...'
      const error = new ApiError(409, backendMessage, {
        rawMessages: [backendMessage],
        globalMessages: [backendMessage],
      })

      expect(extractApiErrorMessage(error, 'Fallback message')).toBe(backendMessage)
    })

    it('prioritizes backend message array from ApiError rawMessages with proper punctuation', () => {
      const error = new ApiError(409, 'Chưa có điểm thường Cần nhập điểm trước', {
        rawMessages: ['Chưa có điểm thường', 'Cần nhập điểm trước'],
      })

      expect(extractApiErrorMessage(error, 'Fallback message')).toBe(
        'Chưa có điểm thường. Cần nhập điểm trước',
      )
    })

    it('falls back to fallback message when ApiError message is generic', () => {
      const error = new ApiError(409, 'The request conflicts with existing data.', {
        rawMessages: [],
        globalMessages: [],
      })

      expect(extractApiErrorMessage(error, '409 Conflict fallback')).toBe('409 Conflict fallback')
    })

    it('falls back to fallback message when ApiError message is bare HTTP reason with period', () => {
      const error = new ApiError(409, 'Conflict.', {
        rawMessages: [],
        globalMessages: [],
      })

      expect(extractApiErrorMessage(error, 'Custom fallback')).toBe('Custom fallback')
    })

    it('returns fallback message on network failure', () => {
      const networkError = new ApiError(0, 'Unable to reach the server.', {
        kind: 'network',
      })

      expect(extractApiErrorMessage(networkError, 'Mất kết nối máy chủ.')).toBe('Mất kết nối máy chủ.')
    })

    it('extracts message from plain object with array message and joins cleanly', () => {
      const customError = {
        message: ['Lỗi 1', 'Lỗi 2'],
      }

      expect(extractApiErrorMessage(customError, 'Fallback')).toBe('Lỗi 1. Lỗi 2')
    })

    it('extracts message from standard Error instance if not generic', () => {
      const standardError = new Error('Database connection timed out')
      expect(extractApiErrorMessage(standardError, 'Fallback')).toBe('Database connection timed out')
    })

    it('uses fallback when standard Error has empty or generic message', () => {
      const emptyError = new Error('')
      expect(extractApiErrorMessage(emptyError, 'Fallback')).toBe('Fallback')

      const genericError = new Error('Error')
      expect(extractApiErrorMessage(genericError, 'Fallback')).toBe('Fallback')
    })
  })
})

