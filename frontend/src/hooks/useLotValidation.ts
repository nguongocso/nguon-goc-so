import { useState, useEffect, useCallback } from 'react';
import { validateLot } from '@/api/eventValidationApi';
import type { LotValidationResponse } from '@/types/eventValidation';

export const useLotValidation = (lotId: string | null, eventType: string | null, debounceDelay = 500) => {
  const [validation, setValidation] = useState<LotValidationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validate = useCallback(async () => {
    if (!lotId || !eventType) {
      setValidation(null);
      setError(null);
      return;
    }
    try {
      setLoading(true);
      const result = await validateLot(lotId, eventType);
      setValidation(result);
      setError(null);
    } catch (err: any) {
      setValidation(null);
      setError(err.response?.data?.message || 'Không thể kiểm tra lô');
    } finally {
      setLoading(false);
    }
  }, [lotId, eventType]);

  useEffect(() => {
    const timer = setTimeout(() => {
      validate();
    }, debounceDelay);
    return () => clearTimeout(timer);
  }, [validate, debounceDelay]);

  return { validation, loading, error, validate };
};