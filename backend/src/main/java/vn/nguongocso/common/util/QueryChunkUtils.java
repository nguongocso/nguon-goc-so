package vn.nguongocso.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tiện ích phân lô danh sách tham số phục vụ cho các mệnh đề SQL IN (:ids)
 * nhằm tránh lỗi tràn giới hạn tham số (Parameter Overflow) và PacketTooBigException của JDBC/MySQL.
 */
public final class QueryChunkUtils {

    /** Kích thước phân lô mặc định an toàn cho các câu lệnh SQL IN (1.000 phần tử). */
    public static final int DEFAULT_CHUNK_SIZE = 1000;

    private QueryChunkUtils() {
        // Utility class, không khởi tạo trực tiếp
    }

    /**
     * Phân chia một danh sách thành các danh sách con có kích thước tối đa là chunkSize.
     *
     * @param list Danh sách gốc cần chia
     * @param chunkSize Kích thước tối đa của mỗi lô (phải > 0)
     * @param <T> Kiểu phần tử
     * @return Danh sách các lô con (không null)
     */
    public static <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        int effectiveChunkSize = chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        List<List<T>> chunks = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += effectiveChunkSize) {
            chunks.add(list.subList(i, Math.min(size, i + effectiveChunkSize)));
        }
        return chunks;
    }

    /**
     * Phân chia một danh sách với kích thước phân lô mặc định 1.000 phần tử.
     */
    public static <T> List<List<T>> chunkList(List<T> list) {
        return chunkList(list, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Phân chia một Collection thành các danh sách con.
     */
    public static <T> List<List<T>> chunkCollection(Collection<T> collection, int chunkSize) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> list = collection instanceof List<T> l ? l : new ArrayList<>(collection);
        return chunkList(list, chunkSize);
    }

    /**
     * Phân chia một Collection với kích thước phân lô mặc định 1.000 phần tử.
     */
    public static <T> List<List<T>> chunkCollection(Collection<T> collection) {
        return chunkCollection(collection, DEFAULT_CHUNK_SIZE);
    }
}
