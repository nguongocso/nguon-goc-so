import type { AxiosError } from "axios";

/**
 * Types hỗ trợ test interceptor.
 *
 * Axios InterceptorManager không export type handler công khai
 * (chỉ qua .use), nên define lại cho tests.
 */
export type FulfilledFn = (value: unknown) => unknown;

export type RejectedFn = (error: AxiosError) => unknown;