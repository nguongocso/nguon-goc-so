import { useState, useEffect, useRef, useCallback } from 'react';
import {
  Mic,
  MicOff,
  Volume2,
  VolumeX,
  Sparkles,
  CheckCircle2,
  ArrowRight,
  AlertCircle,
  Loader2,
  RotateCcw,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { parseFarmLogVoice } from '@/api/aiChatApi';
import type { AiFarmLogParseResponse } from '@/types/aiChat';
import type { ProductionLot } from '@/types/productionLot';
import type { VatTuCache } from '@/lib/offline/farmLogDb';
import type { FarmActivityType } from '@/types/farmLog';

// Web Speech API interface
interface IWindow extends Window {
  SpeechRecognition?: any;
  webkitSpeechRecognition?: any;
}

interface VoiceFarmLogAssistantProps {
  productionLots: ProductionLot[];
  danhSachVatTu?: VatTuCache[];
  onApplyParsedData: (data: {
    productionLotId?: string;
    activityType: FarmActivityType;
    material?: string;
    quantity?: string;
    unit?: string;
    executedDate?: string;
    notes?: string;
  }) => void;
  isOnline?: boolean;
}

/**
 * Trợ lý nhập liệu nhật ký canh tác bằng Giọng nói (Speech-to-Text & Text-to-Speech)
 */
export function VoiceFarmLogAssistant({
  productionLots,
  danhSachVatTu = [],
  onApplyParsedData,
  isOnline = true,
}: VoiceFarmLogAssistantProps) {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [parsedResult, setParsedResult] = useState<AiFarmLogParseResponse | null>(null);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [speechSupported, setSpeechSupported] = useState(true);

  const recognitionRef = useRef<any>(null);
  const silenceTimerRef = useRef<any>(null);
  const isListeningRef = useRef(false);
  const transcriptRef = useRef('');
  const handleParseTextRef = useRef<((text: string) => void) | null>(null);

  // Đồng bộ transcriptRef và handleParseTextRef
  useEffect(() => {
    transcriptRef.current = transcript;
  }, [transcript]);

  // Dừng hoàn toàn nghe âm thanh và hủy phiên micro ngay lập tức
  const stopListening = useCallback(() => {
    isListeningRef.current = false;
    setIsListening(false);
    if (silenceTimerRef.current) {
      clearTimeout(silenceTimerRef.current);
      silenceTimerRef.current = null;
    }
    if (recognitionRef.current) {
      try {
        recognitionRef.current.abort();
      } catch {}
    }
  }, []);

  // Dừng phát âm thanh loa đọc
  const stopSpeaking = useCallback(() => {
    if (window.speechSynthesis) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
    }
  }, []);

  // Phát âm thanh đọc câu tóm tắt (Text-to-Speech) một lần duy nhất
  const speakSummary = useCallback((text: string) => {
    if (!window.speechSynthesis) return;

    // Đảm bảo micro đã tắt và hủy hoàn toàn trước khi phát âm thanh ra loa
    stopListening();
    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'vi-VN';
    utterance.rate = 0.95;

    // Cố gắng chọn voice tiếng Việt nếu có
    const voices = window.speechSynthesis.getVoices();
    const viVoice = voices.find((v) => v.lang.startsWith('vi') || v.lang === 'vi-VN');
    if (viVoice) utterance.voice = viVoice;

    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);

    window.speechSynthesis.speak(utterance);
  }, [stopListening]);

  // Phân tích văn bản thành thực thể form
  const handleParseText = useCallback(
    async (textToParse: string) => {
      // Dừng nghe ngay lập tức trước khi phân tích
      stopListening();
      stopSpeaking();

      const cleanText = textToParse.trim();
      if (!cleanText) return;

      setIsProcessing(true);
      try {
        const availableLots = productionLots.map((l) => ({ id: l.id, name: l.name }));
        const availableMaterials = danhSachVatTu.map((v) => ({
          id: v.id,
          name: v.ten,
          unit: v.donVi,
        }));

        let result: AiFarmLogParseResponse;

        // Nếu online: gọi AI Backend
        if (isOnline) {
          result = await parseFarmLogVoice({
            voiceText: cleanText,
            availableLots,
            availableMaterials,
          });
        } else {
          // Ngoại tuyến: bộ phân tích thông minh cục bộ
          result = parseLocally(cleanText, availableLots, availableMaterials);
        }

        setParsedResult(result);

        // Đọc xác nhận lại bằng Text-to-Speech (TTS) đúng 1 lần
        if (result.summaryText) {
          speakSummary(result.summaryText);
        }
      } catch (error) {
        console.warn('Lỗi phân tích qua backend, dùng bộ phân tích dự phòng cục bộ:', error);
        const availableLots = productionLots.map((l) => ({ id: l.id, name: l.name }));
        const availableMaterials = danhSachVatTu.map((v) => ({
          id: v.id,
          name: v.ten,
          unit: v.donVi,
        }));
        const localResult = parseLocally(cleanText, availableLots, availableMaterials);
        setParsedResult(localResult);
        if (localResult.summaryText) {
          speakSummary(localResult.summaryText);
        }
      } finally {
        setIsProcessing(false);
      }
    },
    [productionLots, danhSachVatTu, isOnline, stopListening, stopSpeaking, speakSummary],
  );

  useEffect(() => {
    handleParseTextRef.current = handleParseText;
  }, [handleParseText]);

  const stopListeningAndParse = useCallback(
    (explicitText?: string) => {
      stopListening();
      const targetText = explicitText !== undefined ? explicitText : transcriptRef.current;
      if (targetText && targetText.trim()) {
        if (handleParseTextRef.current) {
          void handleParseTextRef.current(targetText.trim());
        }
      }
    },
    [stopListening],
  );

  // Khởi tạo nhận diện giọng nói Web Speech API một lần duy nhất lúc mount
  useEffect(() => {
    const customWindow = window as unknown as IWindow;
    const SpeechRecognition = customWindow.SpeechRecognition || customWindow.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setSpeechSupported(false);
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.lang = 'vi-VN';
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onresult = (event: any) => {
        // Chỉ xử lý khi đang trong trạng thái chủ động nghe, chặn micro nghe lại tiếng loa
        if (!isListeningRef.current) return;

        // Xử lý chống lặp giọng nói đặc thù trên Mobile Chrome (Android/iOS):
        // Trên điện thoại, SpeechRecognition thường trả về các bản nháp trung gian (interim)
        // hoặc các câu nối tiếp nhau trong event.results mà câu sau mở rộng/sửa đổi câu trước.
        const pieces: string[] = [];
        for (let i = 0; i < event.results.length; i++) {
          const raw = event.results[i]?.[0]?.transcript?.trim();
          if (raw) {
            pieces.push(raw);
          }
        }

        // Lọc bỏ các bản nháp trung gian:
        // Nếu một đoạn text ở vị trí i là tiền tố hoặc nằm trọn trong bất kỳ đoạn text nào ở vị trí sau j (j > i),
        // thì đoạn i chỉ là kết quả nhận diện dở dang đang được hoàn thiện, ta loại bỏ đoạn i.
        const cleanPieces: string[] = [];
        for (let i = 0; i < pieces.length; i++) {
          const current = pieces[i];
          const curNorm = current.toLowerCase().replace(/[,.?!]/g, '').trim();

          let isDraftOfLater = false;
          for (let j = i + 1; j < pieces.length; j++) {
            const later = pieces[j];
            const laterNorm = later.toLowerCase().replace(/[,.?!]/g, '').trim();
            if (laterNorm.startsWith(curNorm) || laterNorm.includes(curNorm)) {
              isDraftOfLater = true;
              break;
            }
          }

          if (!isDraftOfLater) {
            cleanPieces.push(current);
          }
        }

        const text = cleanPieces.join(' ').trim();
        if (text) {
          transcriptRef.current = text;
          setTranscript(text);
        }

        // Reset bộ đếm im lặng: cho người dùng khoảng 3.5 giây dứt câu trước khi tự động dừng và phân tích
        if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);
        silenceTimerRef.current = setTimeout(() => {
          if (isListeningRef.current && transcriptRef.current.trim()) {
            stopListening();
            if (handleParseTextRef.current) {
              void handleParseTextRef.current(transcriptRef.current.trim());
            }
          }
        }, 3500);
      };

      recognition.onerror = (event: any) => {
        // Lỗi 'no-speech' không phải lỗi nghiêm trọng (người dùng tạm nghỉ 1 nhịp), tiếp tục lắng nghe
        if (event.error === 'no-speech') {
          return;
        }

        if (event.error === 'not-allowed' || event.error === 'audio-capture') {
          toast.error('Trình duyệt chưa cho phép truy cập micro. Hãy kiểm tra cài đặt quyền.');
          isListeningRef.current = false;
          setIsListening(false);
        } else {
          console.warn('SpeechRecognition error:', event.error);
        }
      };

      recognition.onend = () => {
        // Nếu người dùng chưa bấm dừng mà Chrome tự ngắt phiên do im lặng ngắn, tự động khởi động lại phiên nghe
        if (isListeningRef.current) {
          try {
            recognition.start();
          } catch {
            setTimeout(() => {
              if (isListeningRef.current && recognitionRef.current) {
                try {
                  recognitionRef.current.start();
                } catch {}
              }
            }, 150);
          }
        } else {
          setIsListening(false);
        }
      };

      recognitionRef.current = recognition;
    } catch (e) {
      console.warn('Không thể khởi tạo SpeechRecognition:', e);
      setSpeechSupported(false);
    }

    return () => {
      isListeningRef.current = false;
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort();
        } catch {}
      }
      if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);
      if (window.speechSynthesis) window.speechSynthesis.cancel();
    };
  }, [stopListening]);

  // Bắt đầu nghe
  const startListening = () => {
    stopSpeaking();
    setParsedResult(null);
    setTranscript('');
    transcriptRef.current = '';
    isListeningRef.current = true;
    setIsListening(true);

    if (recognitionRef.current) {
      try {
        recognitionRef.current.start();
      } catch {
        try {
          recognitionRef.current.abort();
          setTimeout(() => {
            if (isListeningRef.current && recognitionRef.current) {
              try {
                recognitionRef.current.start();
              } catch {}
            }
          }, 150);
        } catch {}
      }
    } else {
      isListeningRef.current = false;
      setIsListening(false);
      toast.info('Trình duyệt chưa bật quyền Micro hoặc không hỗ trợ Web Speech API.');
    }
  };

  // Áp dụng dữ liệu trích xuất vào form cha
  const handleApply = () => {
    if (!parsedResult) return;

    onApplyParsedData({
      productionLotId: parsedResult.productionLotId,
      activityType: parsedResult.activityType,
      material: parsedResult.material,
      quantity: parsedResult.quantity !== undefined && parsedResult.quantity !== null
        ? String(parsedResult.quantity)
        : '',
      unit: parsedResult.unit || '',
      executedDate: parsedResult.executedDate || undefined,
      notes: parsedResult.notes || parsedResult.rawVoiceText || '',
    });

    toast.success('Đã áp dụng thông tin giọng nói vào biểu mẫu!');
    stopSpeaking();
  };

  // Mẫu câu ví dụ để click test nhanh
  const samplePrompts = [
    'Sáng nay bón 20 cân phân hữu cơ vi sinh cho Lô Nho 01, trời râm mát',
    'Tưới nước nhỏ giọt 50 lít cho Lô Nho 01',
    'Phun 2 bình thuốc trừ sâu sinh học cho Lô Nho 01',
    'Hôm qua làm cỏ và xới gốc cho Lô Nho 01',
  ];

  return (
    <Card className="border-emerald-200 bg-gradient-to-br from-emerald-50/70 via-teal-50/40 to-white shadow-sm transition-all hover:border-emerald-300">
      <CardContent className="p-4 sm:p-5">
        {/* Tiêu đề */}
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex size-9 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-sm ring-4 ring-emerald-100">
              <Sparkles className="size-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-emerald-950 sm:text-lg">
                Trợ lý nhập liệu nhật ký canh tác bằng Giọng nói
              </h3>
            </div>
          </div>

          {/* Trạng thái âm thanh TTS */}
          {isSpeaking && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={stopSpeaking}
              className="border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100"
            >
              <VolumeX className="mr-1.5 size-4 animate-pulse text-amber-600" />
              Đang đọc (Bấm để tắt)
            </Button>
          )}
        </div>

        {/* Khu vực nút Micro và tương tác */}
        <div className="mt-4 flex flex-col items-center justify-center rounded-2xl border border-emerald-100 bg-white/80 p-4 text-center backdrop-blur-sm sm:p-6">
          <div className="relative">
            {/* Vòng sóng âm hiệu ứng khi đang lắng nghe */}
            {isListening && (
              <>
                <div className="absolute -inset-3 animate-ping rounded-full bg-emerald-400/30" />
                <div className="absolute -inset-6 animate-pulse rounded-full bg-emerald-500/20" />
              </>
            )}

            <button
              type="button"
              onClick={isListening ? () => stopListeningAndParse() : startListening}
              disabled={isProcessing}
              className={`relative flex size-20 items-center justify-center rounded-full shadow-lg transition-all duration-300 focus:outline-none focus:ring-4 focus:ring-emerald-300 active:scale-95 sm:size-24 ${
                isListening
                  ? 'bg-rose-500 text-white shadow-rose-200 animate-pulse'
                  : 'bg-emerald-600 text-white shadow-emerald-200 hover:bg-emerald-700 hover:scale-105'
              }`}
              title={isListening ? 'Bấm để dừng và phân tích' : 'Bấm để nói'}
            >
              {isProcessing ? (
                <Loader2 className="size-10 animate-spin" />
              ) : isListening ? (
                <MicOff className="size-10 animate-bounce" />
              ) : (
                <Mic className="size-10" />
              )}
            </button>
          </div>

          <div className="mt-3">
            <span
              className={`inline-block font-bold text-sm sm:text-base ${
                isListening ? 'text-rose-600 animate-pulse' : 'text-emerald-900'
              }`}
            >
              {isProcessing
                ? 'Đang phân tích thông tin giọng nói...'
                : isListening
                ? 'Đang lắng nghe... (Bấm lại vào micro để phân tích)'
                : 'Chạm vào Micro để nói'}
            </span>
          </div>

          {/* Dòng chữ hiển thị những gì máy vừa nghe được */}
          {transcript && (
            <div className="mt-3 w-full max-w-xl rounded-xl border border-slate-200 bg-slate-50 p-3 text-left">
              <span className="text-xs font-semibold uppercase text-slate-500">Lời bạn vừa nói:</span>
              <p className="mt-1 text-sm font-medium text-slate-800 italic">
                "{transcript}"
              </p>
            </div>
          )}

          {/* Các câu mẫu gợi ý để thử nhanh */}
          <div className="mt-4 flex flex-wrap items-center justify-center gap-1.5 text-xs">
            <span className="text-slate-500">Hoặc bấm thử câu mẫu:</span>
            {samplePrompts.map((prompt, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => {
                  setTranscript(prompt);
                  void handleParseText(prompt);
                }}
                disabled={isProcessing || isListening}
                className="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-slate-700 shadow-2xs transition hover:border-emerald-500 hover:bg-emerald-50 hover:text-emerald-800 disabled:opacity-50"
              >
                {prompt.length > 35 ? prompt.slice(0, 35) + '...' : prompt}
              </button>
            ))}
          </div>

          {!speechSupported && (
            <div className="mt-2 flex items-center gap-1.5 text-xs text-amber-700">
              <AlertCircle className="size-3.5" />
              <span>Trình duyệt này không hỗ trợ micro trực tiếp, bạn có thể bấm câu mẫu ở trên để thử nghiệm nhé!</span>
            </div>
          )}
        </div>

        {/* Kết quả bóc tách thành công & Nút áp dụng vào form */}
        {parsedResult && (
          <div className="mt-4 animate-in fade-in slide-in-from-top-3 duration-300 rounded-2xl border-2 border-emerald-400 bg-white p-4 shadow-md sm:p-5">
            <div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="size-5 text-emerald-600 shrink-0" />
                <span className="font-bold text-slate-900 text-sm sm:text-base">
                  AI đã bóc tách thành công thông tin:
                </span>
              </div>

              {/* Câu đọc tóm tắt (TTS) */}
              {parsedResult.summaryText && (
                <div className="mt-2 flex items-center gap-2 rounded-xl bg-emerald-50 p-2.5 text-sm text-emerald-900">
                  <Volume2 className="size-5 shrink-0 text-emerald-700" />
                  <span className="font-medium">{parsedResult.summaryText}</span>
                  <button
                    type="button"
                    onClick={() => speakSummary(parsedResult.summaryText)}
                    className="ml-auto shrink-0 text-xs font-bold text-emerald-700 underline hover:text-emerald-900 cursor-pointer"
                  >
                    Nghe lại
                  </button>
                </div>
              )}

              {/* Các trường dữ liệu sẽ đổ vào form (gồm cả Ngày thực hiện) */}
              <div className="mt-3 grid grid-cols-2 gap-2 text-xs sm:grid-cols-5 sm:text-sm">
                <div className="rounded-lg bg-slate-50 p-2 border border-slate-100">
                  <span className="text-slate-500 block text-xs">Hoạt động:</span>
                  <strong className="text-emerald-800 font-semibold">{parsedResult.activityLabel}</strong>
                </div>
                <div className="rounded-lg bg-slate-50 p-2 border border-slate-100">
                  <span className="text-slate-500 block text-xs">Vật tư:</span>
                  <strong className="text-slate-800 font-semibold">{parsedResult.material || 'Không dùng'}</strong>
                </div>
                <div className="rounded-lg bg-slate-50 p-2 border border-slate-100">
                  <span className="text-slate-500 block text-xs">Số lượng:</span>
                  <strong className="text-slate-800 font-semibold">
                    {parsedResult.quantity !== undefined && parsedResult.quantity !== null
                      ? `${parsedResult.quantity} ${parsedResult.unit || ''}`
                      : '—'}
                  </strong>
                </div>
                <div className="rounded-lg bg-slate-50 p-2 border border-slate-100">
                  <span className="text-slate-500 block text-xs">Ngày thực hiện:</span>
                  <strong className="text-blue-700 font-semibold">
                    {parsedResult.executedDate || new Date().toISOString().slice(0, 10)}
                  </strong>
                </div>
                <div className="rounded-lg bg-slate-50 p-2 border border-slate-100 col-span-2 sm:col-span-1">
                  <span className="text-slate-500 block text-xs">Lô sản xuất:</span>
                  <strong className="text-emerald-700 font-semibold truncate block">
                    {parsedResult.productionLotName || 'Chưa rõ (chọn tay)'}
                  </strong>
                </div>
              </div>

              {/* Nút hành động áp dụng */}
              <div className="mt-4 pt-3.5 border-t border-emerald-100 flex flex-col sm:flex-row items-stretch sm:items-center justify-end gap-2.5">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setParsedResult(null);
                    setTranscript('');
                    stopSpeaking();
                  }}
                  className="w-full sm:w-auto text-slate-700 border-slate-300 hover:bg-slate-100 order-2 sm:order-1 font-medium"
                >
                  <RotateCcw className="mr-1.5 size-4" />
                  Nói lại câu khác
                </Button>
                <Button
                  type="button"
                  onClick={handleApply}
                  className="w-full sm:w-auto bg-emerald-600 font-bold hover:bg-emerald-700 text-white shadow-sm order-1 sm:order-2"
                >
                  <CheckCircle2 className="mr-1.5 size-4" />
                  Áp dụng vào Form
                  <ArrowRight className="ml-1.5 size-4" />
                </Button>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * Bộ phân tích tiếng Việt dự phòng chạy trên máy khách (Offline-safe fallback).
 */
