/** Kết quả kiểm tra địa chỉ webhook. */
export interface WebhookUrlCheck {
  valid: boolean;
  error: string | null;
}

/** Kiểm tra địa chỉ webhook phải là HTTPS hoặc localhost. */
export function validateWebhookUrl(url: string): WebhookUrlCheck {
  if (!url.trim()) {
    return { valid: true, error: null };
  }
  try {
    const parsed = new URL(url.trim());
    const isHttps = parsed.protocol === 'https:';
    const isLocalhost =
      parsed.protocol === 'http:' &&
      (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1');
    if (!isHttps && !isLocalhost) {
      return {
        valid: false,
        error: 'Địa chỉ Webhook bắt buộc phải sử dụng giao thức bảo mật HTTPS (https://).',
      };
    }
    return { valid: true, error: null };
  } catch {
    return {
      valid: false,
      error: 'Định dạng URL không hợp lệ (ví dụ: https://partner.example.com/webhooks).',
    };
  }
}
