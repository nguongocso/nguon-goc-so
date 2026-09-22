package vn.nguongocso.recall.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.recall.entity.RecallEvidenceFile;

/** Repository quản lý tệp biên bản thu hồi. */
@Repository
public interface RecallEvidenceFileRepository extends JpaRepository<RecallEvidenceFile, UUID> {
    /** Tìm danh sách tệp biên bản theo danh sách ID. */
    List<RecallEvidenceFile> findByIdIn(List<UUID> ids);
}
