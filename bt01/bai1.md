# BÁO CÁO BÀI TẬP 1: TRIỂN KHAI CẤU HÌNH ĐA MÔI TRƯỜNG (PROFILES)

## Phần 1: Tiêu đề bài tập và Yêu cầu đề bài
- **Tên bài tập:** Bài 1: Triển khai cấu hình đa môi trường (Profiles)
- **Bối cảnh:** Thiết lập hạ tầng Hybrid AI linh hoạt cho hệ thống AI Logistics Incident Reporter. Sử dụng `qwen2.5-coder:7b` qua Ollama ở môi trường `local` và tự động chuyển sang mô hình `gemini-2.5-flash` qua cổng Aggregator OpenRouter ở môi trường `cloud` mà không thay đổi mã nguồn Java.
- **Yêu cầu chi tiết:**
  1. Thiết lập 3 file thuộc tính Spring Boot: `application.properties` (profile mặc định là local), `application-local.properties` (Ollama), và `application-cloud.properties` (OpenRouter API, đọc key từ `${ROUTER_API_KEY}`).
  2. Viết class REST Controller `SystemConfigController` cung cấp endpoint `/api/v1/incident/config` để hiển thị tên mô hình LLM đang kích hoạt.
  3. Lập luận giải thích cơ chế Spring Boot tự động nạp cấu hình và khởi tạo Bean tương ứng.
  4. Minh chứng chạy thực tế bằng các dòng log console hiển thị đúng profile khởi chạy.

---

## Phần 2: Giả lập cuộc trò chuyện thực tế với AI

### **Người dùng gửi Prompt:**
> "Xin chào AI, tôi đang thiết kế hệ thống AI Logistics Incident Reporter sử dụng Spring Boot và Spring AI. Tôi cần bạn giúp thiết kế cấu hình đa môi trường (Profiles) với yêu cầu:
> 1. File `application.properties` mặc định kích hoạt profile `local`.
> 2. File `application-local.properties` kết nối với mô hình `qwen2.5-coder:7b` chạy cục bộ trên Ollama cổng 11434.
> 3. File `application-cloud.properties` kết nối với `google/gemini-2.5-flash` qua OpenRouter API. API key cần đọc từ biến môi trường `ROUTER_API_KEY`.
> 4. Viết REST Controller `/api/v1/incident/config` để xuất thông tin kiểm tra xem mô hình nào đang chạy.
> 5. Hãy giải thích chi tiết cơ chế nạp profile động của Spring Boot và khởi tạo Bean tương ứng khi thay đổi tham số `--spring.profiles.active`.
> Hãy viết đầy đủ code, file cấu hình và giải thích logic một cách chuyên nghiệp nhất!"

### **AI phản hồi chi tiết:**

Chào bạn! Dưới đây là giải pháp thiết kế kiến trúc cấu hình đa môi trường Hybrid AI hoàn chỉnh cho dự án **AI Logistics Incident Reporter**.

### 1. Kiến trúc Cấu hình & Properties
Để cô lập thông số kết nối của các mô hình AI theo từng môi trường mà không cần thay đổi source code, ta phân tách thành 3 tệp tin cấu hình:

*   **application.properties** (Cấu hình chung và mặc định):
    ```properties
    spring.application.name=ai-logistics-incident-reporter
    spring.profiles.active=local
    ```

*   **application-local.properties** (Môi trường phát triển nội bộ):
    ```properties
    spring.ai.ollama.base-url=http://localhost:11434
    spring.ai.ollama.chat.options.model=qwen2.5-coder:7b
    ai.model.name=qwen2.5-coder:7b (Chạy Local qua Ollama)
    server.port=8080
    ```

*   **application-cloud.properties** (Môi trường production/cloud):
    ```properties
    spring.ai.openai.base-url=https://openrouter.ai/api/v1
    spring.ai.openai.api-key=${ROUTER_API_KEY}
    spring.ai.openai.chat.options.model=google/gemini-2.5-flash
    ai.model.name=google/gemini-2.5-flash (Chạy Cloud qua OpenRouter)
    server.port=8081
    ```

### 2. Viết Mã Nguồn `SystemConfigController`
Lớp này chịu trách nhiệm hiển thị trạng thái cấu hình hiện tại thông qua endpoint HTTP REST giúp Dev và DevOps kiểm soát nhanh tình trạng hoạt động.

```java
package com.example.ailogistics.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/incident")
public class SystemConfigController {

    @Value("${ai.model.name}")
    private String modelName;

    private final Environment environment;

    public SystemConfigController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("applicationName", environment.getProperty("spring.application.name"));
        config.put("activeProfiles", environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles() : new String[]{"default"});
        config.put("activeModel", modelName);
        config.put("serverPort", environment.getProperty("local.server.port", environment.getProperty("server.port")));
        return config;
    }
}
```

---

### 3. Giải thích Lập luận Cơ chế Nạp Profile Động của Spring Boot

Cơ chế quản lý thuộc tính và đa môi trường (Profiles) của Spring Boot vận hành cực kỳ tinh tế dựa trên 2 khái niệm cốt lõi: **Environment Abstraction** và **PropertySource Precedence**.

#### A. Thứ tự ưu tiên nạp cấu hình (Precedence Order)
Khi Spring Boot khởi động, interface `Environment` được khởi tạo và nạp các nguồn dữ liệu cấu hình theo thứ tự ưu tiên tăng dần (nguồn sau đè nguồn trước):
1. Các giá trị mặc định của Spring Boot.
2. Thuộc tính khai báo trong `application.properties` nằm trong classpath.
3. Các tệp tin cấu hình đặc tả profile: `application-{profile}.properties`.
4. Biến môi trường hệ điều hành (Environment Variables).
5. Tham số dòng lệnh (Command-line Arguments).

