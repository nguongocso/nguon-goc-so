package vn.nguongocso.auth.service;

/** Resolves a public client IP to an ISO 3166-1 alpha-2 country code. */
public interface IpCountryResolver {
    String resolveCountryCode(String ipAddress);
}
