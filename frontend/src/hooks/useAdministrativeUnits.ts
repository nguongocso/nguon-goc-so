import { useCallback, useEffect, useState } from 'react';
import { getAdministrativeUnitTree } from '@/api/administrativeUnitApi';
import type { AdministrativeUnitNode } from '@/types/administrativeUnit';

interface UseAdministrativeUnitsResult {
  units: AdministrativeUnitNode[];
  flatById: Map<string, AdministrativeUnitNode>;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

/**
 * Nạp cây đơn vị hành chính (tỉnh → xã/phường) qua api module.
 * Tái sử dụng cho màn hình gán địa bàn và bộ lọc báo cáo.
 */
export function useAdministrativeUnits(): UseAdministrativeUnitsResult {
  const [units, setUnits] = useState<AdministrativeUnitNode[]>([]);
  const [flatById, setFlatById] = useState<Map<string, AdministrativeUnitNode>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const tree = await getAdministrativeUnitTree();
        if (cancelled) return;
        setUnits(tree);
        setFlatById(flatten(tree));
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Không thể tải danh mục địa bàn.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

  const reload = useCallback(() => setReloadToken((token) => token + 1), []);

  return { units, flatById, loading, error, reload };
}

function flatten(nodes: AdministrativeUnitNode[]): Map<string, AdministrativeUnitNode> {
  const map = new Map<string, AdministrativeUnitNode>();
  for (const node of nodes) {
    map.set(node.id, node);
    for (const child of node.children) {
      map.set(child.id, child);
    }
  }
  return map;
}
