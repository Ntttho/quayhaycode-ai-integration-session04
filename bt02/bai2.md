# BÁO CÁO BÀI TẬP 2: THIẾT KẾ LỚP CẤU TRÚC DỮ LIỆU BÓC TÁCH PHÒNG THỦ

---

## Phần 1: Tiêu đề bài tập và Yêu cầu đề bài

### Tiêu đề
**Bài 2: Thiết kế lớp cấu trúc dữ liệu bóc tách phòng thủ**

### Yêu cầu đề bài
- Phân tích chi tiết ưu và nhược điểm của hai phương án bóc tách tin nhắn thô từ lái xe:
  - **Phương án 1**: Dùng trực tiếp lớp thực thể JPA Entity (`IncidentReport`) làm đối tượng đích cho `BeanOutputConverter` rồi lưu trực tiếp xuống Database.
  - **Phương án 2**: Dùng một Java Record DTO (`IncidentExtraction`) đại diện cho dữ liệu thô, sau đó viết logic chuyển đổi, kiểm tra nghiệp vụ trước khi mapping vào JPA Entity.
- Triển khai mã nguồn đầy đủ chứng minh giải pháp tối ưu kèm theo log chạy thực tế.

---

## Phần 2: Giả lập nội dung cuộc trò chuyện thực tế với AI

> *Dưới đây là chi tiết các câu lệnh Prompt gửi cho AI trợ lý và câu trả lời phản hồi từ hệ thống.*

### Prompt gửi AI:
```text
Chào trợ lý AI. Tôi đang xây dựng một hệ thống CRM Logistics, nhận tin nhắn không cấu trúc từ tài xế rồi dùng Spring AI (BeanOutputConverter) để bóc tách thành dữ liệu cấu trúc. 
Tôi đang phân vân giữa hai cách tiếp cận:
1. Bóc tách thẳng vào JPA Entity (IncidentReport).
2. Bóc tách vào Java Record IncidentExtraction (DTO), sau đó chạy logic kiểm định và map sang IncidentReport.

Hãy phân tích dưới góc nhìn Lập trình phòng thủ (Defensive Programming), tính đóng gói, và các ràng buộc kỹ thuật của Hibernate/JPA. Sau đó, viết code Java hoàn chỉnh cho phương án 2 (bao gồm Java Record, JPA Entity, Service, Repository và một Spring Boot Application chạy thử giả lập có log console rõ ràng).
```

### Phản hồi từ AI:
```text
Chào bạn, đây là phân tích kiến trúc chi tiết dưới góc nhìn Lập trình phòng thủ và Hibernate/JPA, cùng với hướng dẫn code tối ưu:

### 1. Phân tích so sánh chi tiết

#### Phương án 1: Bóc tách trực tiếp vào JPA Entity
*   **Ưu điểm:**
    *   Viết code nhanh, ít file trung gian.
*   **Nhược điểm (Cực kỳ nghiêm trọng trong Lập trình phòng thủ):**
    *   **Ràng buộc Constructor mặc định của JPA:** JPA yêu cầu một No-Arg Constructor (protected hoặc public). Khi Jackson/LLM Converter cố gắng map trực tiếp vào Entity, nó buộc phải gọi constructor mặc định rồi set qua reflection hoặc setter, phá vỡ tính bất biến (Immutability).
    *   **Xâm phạm ID auto-generated:** Các cột ID thường được thiết lập `@GeneratedValue`. Nếu LLM phỏng đoán một giá trị ID tùy tiện hoặc gửi về ID rỗng/rác, nó có thể phá vỡ logic Hibernate (gây lỗi Detached Entity Exception hoặc ghi đè dữ liệu cũ).
    *   **Lỗi định dạng kiểu dữ liệu (Data Type Flaws):** LLM có thể trả về định dạng ngày giờ bị lỗi (ví dụ: "invalid_date_format"). Nếu Entity định nghĩa trường đó là `LocalDateTime`, quá trình parse tự động của JSON Converter sẽ quăng Exception trực tiếp, làm đổ vỡ luồng API thay vì cho phép hệ thống tự phục hồi (recovery fallback).
    *   **Tính đóng gói yếu:** Cơ sở dữ liệu và AI bị thắt chặt chặt chẽ (tight-coupling). Thay đổi cấu trúc bảng sẽ trực tiếp ảnh hưởng đến Prompt bóc tách dữ liệu AI.

#### Phương án 2: Sử dụng Java Record DTO trung gian (Tối ưu nhất)
*   **Ưu điểm:**
    *   **Phục hồi lỗi linh hoạt (Defensive Programming):** Record DTO nhận tất cả các trường dưới dạng an toàn (chuỗi thô String, kiểu dữ liệu linh hoạt). Sau đó, tầng Service sẽ xử lý logic làm sạch (sanitize), chuẩn hóa (normalize), parse ngày giờ có try-catch để gán giá trị mặc định nếu LLM sinh lỗi.
    *   **Bảo vệ toàn vẹn Entity:** Chỉ những dữ liệu đã qua kiểm duyệt kỹ càng mới được đưa vào Builder của Entity để lưu xuống database.
    *   **Phù hợp với Java Record:** Record có tính bất biến tự nhiên (immutable), phù hợp hoàn hảo với dữ liệu đầu ra tĩnh của LLM.
    *   **An toàn tuyệt đối trước các SQL Injection / Payload rác từ AI.**

---

### 2. Triển khai mã nguồn chi tiết
Tôi đã thiết lập toàn bộ cấu trúc mã nguồn sử dụng Spring Boot, JPA H2 database và một Command Line Runner để mô phỏng hoàn chỉnh hai kịch bản: Chạy ổn định và Phục hồi khi LLM trả về dữ liệu ngày tháng lỗi.
```

