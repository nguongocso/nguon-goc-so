import { useState } from 'react';
import { LoaderCircle, ShieldCheck, ShieldAlert, CheckCircle2, XCircle, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { verifyChainIntegrity } from '@/api/eventChainVerificationApi';
import { HelpButton } from '@/components/help/HelpButton';
import type { ChainVerificationResponse } from '@/types/eventChainVerification';

export default function EventChainVerificationPage() {
  const [shipmentId, setShipmentId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ChainVerificationResponse | null>(null);

  const handleVerify = async () => {
    if (!shipmentId.trim()) {
      setError('Vui lòng nhập mã lô hàng.');
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await verifyChainIntegrity(shipmentId.trim());
      setResult(res);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Không thể kiểm chứng dòng sự kiện.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Kiểm chứng tính toàn vẹn dòng sự kiện</h1>
          <p className="mt-1 text-sm text-slate-500">
            Tính lại chuỗi băm liên kết của các sự kiện để xác minh dòng sự kiện lô hàng không bị can thiệp.
          </p>
        </div>
        <HelpButton screenKey="event-chain-verification" />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ShieldCheck className="size-5 text-emerald-600" />
            Kiểm chứng lô hàng
          </CardTitle>
          <CardDescription>Nhập mã lô hàng (Shipment ID) và bấm kiểm chứng.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-end gap-2">
            <div className="flex-1 space-y-2">
              <Label htmlFor="shipmentId">Mã lô hàng (Shipment ID)</Label>
              <Input
                id="shipmentId"
                value={shipmentId}
                onChange={(e) => setShipmentId(e.target.value)}
                placeholder="9c8b7a6f-2222-4a2a-9f3d-1a2b3c4d5e6f"
              />
            </div>
            <Button variant="view" onClick={handleVerify} disabled={loading}>
              {loading ? <LoaderCircle className="size-4 animate-spin" /> : <ShieldCheck className="size-4" />}
              {loading ? 'Đang kiểm chứng...' : 'Kiểm chứng'}
            </Button>
          </div>

          {error && (
            <Alert variant="destructive" className="mt-4">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      {result && (
        <Card className={result.isIntegrityVerified ? 'border-emerald-200' : 'border-red-200'}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-lg">
              {result.isIntegrityVerified ? (
                <CheckCircle2 className="size-6 text-emerald-600" />
              ) : (
                <ShieldAlert className="size-6 text-red-600" />
              )}
              {result.isIntegrityVerified ? 'Dòng sự kiện còn nguyên vẹn' : 'Dòng sự kiện bị can thiệp'}
            </CardTitle>
            <CardDescription>
              {result.shipmentName} • {result.hashAlgorithm} • {new Date(result.verifiedAt).toLocaleString('vi-VN')}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap gap-2">
              <Badge variant={result.isIntegrityVerified ? 'outline' : 'destructive'}>
                {result.verificationStatus}
              </Badge>
              <Badge variant="outline">{result.totalEvents} sự kiện</Badge>
            </div>

            {!result.isIntegrityVerified && result.failedEventIndex != null && (
              <Alert variant="destructive">
                <AlertDescription>
                  Sự kiện đầu tiên bị lệch: sự kiện #{result.failedEventIndex} (ID: {result.failedEventId})
                  {result.failureReason ? ` — ${result.failureReason}` : ''}
                </AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              {result.events.map((ev) => (
                <div
                  key={ev.eventId}
                  className={`flex items-start gap-3 rounded-lg border p-3 ${
                    ev.isValid
                      ? 'border-emerald-200 bg-emerald-50'
                      : 'border-red-200 bg-red-50'
                  }`}
                >
                  {ev.isValid ? (
                    <CheckCircle2 className="mt-0.5 size-4 text-emerald-600" />
                  ) : (
                    <XCircle className="mt-0.5 size-4 text-red-600" />
                  )}
                  <div className="flex-1 text-sm">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-semibold">Sự kiện #{ev.index}</span>
                      <Badge variant="outline">{ev.eventType}</Badge>
                      {!ev.isValid && <Badge variant="destructive">Không hợp lệ</Badge>}
                    </div>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">
                      Hash: {ev.hash || '—'}
                    </p>
                    {ev.expectedHash && (
                      <p className="mt-1 font-mono text-xs text-red-700">
                        Expected: {ev.expectedHash}
                      </p>
                    )}
                  </div>
                  <Eye className="size-4 text-muted-foreground" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}