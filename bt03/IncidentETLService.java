package com.example.etl.service;

import com.example.etl.dto.IncidentExtraction;
import com.example.etl.entity.IncidentReport;
import com.example.etl.entity.UrgencyLevel;
import com.example.etl.exception.ValidationException;
import com.example.etl.repository.IncidentRepository;
import com.example.etl.util.JsonCleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class IncidentETLService {

    private static final Logger log = LoggerFactory.getLogger(IncidentETLService.class);
    
    // Regex cho biển số xe Việt Nam tiêu chuẩn dạng: 29C-1234, 51F-123.45, 30A-99999
    private static final Pattern VN_LICENSE_PLATE_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]-[0-9]{4,5}(\\.[0-9]{2})?$");

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private IncidentRepository repository;

    /**
     * Tiếp nhận, phân tích, làm sạch, kiểm chứng dữ liệu phòng thủ và lưu báo cáo sự cố.
     */
    @Transactional(rollbackFor = Exception.class)
    public IncidentReport processReport(String rawMessage) {
        log.info("[ETL Start] Nhận được tin nhắn thô từ tài xế: \"{}\"", rawMessage);

        try {
            // 1. Chuẩn bị định dạng phản hồi và Prompt gửi LLM
            BeanOutputConverter<IncidentExtraction> converter = new BeanOutputConverter<>(IncidentExtraction.class);
            String formatInstructions = converter.getFormatInstructions();
            String finalPrompt = "Phân tích tin nhắn sự cố sau: " + rawMessage + "\n" + formatInstructions;
            
            log.debug("[AI Request] Gửi prompt tới ChatModel...");
            String rawResponse = chatModel.call(new Prompt(finalPrompt)).getResult().getOutput().getContent();
            log.info("[AI Raw Response] Nhận kết quả từ LLM: \n{}", rawResponse);

            // 2. Làm sạch chuỗi phản hồi (Xử lý lỗi bọc Markdown)
            String cleanedJson = JsonCleanUtils.cleanJson(rawResponse);
            if (!rawResponse.equals(cleanedJson)) {
                log.info("[Sanitizer] Đã phát hiện và bóc tách thành công JSON từ khối Markdown.");
            } else {
                log.info("[Sanitizer] Chuỗi phản hồi không chứa markdown block. Sử dụng trực tiếp dữ liệu thô.");
            }

            // 3. Khử tuần tự hóa từ chuỗi sạch sang DTO
            IncidentExtraction dto = converter.convert(cleanedJson);
            if (dto == null) {
                throw new ValidationException("Không thể chuyển đổi dữ liệu phản hồi từ AI thành DTO.");
            }

            // 4. Kiểm chứng dữ liệu phòng thủ (Defensive Validation)
            validateDto(dto);

            // 5. Ánh xạ từ DTO hợp lệ sang Database Entity
            IncidentReport entity = IncidentReport.builder()
                    .orderCode(dto.orderCode().trim())
                    .licensePlate(dto.licensePlate().toUpperCase().trim())
                    .incidentType(dto.incidentType().trim())
                    .urgency(UrgencyLevel.valueOf(dto.urgency().toUpperCase().trim()))
                    .build();

            // 6. Lưu xuống cơ sở dữ liệu và ghi nhận trạng thái giao dịch
            IncidentReport savedReport = repository.save(entity);
            log.info("[ETL Success] Lưu thành công báo cáo sự cố ID: {}. Đã commit giao dịch.", savedReport.getId());
            return savedReport;

        } catch (ValidationException ve) {
            log.error("[ETL Failed] Lỗi nghiệp vụ xảy ra trong quá trình xử lý tin nhắn: [{}]", ve.getMessage());
            throw ve; // Re-throw để hệ thống Spring Transaction thực hiện Rollback tự động
        } catch (Exception e) {
            log.error("[ETL Failed] Lỗi hệ thống nghiêm trọng khi xử lý tin nhắn: ", e);
            throw new RuntimeException("Hệ thống ETL tạm thời gián đoạn do lỗi kỹ thuật.", e);
        }
    }

    /**
     * Hàm kiểm tra logic nghiệp vụ phòng thủ (Defensive Validation) thủ công trên DTO
     */
    private void validateDto(IncidentExtraction dto) {
        // Validate trường orderCode
        if (dto.orderCode() == null || dto.orderCode().trim().isEmpty()) {
            throw new ValidationException("Kiểm chứng dữ liệu thất bại: 'orderCode' không được phép để trống hoặc null!");
        }

        // Validate trường licensePlate
        if (dto.licensePlate() == null || dto.licensePlate().trim().isEmpty()) {
            throw new ValidationException("Kiểm chứng dữ liệu thất bại: 'licensePlate' không được phép để trống hoặc null!");
        }
        String formattedPlate = dto.licensePlate().toUpperCase().replaceAll("\\s+", "");
        if (!VN_LICENSE_PLATE_PATTERN.matcher(formattedPlate).matches()) {
            throw new ValidationException("Kiểm chứng dữ liệu thất bại: Biển số xe '" + dto.licensePlate() + "' không đúng định dạng Việt Nam!");
        }

        // Validate trường urgency
        if (dto.urgency() == null || dto.urgency().trim().isEmpty()) {
            throw new ValidationException("Kiểm chứng dữ liệu thất bại: Mức độ khẩn cấp 'urgency' không được trống!");
        }
        try {
            UrgencyLevel.valueOf(dto.urgency().toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Kiểm chứng dữ liệu thất bại: Mức độ khẩn cấp '" + dto.urgency() + "' không đúng giá trị Enum hợp lệ (LOW, MEDIUM, HIGH, CRITICAL)!");
        }

        // Validate trường incidentType
        if (dto.incidentType() == null || dto.incidentType().trim().isEmpty()) {
            throw new ValidationException("Kiểm chứng dữ liệu thất bại: Loại sự cố 'incidentType' không được phép để trống!");
        }

        log.info("[Validation] Dữ liệu DTO hợp lệ hoàn toàn. Tiến hành ánh xạ sang Entity.");
    }
}