package vn.nguongocso.common.annotation;

import java.lang.annotation.*;

/**
 * Annotation Auditable được sử dụng để đánh dấu các phương thức cần được ghi
 * lại trong nhật ký (audit log).
 * Nó cung cấp thông tin về hành động, loại thực thể và mô tả liên quan đến
 * phương thức đó.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String action();

    String entityType() default "";

    String description();
}
