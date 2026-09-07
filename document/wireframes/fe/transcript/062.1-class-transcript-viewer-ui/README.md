# Wireframe 062.1 — Class Transcript Viewer UI

## Mục đích
Mô phỏng giao diện tra cứu bảng điểm theo lớp (Class Transcript Viewer) dành cho Giáo viên chủ nhiệm (GVCN), Giáo vụ (Academic Office) và Quản trị viên (Admin), bao gồm 2 nhóm bảng điểm chính (theo môn và tổng kết) ứng với từng chu kỳ (Học kỳ & Cả năm).

## 4 Loại Bảng Điểm Chuẩn Hóa Theo Phác Thảo

### 1. Nhóm Bảng Điểm Theo Môn (Mỗi bảng là một môn học)
- **1A. Bảng điểm môn học - Học kỳ** (Phác thảo ảnh 1: `Bảng điểm học kì A - Năm học B - Môn C`):
  - Cột: `STT`, `Họ và tên`, `KTTX` (cột con 1, 2, 3, 4...), `KTĐK` (cột con 1, 2...), `KTCK` (1), `TBMHK`, `Ghi chú`.
  - Cột con sinh động dựa trên số cột thực tế lớn nhất của môn trong học kỳ.
- **1B. Bảng điểm môn học - Cả năm**:
  - Cột: `STT`, `Họ và tên`, `TBM HK1`, `TBM HK2`, `ĐTBCN Ban đầu`, `Điểm Thi lại`, `ĐTBCN Chính thức`, `Ghi chú`.

### 2. Nhóm Bảng Điểm Tổng Kết Lớp
- **2A. Bảng điểm tổng kết - Học kỳ** (Phác thảo ảnh 2: `Bảng điểm học kì A - Năm học B`):
  - Cột: `STT`, `Họ và tên`, các cột điểm trung bình từng môn (`Toán`, `Vật lí`, `Hóa học`, `Sinh học`...), `TBHK`, `Ghi chú`.
- **2B. Bảng điểm tổng kết - Cả năm** (Tương tự cấu trúc 2A: `Bảng điểm cả năm - Năm học B`):
  - Cột: `STT`, `Họ và tên`, các cột điểm trung bình cả năm từng môn (`Toán`, `Vật lí`, `Hóa học`, `Sinh học`...), `TBCN`, `Ghi chú`.
  - **Quy tắc hiển thị thi lại**: Hiển thị trực tiếp trong chính ô môn học đó, ví dụ học sinh thi lại môn Vật lí: ô điểm hiển thị `2.8 (Thi lại: 5.5)`. Cột `TBCN` phản ánh điểm trung bình cả năm chính thức sau thi lại.

## Phân quyền & Điều hướng
- **GVCN**: Chỉ được phép chọn và xem lớp mình làm chủ nhiệm.
- **Admin & Giáo vụ**: Xem được tất cả các lớp trong trường.
- **Điều hướng học sinh**: Bấm vào tên/mã của bất kỳ học sinh nào trong bảng để chuyển hướng sang màn hình tra cứu bảng điểm cá nhân (`/v2/transcripts?studentId={id}`).

## File xem thử
Mở file `index.html` trong thư mục này trên trình duyệt để tương tác trực tiếp.