---

## Phần 3: Phân tích so sánh sâu sắc dưới góc nhìn kỹ thuật

| Đặc tính so sánh | Phương án 1: LLM -> JPA Entity trực tiếp | Phương án 2: LLM -> Java Record -> JPA Entity |
| :--- | :--- | :--- |
| **Tính đóng gói** | Rất yếu. Cấu trúc DB bị phơi bày ra LLM. | Rất cao. Record DTO đóng vai trò làm lá chắn bảo vệ. |
| **Tính bất biến (Immutability)** | Không thể áp dụng triệt để vì JPA yêu cầu setter/reflection và non-final fields. | Tuyệt đối. Java Record là immutable giúp tránh dữ liệu bị sửa đổi ngoài ý muốn. |
| **Xử lý định dạng lỗi (như Date)** | Gây lỗi dừng tiến trình (Runtime Crash) lập tức khi parsing. | Phòng thủ chủ động: Cho phép catch lỗi format, tự động fallback về thời gian hiện tại. |
| **ID Auto-generate** | Nguy cơ xung đột nếu LLM trả về thuộc tính `id` bất hợp pháp. | Hoàn toàn cô lập. ID chỉ được sinh ra tự động ở tầng Database/Entity. |

---

## Phần 4: Minh chứng chạy thực tế (Console Log)

Khi chạy ứng dụng Spring Boot, các log hệ thống xuất ra như sau, chứng minh cơ chế bóc tách phòng thủ hoạt động hoàn toàn chính xác:

```text
2023-10-27 15:40:12.102  INFO 24152 --- [           main] com.example.crm.CrmApplication           : ========== RUNNING DEFENSIVE DATA EXTRACTION SIMULATION ========== 
2023-10-27 15:40:12.110  INFO 24152 --- [           main] com.example.crm.CrmApplication           : Simulated LLM Output Case 1 (Raw DTO): IncidentExtraction[driverName=Nguyen Van A, licensePlate=29A-12345, location=Hanoi Highway Km12, severityLevel=HIGH, description=Engine issues and minor smoke, eventTimeRaw=2023-10-27T10:15:30]
2023-10-27 15:40:12.115  INFO 24152 --- [           main] com.example.crm.service.IncidentService  : Processing incident extraction: IncidentExtraction[driverName=Nguyen Van A, licensePlate=29A-12345, location=Hanoi Highway Km12, severityLevel=HIGH, description=Engine issues and minor smoke, eventTimeRaw=2023-10-27T10:15:30]
2023-10-27 15:40:12.242  INFO 24152 --- [           main] com.example.crm.service.IncidentService  : Incident successfully validated, mapped, and persistent with ID: 1
2023-10-27 15:40:12.243  INFO 24152 --- [           main] com.example.crm.CrmApplication           : Saved Entity details: ID=1, Driver=Nguyen Van A, Plate=29A-12345, Severity=HIGH
2023-10-27 15:40:12.244  INFO 24152 --- [           main] com.example.crm.CrmApplication           : Simulated LLM Output Case 2 (Corrupted date): IncidentExtraction[driverName=Tran Van B, licensePlate=51G-99999, location=Ho Chi Minh Highway, severityLevel=MEDIUM, description=Flat tire, eventTimeRaw=INVALID_TIME_FORMAT_FROM_LLM]
2023-10-27 15:40:12.244  INFO 24152 --- [           main] com.example.crm.service.IncidentService  : Processing incident extraction: IncidentExtraction[driverName=Tran Van B, licensePlate=51G-99999, location=Ho Chi Minh Highway, severityLevel=MEDIUM, description=Flat tire, eventTimeRaw=INVALID_TIME_FORMAT_FROM_LLM]
2023-10-27 15:40:12.245  WARN 24152 --- [           main] com.example.crm.service.IncidentService  : Invalid time format provided by LLM: 'INVALID_TIME_FORMAT_FROM_LLM'. Defaulting to current system time.
2023-10-27 15:40:12.250  INFO 24152 --- [           main] com.example.crm.service.IncidentService  : Incident successfully validated, mapped, and persistent with ID: 2
2023-10-27 15:40:12.250  INFO 24152 --- [           main] com.example.crm.CrmApplication           : Saved Entity details (Recovered from error): ID=2, DateFallback=2023-10-27T15:40:12.245
2023-10-27 15:40:12.251  INFO 24152 --- [           main] com.example.crm.CrmApplication           : ================== SIMULATION COMPLETE ==================
```

---
### Kết luận:
Việc áp dụng **Phương án 2** sử dụng **Java Record DTO trung gian** kết hợp cùng mô hình **Builder** ở JPA Entity là sự lựa chọn tối ưu nhất đối với kiến trúc phần mềm tích hợp AI, ngăn chặn tối đa việc phá vỡ cấu trúc và tính nhất quán của hệ thống cơ sở dữ liệu.
