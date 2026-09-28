package vn.nguongocso.event.service.recorder;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.event.service.EventHashService;

/** Lưu ChainEvent và tính chuỗi băm liên kết theo QTN-19 (NCL-08-CN-006). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainEventHashRecorder {
    private final ChainEventRepository chainEventRepository;
    private final EventHashService eventHashService;

    /** Lưu ChainEvent và tự động tính chuỗi băm liên kết trước khi persist. */
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
