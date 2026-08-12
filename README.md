# Phần mềm quản trị đào tạo - Quản lý khối lượng giảng dạy

Tài liệu này hướng dẫn cài đặt và chạy dự án trên máy Windows theo cách dễ nhất, dành cho cả người không biết lập trình.

## 1. Phần mềm này gồm những gì?

Dự án có 3 phần chính:

- `frontend`: giao diện web người dùng, chạy ở địa chỉ `http://localhost:3000`.
- `backend`: máy chủ xử lý dữ liệu, API, đăng nhập, tính khối lượng giảng dạy.
- `postgres`: cơ sở dữ liệu lưu người dùng, dữ liệu import, quy tắc tính và kết quả.

Cách chạy khuyến nghị là dùng Docker. Khi dùng Docker, bạn không cần tự cài riêng database hay cấu hình phức tạp.

## 2. Yêu cầu máy tính

Máy nên dùng:

- Windows 10 hoặc Windows 11 64-bit.
- RAM tối thiểu 8 GB, khuyến nghị 16 GB.
- Còn trống ít nhất 10 GB ổ đĩa.
- Có quyền Administrator trên máy.
- Có Internet để tải công cụ lần đầu.

## 3. Cách cài dễ nhất bằng file tự động

Trong thư mục dự án có file:

```text
cai-dat-tu-dong.txt
```

Vì Windows không cho chạy trực tiếp file `.txt` như chương trình, hãy làm như sau:

1. Mở thư mục dự án `C:\Users\Dell\Desktop\streamlit`.
2. Đổi tên file `cai-dat-tu-dong.txt` thành `cai-dat-tu-dong.ps1`.
3. Bấm chuột phải vào nút Start của Windows.
4. Chọn `Terminal (Admin)` hoặc `Windows PowerShell (Admin)`.
5. Chạy lệnh:

```powershell
cd C:\Users\Dell\Desktop\streamlit
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\cai-dat-tu-dong.ps1
```

File này sẽ tự động:

- Kiểm tra quyền Administrator.
- Cài Docker Desktop nếu chưa có.
- Cài Git nếu chưa có.
- Cài Java 21 nếu chưa có.
- Cài Node.js LTS nếu chưa có.
- Tạo file `.env` từ `.env.example` nếu chưa có.
- Build và chạy toàn bộ hệ thống bằng Docker.

Sau khi chạy xong, mở trình duyệt và vào:

```text
http://localhost:3000
```

## 4. Chạy thủ công bằng Docker

Nếu máy đã có Docker Desktop, bạn có thể chạy nhanh như sau.

Mở PowerShell tại thư mục dự án:

```powershell
cd C:\Users\Dell\Desktop\streamlit
```

Nếu chưa có file `.env`, tạo từ file mẫu:

```powershell
Copy-Item .env.example .env
```

Chạy phần mềm:

```powershell
docker compose up -d --build
```

Kiểm tra các container:

```powershell
docker compose ps
```

Nếu mọi thứ chạy đúng, bạn sẽ thấy các service:

- `klgd-postgres`
- `klgd-backend`
- `klgd-frontend`

Mở giao diện:

```text
http://localhost:3000
```

## 5. Tài khoản đăng nhập mặc định

Tài khoản mặc định thường dùng để đăng nhập lần đầu:

```text
Tên đăng nhập: admin
Mật khẩu: admin123
```

Sau khi đăng nhập, nên đổi mật khẩu nếu triển khai dùng thật.

## 6. Các lệnh thường dùng

Dừng phần mềm:

```powershell
docker compose down
```

Chạy lại phần mềm:

```powershell
docker compose up -d
```

Build lại sau khi sửa code:

```powershell
docker compose up -d --build
```

Xem log backend:

```powershell
docker logs -f klgd-backend
```

Xem log frontend:

```powershell
docker logs -f klgd-frontend
```

Xem log database:

```powershell
docker logs -f klgd-postgres
```

## 7. Cập nhật phiên bản mới

Khi có code mới, chạy:

```powershell
cd C:\Users\Dell\Desktop\streamlit
docker compose up -d --build
```

Nếu có thay đổi database, backend sẽ tự chạy migration khi khởi động.

## 8. Sao lưu dữ liệu

Dữ liệu được lưu trong Docker volume tên `streamlit_postgres-data` hoặc tên tương tự tùy thư mục.

Sao lưu database ra file:

```powershell
docker exec klgd-postgres pg_dump -U klgd -d klgd > backup-klgd.sql
```

Khôi phục database từ file sao lưu:

```powershell
Get-Content backup-klgd.sql | docker exec -i klgd-postgres psql -U klgd -d klgd
```

## 9. Chạy kiểu lập trình viên, không dùng Docker

Cách này chỉ dành cho người phát triển.

Bạn cần cài:

- Java JDK 21.
- Maven.
- Node.js LTS.
- PostgreSQL 16.

Chạy backend:

```powershell
cd C:\Users\Dell\Desktop\streamlit\backend
mvn spring-boot:run
```

Chạy frontend:

```powershell
cd C:\Users\Dell\Desktop\streamlit\frontend
npm install
npm run dev
```

Frontend dev mặc định chạy ở:

```text
http://localhost:5173
```

## 10. Kiểm tra trước khi bàn giao

Chạy kiểm tra backend:

```powershell
cd C:\Users\Dell\Desktop\streamlit\backend
mvn test
```

Build frontend:

```powershell
cd C:\Users\Dell\Desktop\streamlit\frontend
npm run build
```

Kiểm tra lỗi UTF-8:

```powershell
cd C:\Users\Dell\Desktop\streamlit\frontend
npm run check:encoding
```

## 11. Lỗi thường gặp

### Không mở được `http://localhost:3000`

Kiểm tra Docker Desktop đã chạy chưa, sau đó chạy:

```powershell
docker compose ps
```

Nếu frontend chưa chạy, chạy lại:

```powershell
docker compose up -d --build
```

### Backend chưa healthy

Chạy:

```powershell
docker logs -f klgd-backend
```

Đợi 1-2 phút vì lần đầu backend cần migrate database.

### Cổng 3000 hoặc 8080 bị chiếm

Mở file `.env` và đổi:

```env
FRONTEND_PORT=3001
BACKEND_PORT=8081
```

Sau đó chạy lại:

```powershell
docker compose up -d --build
```

### Docker báo lỗi chưa bật WSL

Mở PowerShell Admin và chạy:

```powershell
wsl --install
```

Sau đó khởi động lại máy, mở Docker Desktop và chạy lại dự án.

## 12. Ghi chú quan trọng

- Không xóa Docker volume nếu chưa sao lưu, vì dữ liệu import và kết quả tính nằm trong database.
- File `.env` chứa mật khẩu database và khóa JWT, không nên gửi cho người khác khi triển khai thật.
- Khi đổi công thức ở trang Quy tắc tính, cần bấm lưu để hệ thống lưu xuống database và tính lại dữ liệu.
