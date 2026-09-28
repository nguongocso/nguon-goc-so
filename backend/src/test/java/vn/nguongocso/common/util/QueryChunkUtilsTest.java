package vn.nguongocso.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QueryChunkUtilsTest {

    @Test
    @DisplayName("chunkList: 2500 phần tử với chunkSize=1000 chia đúng thành 3 chunk (1000, 1000, 500) và gộp lại đầy đủ")
    void chunkList_shouldSplit2500ElementsInto3Chunks() {
        List<UUID> originalList = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            originalList.add(UUID.randomUUID());
        }

        List<List<UUID>> chunks = QueryChunkUtils.chunkList(originalList, 1000);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(1000);
        assertThat(chunks.get(1)).hasSize(1000);
        assertThat(chunks.get(2)).hasSize(500);

        // Kết quả gộp khớp 100% danh sách ban đầu
        List<UUID> mergedList = new ArrayList<>();
        for (List<UUID> chunk : chunks) {
            mergedList.addAll(chunk);
        }

        assertThat(mergedList).isEqualTo(originalList);
    }

    @Test
    @DisplayName("chunkList: Danh sách rỗng hoặc null trả về danh sách rỗng an toàn")
    void chunkList_shouldHandleNullAndEmpty() {
        assertThat(QueryChunkUtils.chunkList(null)).isEmpty();
        assertThat(QueryChunkUtils.chunkList(Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("chunkCollection: Phân lô đúng khi truyền Collection Set")
    void chunkCollection_shouldHandleSet() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        List<List<Integer>> chunks = QueryChunkUtils.chunkCollection(list, 2);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).containsExactly(1, 2);
        assertThat(chunks.get(1)).containsExactly(3, 4);
        assertThat(chunks.get(2)).containsExactly(5);
    }
}
