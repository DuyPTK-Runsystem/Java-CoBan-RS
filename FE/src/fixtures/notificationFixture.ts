import type { NotificationItem, NotificationPage, NotificationReceipt } from '@/types/notification'

export const mockNotificationUnread: NotificationItem = {
  id: 101,
  title: 'Thông báo nghỉ học phòng chống bão',
  body: 'Toàn trường nghỉ học từ chiều thứ 6 do ảnh hưởng áp thấp nhiệt đới.',
  channel: 'IN_APP',
  status: 'PUBLISHED',
  audienceType: 'SCHOOL',
  schoolScope: 'DEFAULT_SCHOOL',
  senderId: 1,
  publishAt: '2026-09-15T08:00:00',
  expiresAt: '2026-09-20T23:59:59',
  version: 1,
  createdAt: '2026-09-15T07:30:00',
  updatedAt: '2026-09-15T08:00:00',
  read: false,
  readAt: null,
}

export const mockNotificationRead: NotificationItem = {
  id: 102,
  title: 'Kế hoạch kiểm tra giữa kỳ 1',
  body: 'Lịch thi giữa kỳ đã được cập nhật trên cổng thông tin học vụ.',
  channel: 'IN_APP',
  status: 'PUBLISHED',
  audienceType: 'CLASS',
  targetReference: '10A1',
  schoolScope: 'DEFAULT_SCHOOL',
  senderId: 2,
  publishAt: '2026-09-14T09:00:00',
  expiresAt: '2026-10-15T23:59:59',
  version: 1,
  createdAt: '2026-09-14T08:45:00',
  updatedAt: '2026-09-14T09:00:00',
  read: true,
  readAt: '2026-09-14T14:20:00',
}

export const mockNotificationDraft: NotificationItem = {
  id: 103,
  title: 'Dự thảo thông báo họp phụ huynh đầu năm',
  body: 'Kính gửi quý phụ huynh, trường tổ chức buổi họp vào sáng Chủ nhật.',
  channel: 'IN_APP',
  status: 'DRAFT',
  audienceType: 'CLASS',
  targetReference: '10A2',
  schoolScope: 'DEFAULT_SCHOOL',
  senderId: 2,
  version: 0,
  createdAt: '2026-09-15T10:00:00',
  updatedAt: '2026-09-15T10:00:00',
  read: null,
  readAt: null,
}

export const mockNotificationScheduled: NotificationItem = {
  id: 104,
  title: 'Lịch khai mạc giải bóng đá học sinh',
  body: 'Lễ khai mạc diễn ra tại sân vận động trường vào sáng thứ 2 tuần tới.',
  channel: 'IN_APP',
  status: 'SCHEDULED',
  audienceType: 'SCHOOL',
  schoolScope: 'DEFAULT_SCHOOL',
  senderId: 1,
  publishAt: '2026-09-20T07:00:00',
  expiresAt: '2026-09-30T23:59:59',
  version: 1,
  createdAt: '2026-09-15T11:00:00',
  updatedAt: '2026-09-15T11:00:00',
  read: null,
  readAt: null,
}

export const mockNotificationCancelled: NotificationItem = {
  id: 105,
  title: 'Buổi dã ngoại ngoại khóa (Đã hủy)',
  body: 'Do thời tiết xấu nên buổi tham quan di tích lịch sử tạm hoãn.',
  channel: 'IN_APP',
  status: 'CANCELLED',
  audienceType: 'INDIVIDUAL',
  targetReference: '10,11,12',
  schoolScope: 'DEFAULT_SCHOOL',
  senderId: 1,
  version: 2,
  createdAt: '2026-09-13T09:00:00',
  updatedAt: '2026-09-14T10:00:00',
  read: true,
  readAt: '2026-09-13T10:00:00',
}

export const mockNotificationExpired: NotificationItem = {
  id: 106,
  title: 'Hạn chót đăng ký câu lạc bộ tiếng Anh',
  body: 'Học sinh hoàn thành đơn đăng ký trước ngày 10/09.',
  channel: 'IN_APP',
  status: 'EXPIRED',
  audienceType: 'SCHOOL',
  schoolScope: 'DEFAULT_SCHOOL',
  senderId: 3,
  publishAt: '2026-09-01T08:00:00',
  expiresAt: '2026-09-10T23:59:59',
  version: 1,
  createdAt: '2026-09-01T07:30:00',
  updatedAt: '2026-09-01T08:00:00',
  read: true,
  readAt: '2026-09-02T09:15:00',
}

export const mockNotificationPage: NotificationPage = {
  meta: {
    page: 0,
    pageSize: 10,
    totalPages: 1,
    totalItems: 3,
  },
  result: [mockNotificationUnread, mockNotificationRead, mockNotificationExpired],
}

export const mockManagedNotificationPage: NotificationPage = {
  meta: {
    page: 0,
    pageSize: 10,
    totalPages: 1,
    totalItems: 4,
  },
  result: [mockNotificationDraft, mockNotificationScheduled, mockNotificationUnread, mockNotificationCancelled],
}

export const mockReceipt: NotificationReceipt = {
  id: 501,
  notificationId: 101,
  recipientUserId: 10,
  accessScope: 'SCHOOL',
  readAt: '2026-09-15T09:30:00',
  createdAt: '2026-09-15T08:00:00',
}
