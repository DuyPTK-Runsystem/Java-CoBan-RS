import { extractApiErrorMessage, isApiError } from '@/types/api'

const LIBRARY_ERROR_MESSAGES: Record<string, string> = {
  COPY_ALREADY_ON_LOAN: 'Bản sao vừa được mượn. Tải lại dữ liệu rồi thử lại.',
  MAX_ACTIVE_LOANS: 'Bạn đọc đã đạt giới hạn loan đang hoạt động.',
  REFERENCE_ONLY: 'Bản sao này chỉ dùng tại thư viện.',
  PATRON_BORROWING_SUSPENDED: 'Bạn đọc đang bị đình chỉ mượn.',
  CARD_EXPIRED: 'Thẻ thư viện đã hết hạn.',
  CARD_REVOKED: 'Thẻ thư viện đã bị thu hồi.',
  CARD_PAYLOAD_MALFORMED: 'QR thẻ không đúng định dạng.',
  CARD_SIGNATURE_INVALID: 'Không xác thực được QR thẻ.',
  ACTIVE_LOAN_NOT_FOUND: 'Không tìm thấy loan đang hoạt động cho barcode này.',
  RENEW_LIMIT_REACHED: 'Loan đã hết số lần gia hạn cho phép.',
  COPY_RESERVED: 'Bản sao đang được ưu tiên cho reservation.',
  COPY_NOT_FOUND: 'Không tìm thấy barcode bản sao.',
  BOOK_NOT_FOUND: 'Không tìm thấy đầu sách.',
  PATRON_NOT_FOUND: 'Không tìm thấy bạn đọc.',
  RESERVATION_ALREADY_EXISTS: 'Bạn đọc đã có reservation đang hoạt động cho đầu sách này.',
  INVALID_FINE_AMOUNT: 'Số tiền fine không hợp lệ.',
  LIBRARY_RESOURCE_FORBIDDEN: 'Tài khoản không có quyền thực hiện thao tác này.',
  VERSION_CONFLICT: 'Dữ liệu đã được cập nhật bởi người khác. Tải lại rồi thử lại.',
}

export function getLibraryErrorMessage(error: unknown, fallback: string): string {
  const code = isApiError(error) ? error.code : undefined
  return (code && LIBRARY_ERROR_MESSAGES[code]) || extractApiErrorMessage(error, fallback)
}
