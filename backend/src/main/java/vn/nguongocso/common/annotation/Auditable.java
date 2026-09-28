package vn.nguongocso.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Đánh dấu phương thức cần ghi nhật ký lưu vết (Audit Log). */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String action();

    String entityType() default "";

    String description();

    /** Biểu thức SpEL lấy giá trị trước thay đổi; để trống khi không áp dụng. */
    String beforeValue() default "";

    /** Biểu thức SpEL lấy giá trị sau thay đổi; để trống khi không áp dụng. */
    String afterValue() default "";
}