function parseLocally(
  text: string,
  lots: Array<{ id: string; name: string }>,
  materials: Array<{ id?: string | number; name: string; unit?: string }>,
): AiFarmLogParseResponse {
  const lower = text.toLowerCase();

  let activityType: FarmActivityType = 'OTHER';
  let activityLabel = 'Khác';

  if (
    lower.includes('bón') ||
    lower.includes('phân') ||
    lower.includes('đạm') ||
    lower.includes('hữu cơ') ||
    lower.includes('vi sinh')
  ) {
    activityType = 'FERTILIZING';
    activityLabel = 'Bón phân';
  } else if (
    lower.includes('tưới') ||
    lower.includes('nước') ||
    lower.includes('nhỏ giọt')
  ) {
    activityType = 'WATERING';
    activityLabel = 'Tưới nước';
  } else if (
    lower.includes('phun') ||
    lower.includes('xịt') ||
    lower.includes('thuốc') ||
    lower.includes('trừ sâu') ||
    lower.includes('sinh học')
  ) {
    activityType = 'PESTICIDE';
    activityLabel = 'Phun thuốc';
  } else if (lower.includes('cỏ') || lower.includes('làm cỏ') || lower.includes('nhổ cỏ')) {
    activityType = 'WEEDING';
    activityLabel = 'Làm cỏ';
  } else if (lower.includes('thu hoạch') || lower.includes('hái') || lower.includes('cắt')) {
    activityType = 'HARVESTING';
    activityLabel = 'Thu hoạch';
  } else if (lower.includes('gieo') || lower.includes('trồng') || lower.includes('xuống giống')) {
    activityType = 'PLANTING';
    activityLabel = 'Gieo trồng';
  }

  // Khớp số và đơn vị
  const normalized = lower
    .replace('hai mươi lăm', '25')
    .replace('hai mươi', '20')
    .replace('mười lăm', '15')
    .replace('ba mươi', '30')
    .replace('năm mươi', '50')
    .replace('mười', '10')
    .replace('nửa', '0.5');

  const match = normalized.match(/(\d+(?:[.,]\d+)?)\s*(kg|kí|ký|cân|lít|lit|l|bao|bình|chai|gói)?/i);
  let quantity: number | undefined = undefined;
  let unit = '';

  if (match) {
    quantity = parseFloat(match[1].replace(',', '.'));
    const rawUnit = (match[2] || '').toLowerCase();
    if (rawUnit.includes('cân') || rawUnit.includes('kí') || rawUnit.includes('kg')) unit = 'kg';
    else if (rawUnit.includes('lít') || rawUnit.includes('lit') || rawUnit === 'l') unit = 'lít';
    else if (rawUnit.includes('bình')) unit = 'bình';
    else if (rawUnit.includes('bao')) unit = 'bao';
    else if (rawUnit.includes('chai')) unit = 'chai';
  }

  if (!unit && quantity !== undefined) {
    if (activityType === 'FERTILIZING') unit = 'kg';
    else if (activityType === 'WATERING') unit = 'lít';
    else if (activityType === 'PESTICIDE') unit = 'bình';
  }

  // Khớp ngày thực hiện
  let executedDate = new Date().toISOString().slice(0, 10);
  if (lower.includes('hôm qua') || lower.includes('hôm trc') || lower.includes('hôm trước')) {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    executedDate = yesterday.toISOString().slice(0, 10);
  } else {
    const dateMatch1 = lower.match(/(?:ngày\s*)?(\d{1,2})[\/\-](\d{1,2})(?:[\/\-](\d{4}))?/);
    const dateMatch2 = lower.match(/ngày\s*(\d{1,2})\s*tháng\s*(\d{1,2})/);
    if (dateMatch1) {
      const day = parseInt(dateMatch1[1], 10);
      const month = parseInt(dateMatch1[2], 10);
      const year = dateMatch1[3] ? parseInt(dateMatch1[3], 10) : new Date().getFullYear();
      if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
        executedDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      }
    } else if (dateMatch2) {
      const day = parseInt(dateMatch2[1], 10);
      const month = parseInt(dateMatch2[2], 10);
      const year = new Date().getFullYear();
      if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
        executedDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      }
    }
  }

  // Khớp lô
  let matchedLotId: string | undefined = undefined;
  let matchedLotName: string | undefined = undefined;
  for (const lot of lots) {
    const lotLower = lot.name.toLowerCase();
    if (lower.includes(lotLower) || (lotLower.includes('01') && (lower.includes('1') || lower.includes('01')))) {
      matchedLotId = lot.id;
      matchedLotName = lot.name;
      break;
    }
  }

  // Khớp vật tư
  let materialName = '';
  for (const mat of materials) {
    if (lower.includes(mat.name.toLowerCase())) {
      materialName = mat.name;
      if (!unit && mat.unit) unit = mat.unit;
      break;
    }
  }

  if (!materialName) {
    if (lower.includes('hữu cơ')) materialName = 'Phân hữu cơ vi sinh';
    else if (lower.includes('trừ sâu')) materialName = 'Thuốc trừ sâu sinh học';
    else if (lower.includes('nước')) materialName = 'Nước tưới sạch';
  }

  let dateTextDesc = '';
  const yesterdayStr = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
  if (executedDate === yesterdayStr) {
    dateTextDesc = ' ngày hôm qua';
  } else if (executedDate !== new Date().toISOString().slice(0, 10)) {
    dateTextDesc = ` ngày ${executedDate.slice(8, 10)}/${executedDate.slice(5, 7)}`;
  }

  const summary = `Đã ghi nhận: ${activityLabel}${materialName ? ` ${materialName}` : ''}${
    quantity ? `, số lượng ${quantity} ${unit}` : ''
  }${matchedLotName ? ` cho ${matchedLotName}` : ''}${dateTextDesc}. Bác bấm Lưu nhé!`;

  return {
    activityType,
    activityLabel,
    material: materialName,
    quantity,
    unit,
    productionLotId: matchedLotId,
    productionLotName: matchedLotName,
    executedDate,
    notes: text,
    summaryText: summary,
    rawVoiceText: text,
    confidence: 0.9,
  };
}