#### B. Quá trình xử lý động khi thay đổi `--spring.profiles.active`
Khi bạn truyền tham số `--spring.profiles.active=cloud` lúc khởi chạy ứng dụng:

1.  **Giai đoạn Bootstrap & Bootstrap Context:**
    Spring Boot đọc tham số dòng lệnh `--spring.profiles.active` đầu tiên (đây là nguồn có độ ưu tiên cao nhất, đè lên giá trị mặc định `spring.profiles.active=local` khai báo trong file `application.properties`).
2.  **Kích hoạt Profiles trong Environment:**
    Spring Boot thiết lập danh sách active profile trong đối tượng `Environment` thành `["cloud"]`.
3.  **Quá trình quét và nạp file cấu hình bổ sung:**
    Sau khi xác định profile đang active là `cloud`, bộ nạp tệp cấu hình của Spring Boot (`ConfigFileApplicationListener` hoặc `ConfigDataEnvironmentPostProcessor` trên phiên bản 3.x) sẽ tìm kiếm và tải tệp tin có tên khớp định dạng `application-cloud.properties` hoặc `application-cloud.yml` trong Classpath.
4.  **Ghi đè thuộc tính (Property Overriding):**
    Các cấu hình trong `application-cloud.properties` sẽ được nạp đè lên các cấu hình trùng tên trong `application.properties` gốc. Do đó, cổng `server.port` chuyển thành `8081` và `ai.model.name` trở thành `google/gemini-2.5-flash (Chạy Cloud qua OpenRouter)`.
5.  **Cơ chế nội suy Biến Môi trường:**
    Khi xử lý dòng `spring.ai.openai.api-key=${ROUTER_API_KEY}`, Spring Boot tự động tra cứu trong danh sách biến môi trường hệ thống xem có key `ROUTER_API_KEY` nào hay không để truyền thẳng giá trị đó vào cấu hình Bean Spring AI OpenAI.
6.  **Khởi tạo Bean có điều kiện (Conditional Bean Instantiation):**
    - Các auto-configuration của Spring AI như `OllamaAutoConfiguration` sử dụng `@ConditionalOnProperty` hoặc `@ConditionalOnClass`. Vì không tìm thấy thuộc tính kết nối hợp lệ của Ollama ở profile cloud, Spring Boot sẽ từ chối khởi tạo `OllamaChatModel` bean.
    - Ngược lại, nhờ các cấu hình API key và Base URL của OpenAI được điền đầy đủ ở profile `cloud`, Spring Boot sẽ khởi tạo thành công bean `OpenAiChatModel` để thực thi các tác vụ kết nối với OpenRouter.

---

### 4. Minh chứng Chạy thực tế (Simulation Logs)

#### KỊCH BẢN 1: Khởi chạy ở chế độ LOCAL (Mặc định hoặc truyền `--spring.profiles.active=local`)
```text
$ java -jar target/ai-logistics-0.0.1-SNAPSHOT.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |__\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.4)

2025-02-18T10:00:01.123+07:00  INFO 12345 --- [           main] c.e.a.AiLogisticsApplication             : Starting AiLogisticsApplication v0.0.1-SNAPSHOT on localhost with PID 12345
2025-02-18T10:00:01.125+07:00  INFO 12345 --- [           main] c.e.a.AiLogisticsApplication             : The following 1 profile is active: "local"
2025-02-18T10:00:02.550+07:00  INFO 12345 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2025-02-18T10:00:02.580+07:00  INFO 12345 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 1 endpoint(s) beneath platform path '/actuator'
2025-02-18T10:00:03.110+07:00  INFO 12345 --- [           main] c.e.a.AiLogisticsApplication             : Started AiLogisticsApplication in 2.345 seconds (process running for 2.89)
```
**Gọi API thử nghiệm ở Local:**
```bash
$ curl http://localhost:8080/api/v1/incident/config
```
**Kết quả trả về:**
```json
{
  "applicationName": "ai-logistics-incident-reporter",
  "activeProfiles": ["local"],
  "activeModel": "qwen2.5-coder:7b (Chạy Local qua Ollama)",
  "serverPort": "8080"
}
```

---

#### KỊCH BẢN 2: Khởi chạy ở chế độ CLOUD (Truyền `--spring.profiles.active=cloud`)
```text
$ export ROUTER_API_KEY=sk-or-v1-abcdef1234567890
$ java -jar target/ai-logistics-0.0.1-SNAPSHOT.jar --spring.profiles.active=cloud

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |__\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.4)

2025-02-18T10:05:12.456+07:00  INFO 12400 --- [           main] c.e.a.AiLogisticsApplication             : Starting AiLogisticsApplication v0.0.1-SNAPSHOT on cloud-server-01 with PID 12400
2025-02-18T10:05:12.458+07:00  INFO 12400 --- [           main] c.e.a.AiLogisticsApplication             : The following 1 profile is active: "cloud"
2025-02-18T10:05:13.880+07:00  INFO 12400 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8081 (http)
2025-02-18T10:05:14.300+07:00  INFO 12400 --- [           main] c.e.a.AiLogisticsApplication             : Started AiLogisticsApplication in 2.112 seconds (process running for 2.65)
```
**Gọi API thử nghiệm ở Cloud:**
```bash
$ curl http://localhost:8081/api/v1/incident/config
```
**Kết quả trả về:**
```json
{
  "applicationName": "ai-logistics-incident-reporter",
  "activeProfiles": ["cloud"],
  "activeModel": "google/gemini-2.5-flash (Chạy Cloud qua OpenRouter)",
  "serverPort": "8081"
}
```