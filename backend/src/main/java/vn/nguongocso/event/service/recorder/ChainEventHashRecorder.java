package vn.nguongocso.event.service.recorder;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;

/**
 * Component chuyên trách lưu trữ ChainEvent và tính toán chuỗi băm mật mã liên kết
 * theo quy định QTN-19 và NCL-08-CN-006.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainEventHashRecorder {
    private final ChainEventRepository chainEventRepository;
    private final EventHashService eventHashService;

    /**
     * Lưu ChainEvent và tự động tính chuỗi băm liên kết trước khi persist.
     *
     * <p>Tìm sự kiện được ghi gần nhất (createdAt DESC) của cùng shipment để lấy previousHash.
     * Sự kiện đầu tiên dùng previousHash = "".</p>
     *
     * @param event sự kiện cần lưu
     * @return sự kiện đã được gán hash và lưu vào cơ sở dữ liệu
     */
    public ChainEvent saveWithChainHash(ChainEvent event) {
        if (event.getShipment() == null) {
            return chainEventRepository.save(event);
        }

        Optional<ChainEvent> lastEvent = chainEventRepository
                .findTopByShipmentIdOrderByCreatedAtDesc(event.getShipment().getId());

        String previousHash = "";
        if (lastEvent.isPresent()) {
            previousHash = lastEvent.get().getHash() != null ? lastEvent.get().getHash() : "";
        }

        event.setPreviousHash(previousHash.isEmpty() ? null : previousHash);
        event.setHash(eventHashService.calculateHash(event, previousHash));

        return chainEventRepository.save(event);
    }
}
