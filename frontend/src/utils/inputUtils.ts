import type { FocusEvent, MouseEvent } from "react";

export const selectAllOnFocus = (e: FocusEvent<HTMLInputElement>) => {
  e.target.select();
};

export const preventMouseUpCollapse = (e: MouseEvent<HTMLInputElement>) => {
  e.preventDefault();
};
