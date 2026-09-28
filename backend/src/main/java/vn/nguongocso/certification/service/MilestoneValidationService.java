package vn.nguongocso.certification.service;

import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.farm.entity.ProductionLot;

import java.util.List;

/**
 * Service kiểm tra việc hoàn thành mốc canh tác trước khi đóng gói lô sản xuất.
 * Story: NCL-09-CN-011
 */
public interface MilestoneValidationService {
        /**
         * Kiểm tra các mốc canh tác bắt buộc đã hoàn thành trong nhật ký canh tác chưa.
         * Trả về danh sách tên mốc còn thiếu (danh sách rỗng nếu đã hoàn thành đủ).
         */
        List<String> validateMilestoneCompletion(
                        ProductionLot lot);

        /**
         * Kiểm tra các mốc canh tác bắt buộc đã hoàn thành chưa và trả về danh sách thực thể mốc còn thiếu.
         */
        List<CultivationMilestone> findMissingMilestones(
                        ProductionLot lot);
}
