import { useEffect, useState } from 'react';

/**
 * Hook that tracks whether a CSS media query matches.
 * Used for responsive layout decisions (mobile, tablet, desktop).
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia(query).matches;
  });

  useEffect(() => {
    const mql = window.matchMedia(query);
    const handler = (event: MediaQueryListEvent) => setMatches(event.matches);

    // Modern browsers
    mql.addEventListener('change', handler);

    // Initial check in case it changed between render and effect
    setMatches(mql.matches);

    return () => mql.removeEventListener('change', handler);
  }, [query]);

  return matches;
}