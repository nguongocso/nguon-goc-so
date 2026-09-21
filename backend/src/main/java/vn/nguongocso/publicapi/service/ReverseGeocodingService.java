package vn.nguongocso.publicapi.service;

/**
 * Service chuyển đổi tọa độ địa lý (vĩ độ, kinh độ) thành địa chỉ dạng văn bản.
 */
public interface ReverseGeocodingService {

    /**
     * Chuyển đổi tọa độ thành địa chỉ mô tả.
     *
     * @param latitude  vĩ độ
     * @param longitude kinh độ
     * @return địa chỉ văn bản hoặc null nếu không thể xác định
     */
    String reverseGeocode(double latitude, double longitude);
}
