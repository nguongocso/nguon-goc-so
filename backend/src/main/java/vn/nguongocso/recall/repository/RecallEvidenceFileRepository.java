package vn.nguongocso.recall.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.nguongocso.recall.entity.RecallEvidenceFile;

/**
 * Repository thao tác với bảng tệp biên bản thu hồi {@link RecallEvidenceFile}.
 */
@Repository
public interface RecallEvidenceFileRepository extends JpaRepository<RecallEvidenceFile, UUID> {

    /**
     * Tìm danh sách tệp biên bản theo danh sách UUID.
     *
     * @param ids Danh sách UUID tệp biên bản.
     * @return Danh sách thực thể {@link RecallEvidenceFile}.
     */
    List<RecallEvidenceFile> findByIdIn(List<UUID> ids);
}
