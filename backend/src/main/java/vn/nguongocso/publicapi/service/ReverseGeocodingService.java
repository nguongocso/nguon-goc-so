package vn.nguongocso.publicapi.service;

/** Dịch vụ dịch tọa độ địa lý sang địa chỉ chuỗi. */
public interface ReverseGeocodingService {
    /** Chuyển đổi tọa độ vĩ độ và kinh độ thành chuỗi địa chỉ địa lý. */
    String reverseGeocode(double latitude, double longitude);
}
