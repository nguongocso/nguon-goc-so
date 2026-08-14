import logoImg from '@/assets/logo.png';
import { cn } from '@/lib/utils';

interface LogoProps {
  /** Height in pixels (width auto-scaled to maintain ratio) */
  height?: number;
  /** Additional classes for the image */
  className?: string;
  /** Whether to show the brand name text next to the logo */
  showText?: boolean;
  /** Additional classes for the text span */
  textClassName?: string;
}

export function Logo({
  height = 36,
  className,
}: LogoProps) {
  return (
    <span className="inline-flex items-center gap-3">
      <img
        src={logoImg}
        alt="Nguồn Gốc Số"
        height={height}
        className={cn('object-contain', className)}
        style={{ height, width: 'auto' }}
      />
    </span>
  );
}