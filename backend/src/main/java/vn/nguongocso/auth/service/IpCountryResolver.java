package vn.nguongocso.auth.service;

/**
 * Interface để giải quyết mã quốc gia từ địa chỉ IP công cộng của client
 * (NCL-01-
 * CN-011).
 */
public interface IpCountryResolver {
    /**
     * Giải quyết mã quốc gia từ địa chỉ IP công cộng của client.
     */
    String resolveCountryCode(String ipAddress);
}
