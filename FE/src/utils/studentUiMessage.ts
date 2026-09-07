const messages: Record<string, string> = {
  'The request is invalid.': 'Yêu cầu không hợp lệ.',
  'Authentication is required.': 'Vui lòng đăng nhập để tiếp tục.',
  'You do not have permission to perform this action.': 'Bạn không có quyền thực hiện thao tác này.',
  'The requested resource was not found.': 'Không tìm thấy dữ liệu được yêu cầu.',
  'The request conflicts with existing data.': 'Yêu cầu xung đột với dữ liệu hiện có.',
  'The server could not complete the request.': 'Máy chủ không thể xử lý yêu cầu.',
  'The request could not be completed.': 'Không thể hoàn tất yêu cầu.',
  'Unable to reach the server.': 'Không thể kết nối đến máy chủ.',
}

export function studentUiMessage(error: unknown, fallback: string): string {
  if (!(error instanceof Error) || !error.message) return fallback
  return messages[error.message] ?? error.message
}
