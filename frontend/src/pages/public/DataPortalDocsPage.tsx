import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import {
  Code2,
  Copy,
  Check,
  ShieldCheck,
  Server,
  Layers,
  ArrowRight,
  Home,
  LogIn,
  AlertTriangle,
  Key,
  SlidersHorizontal,
  FileCode,
  BookOpen,
  Info,
} from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { useAuth } from '@/hooks/useAuth';

type TargetEnvironment = 'production' | 'staging' | 'localhost';
type SnippetTab = 'curl' | 'fetch' | 'python';

const ENV_URLS: Record<TargetEnvironment, string> = {
  production: 'https://agri-trace.online',
  staging: 'https://staging.agri-trace.online',
  localhost: 'http://localhost:8080',
};

const getSampleCurl = (env: TargetEnvironment, key: string, path: string) => `curl -s -X GET "${ENV_URLS[env]}${path}" \\
  -H "Accept: application/json" \\
  -H "X-API-KEY: ${key}" | jq .`;

const getSampleFetch = (env: TargetEnvironment, key: string, path: string) => `fetch("${ENV_URLS[env]}${path}", {
  method: "GET",
  headers: {
    "Accept": "application/json",
    "X-API-KEY": "${key}"
  }
})
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error("Lỗi:", error));`;

const getSamplePython = (env: TargetEnvironment, key: string, path: string) => `import requests

url = "${ENV_URLS[env]}${path}"
headers = {
    "Accept": "application/json",
    "X-API-KEY": "${key}"
}

response = requests.get(url, headers=headers)
data = response.json()
print("Kết quả:", data)`;

const SAMPLE_LOT_DOSSIER_JSON = `{
  "success": true,
  "status": 200,
  "data": {
    "lotInfo": {
      "lotId": "00000000-0000-0000-0000-000000000001",
      "lotName": "[DỮ LIỆU MẪU] Lô Xoài Cát Chu Thử Nghiệm",
      "productCategoryName": "Xoài Cát Chu",
      "expectedQuantity": 10000.0,
      "actualQuantity": 9800.0,
      "quantityUnit": "KG",
      "plantingDate": "2026-02-01",
      "harvestDate": "2026-07-15",
      "status": "HARVESTED"
    },
    "organizationInfo": {
      "organizationId": "00000000-0000-0000-0000-000000000002",
      "organizationName": "[DỮ LIỆU MẪU] Hợp Tác Xã Trái Cây Mẫu Nguồn Gốc Số",
      "organizationCode": "HTX-TEST-DEMO",
      "address": "Khu Thực Nghiệm Công Nghệ Nông Nghiệp Số",
      "phone": "0901234567",
      "email": "sandbox@nguongocso.vn"
    },
    "farmAreaInfo": {
      "farmAreaId": "00000000-0000-0000-0000-000000000003",
      "farmAreaName": "[DỮ LIỆU MẪU] Vùng Canh Tác Thực Nghiệm A1",
      "area": 2.0,
      "areaUnit": "HECTARE"
    },
    "certifications": [
      {
        "certificationName": "[DỮ LIỆU MẪU] Chứng nhận VietGAP Mẫu",
        "standardName": "VietGAP",
        "certificateCode": "VG-TEST-9999",
        "issueDate": "2026-01-01",
        "expiryDate": "2027-01-01",
        "issuedBy": "Hệ Thống Kiểm Nghiệm Thử Nghiệm"
      }
    ],
    "farmLogSummary": {
      "totalLogsRecorded": 25,
      "lastActivityAt": "2026-07-15T10:00:00"
    },
    "is_test": true,
    "test_notice": "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế"
  },
  "timestamp": "2026-09-18T08:08:18.022Z"
}`;

const SAMPLE_GS1_DOSSIER_JSON = `{
  "success": true,
  "status": 200,
  "data": {
    "shipment": {
      "id": "00000000-0000-0000-0000-000000000010",
      "name": "[DỮ LIỆU MẪU] Lô Hàng Xoài Cát Xuất Khẩu Thử Nghiệm",
      "codeValues": [
        "TEST-TRACE-001"
      ],
      "productCategory": "Xoài Cát Chu",
      "totalQuantity": 5000,
      "unit": "KG",
      "status": "ACTIVATED",
      "organization": {
        "id": "00000000-0000-0000-0000-000000000002",
        "name": "[DỮ LIỆU MẪU] Hợp Tác Xã Trái Cây Mẫu Nguồn Gốc Số",
        "code": "HTX-TEST-DEMO"
      }
    },
    "events": [
      {
        "eventId": "00000000-0000-0000-0000-000000000021",
        "eventType": "HARVESTING",
        "eventTypeLabel": "Thu hoạch",
        "recordedAt": "2026-07-20T08:00:00",
        "recordedBy": "Kỹ thuật viên Thử nghiệm",
        "location": {
          "latitude": 10.352,
          "longitude": 105.987
        },
        "details": {
          "yield": "5000 KG"
        }
      }
    ],
    "inspections": [
      {
        "requestId": "00000000-0000-0000-0000-000000000031",
        "inspectionUnit": "Trung Tâm Kiểm Nghiệm Thực Nghiệm",
        "sampleSentDate": "2026-07-18",
        "status": "PASSED",
        "criteria": [
          {
            "criterionCode": "CT-TEST-01",
            "criterionName": "Dư lượng kim loại nặng",
            "standardName": "VietGAP",
            "passed": true,
            "resultDate": "2026-07-19",
            "expiryDate": "2027-07-19"
          }
        ]
      }
    ],
    "mapping": {
      "standard": "GS1_SIMULATED_V1",
      "complianceNote": "Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ chính thức GS1"
    },
    "warnings": [],
    "exportedAt": "2026-09-18T15:08:50",
    "exportedBy": "Hệ Thống Thử Nghiệm Nguồn Gốc Số"
  },
  "timestamp": "2026-09-18T08:08:50.348Z"
}`;

interface EndpointCodeSnippetProps {
  endpointPath: string;
  copyId: string;
  responseCopyId: string;
  sampleResponseJson: string;
  responseBadge?: string;
  activeApiKey: string;
  copiedSection: string | null;
  onCopy: (text: string, id: string) => void;
  defaultEnv?: TargetEnvironment;
  defaultTab?: SnippetTab;
}

const EndpointCodeSnippet: React.FC<EndpointCodeSnippetProps> = ({
  endpointPath,
  copyId,
  responseCopyId,
  sampleResponseJson,
  responseBadge,
  activeApiKey,
  copiedSection,
  onCopy,
  defaultEnv = 'production',
  defaultTab = 'curl',
}) => {
  const [activeEnv, setActiveEnv] = useState<TargetEnvironment>(defaultEnv);
  const [activeTab, setActiveTab] = useState<SnippetTab>(defaultTab);

  const textToCopy =
    activeTab === 'curl'
      ? getSampleCurl(activeEnv, activeApiKey, endpointPath)
      : activeTab === 'fetch'
        ? getSampleFetch(activeEnv, activeApiKey, endpointPath)
        : getSamplePython(activeEnv, activeApiKey, endpointPath);

  return (
    <div className="space-y-4 pt-3 border-t border-border/60">
      {/* 1. Khối Lệnh gọi mẫu */}
      <div className="space-y-2.5">
        <div className="flex flex-wrap items-center justify-between gap-2.5">
          <div className="flex items-center gap-1.5 font-semibold text-xs text-foreground">
            <Code2 className="w-3.5 h-3.5 text-primary" />
            <span>Ví dụ lệnh gọi mẫu</span>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {/* Tabs chọn môi trường độc lập */}
            <div className="flex items-center gap-0.5 p-0.5 bg-muted rounded-md border border-border text-xs">
              <button
                type="button"
                onClick={() => setActiveEnv('production')}
                className={`px-2 py-0.5 rounded text-xs font-semibold transition-colors ${activeEnv === 'production'
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground'
                  }`}
              >
                Production
              </button>
              <button
                type="button"
                onClick={() => setActiveEnv('staging')}
                className={`px-2 py-0.5 rounded text-xs font-semibold transition-colors ${activeEnv === 'staging'
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground'
                  }`}
              >
                Staging
              </button>
              <button
                type="button"
                onClick={() => setActiveEnv('localhost')}
                className={`px-2 py-0.5 rounded text-xs font-semibold transition-colors ${activeEnv === 'localhost'
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground'
                  }`}
              >
                Localhost
              </button>
            </div>

            {/* Tabs chọn ngôn ngữ độc lập */}
            <div className="flex items-center gap-0.5 p-0.5 bg-muted rounded-md border border-border text-xs">
              <button
                type="button"
                onClick={() => setActiveTab('curl')}
                className={`px-2.5 py-0.5 rounded text-xs font-semibold transition-colors ${activeTab === 'curl'
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground'
                  }`}
              >
                cURL
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('fetch')}
                className={`px-2.5 py-0.5 rounded text-xs font-semibold transition-colors ${activeTab === 'fetch'
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground'
                  }`}
              >
                JavaScript
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('python')}
                className={`px-2.5 py-0.5 rounded text-xs font-semibold transition-colors ${activeTab === 'python'
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:text-foreground'
                  }`}
              >
                Python
              </button>
            </div>
          </div>
        </div>

        <div className="relative rounded-xl bg-slate-950 p-4 font-mono text-xs sm:text-sm text-slate-100 overflow-x-auto leading-relaxed border border-slate-800">
          <Button
            type="button"
            size="sm"
            variant="outline"
            className="absolute top-3 right-3 bg-slate-900/80 hover:bg-slate-800 text-slate-200 border-slate-700 gap-1 text-xs"
            onClick={() => onCopy(textToCopy, copyId)}
          >
            {copiedSection === copyId ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span>Đã chép</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" />
                <span>Sao chép</span>
              </>
            )}
          </Button>
          <pre className="select-all">
            {textToCopy}
          </pre>
        </div>
      </div>

      {/* 2. Khối Ví dụ dữ liệu phản hồi mẫu */}
      <div className="space-y-2 pt-2 border-t border-border/40">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <FileCode className="w-3.5 h-3.5 text-primary" />
            <span className="text-xs font-semibold text-foreground">
              Ví dụ dữ liệu phản hồi mẫu
            </span>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20">
              Status: 200 OK
            </span>
            <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-muted text-muted-foreground border border-border">
              application/json
            </span>
          </div>
          {responseBadge && (
            <span className="text-[11px] font-medium text-blue-600 dark:text-blue-400">
              {responseBadge}
            </span>
          )}
        </div>

        <div className="relative rounded-xl bg-slate-950 p-4 font-mono text-xs text-slate-100 overflow-x-auto leading-relaxed border border-slate-800">
          <Button
            type="button"
            size="sm"
            variant="outline"
            className="absolute top-3 right-3 bg-slate-900/80 hover:bg-slate-800 text-slate-200 border-slate-700 gap-1 text-xs"
            onClick={() => onCopy(sampleResponseJson, responseCopyId)}
          >
            {copiedSection === responseCopyId ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span>Đã chép JSON</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" />
                <span>Sao chép JSON</span>
              </>
            )}
          </Button>
          <pre className="select-all">
            {sampleResponseJson}
          </pre>
        </div>
      </div>
    </div>
  );
};

export const DataPortalDocsPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [copiedSection, setCopiedSection] = useState<string | null>(null);
  const [customApiKey, setCustomApiKey] = useState<string>('Ví dụ');

  const copyToClipboard = async (text: string, sectionId: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedSection(sectionId);
      toast.success('Đã sao chép vào khay nhớ tạm!');
      setTimeout(() => setCopiedSection(null), 2500);
    } catch {
      toast.error('Không thể sao chép tự động. Vui lòng chọn và sao chép thủ công.');
    }
  };

  const activeApiKey = customApiKey.trim() || '<YOUR_API_KEY>';

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col selection:bg-primary/20">
      {/* Navigation Bar */}
      <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/95 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div
              className="cursor-pointer flex items-center gap-2"
              onClick={() => navigate('/')}
            >
              <Logo height={42} />
            </div>
            <div className="hidden md:flex items-center gap-2 pl-4 border-l border-border/80">
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-primary/10 text-primary border border-primary/20">
                Tài liệu API v1.0
              </span>
              <span className="text-xs text-muted-foreground font-medium">
                Cổng dữ liệu Nguồn Gốc Số
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate('/')}
              className="text-xs font-medium gap-1.5"
            >
              <Home className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Trang chủ</span>
            </Button>
            {user ? (
              <Button
                variant="create"
                size="sm"
                onClick={() => navigate('/dashboard')}
                className="text-xs font-medium gap-1.5"
              >
                <span>Bảng điều khiển</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            ) : (
              <Button
                variant="outline"
                size="sm"
                onClick={() => navigate('/login')}
                className="text-xs font-medium gap-1.5 border-primary/30 text-primary hover:bg-primary/10"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>Đăng nhập</span>
              </Button>
            )}
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden border-b border-border/60 bg-gradient-to-b from-primary/5 via-background to-background py-12 sm:py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 relative z-10">
          <div className="max-w-3xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium bg-emerald-100 dark:bg-emerald-950 text-emerald-800 dark:text-emerald-300 border border-emerald-300/40">
              <ShieldCheck className="w-3.5 h-3.5" />
              Cổng tích hợp B2B & Chuỗi cung ứng nông sản
            </div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-foreground">
              Tài liệu Cổng dữ liệu Nguồn Gốc Số
            </h1>
            <p className="text-base sm:text-lg text-muted-foreground leading-relaxed">
              API chuẩn hóa kết nối bên thứ ba, cho phép doanh nghiệp thu mua, sàn thương mại
              điện tử và hệ thống ERP tự động truy xuất hồ sơ nguồn gốc sản phẩm theo hướng chuẩn quốc tế GS1 EPCIS.
            </p>
          </div>
        </div>
      </section>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 py-10 space-y-12 flex-1">
        {/* Section 1: Tổng quan & Cơ chế xác thực */}
        <section id="overview" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Server className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              1. Tổng quan & Cơ chế Xác thực
            </h2>
          </div>

          {/* Hướng dẫn sử dụng Cổng dữ liệu */}
          <div className="rounded-lg border border-border bg-white dark:bg-card p-4 sm:p-5 text-sm space-y-3 shadow-xs">
            <div className="flex items-center gap-2 font-semibold text-primary">
              <BookOpen className="w-4 h-4" />
              <span>Hướng dẫn sử dụng Cổng dữ liệu Nguồn Gốc Số</span>
            </div>
            <p className="text-muted-foreground leading-relaxed text-xs sm:text-sm">
              Cổng dữ liệu Nguồn Gốc Số cung cấp chuẩn giao diện lập trình ứng dụng (RESTful API) mở, cho phép các bên liên quan (doanh nghiệp thu mua, sàn thương mại điện tử, đơn vị logistics và đối tác quốc tế) tự động tích hợp và truy xuất hồ sơ chuỗi giá trị nông sản theo thời gian thực.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs pt-1">
              <div className="bg-slate-50/80 dark:bg-muted/40 rounded-md border border-border p-3 space-y-1">
                <div className="font-semibold text-foreground flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-primary/10 text-primary flex items-center justify-center text-[11px] font-bold">1</span>
                  Lấy Khóa API (API Key)
                </div>
                <p className="text-muted-foreground leading-normal">
                  Liên hệ Quản trị viên Hợp tác xã (HTX) để được cấp mã khóa tích hợp (khóa thử nghiệm để kiểm thử hoặc khóa chính thức).
                </p>
              </div>
              <div className="bg-slate-50/80 dark:bg-muted/40 rounded-md border border-border p-3 space-y-1">
                <div className="font-semibold text-foreground flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-primary/10 text-primary flex items-center justify-center text-[11px] font-bold">2</span>
                  Cấu hình HTTP Request
                </div>
                <p className="text-muted-foreground leading-normal">
                  Lựa chọn môi trường Base URL phù hợp và đính kèm khóa vào HTTP Request Header <code className="font-mono text-primary bg-primary/10 px-1 py-0.5 rounded text-[11px]">X-API-KEY: &lt;khóa&gt;</code>.
                </p>
              </div>
              <div className="bg-slate-50/80 dark:bg-muted/40 rounded-md border border-border p-3 space-y-1">
                <div className="font-semibold text-foreground flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-primary/10 text-primary flex items-center justify-center text-[11px] font-bold">3</span>
                  Truy xuất & Nhận Dữ liệu
                </div>
                <p className="text-muted-foreground leading-normal">
                  Gọi các endpoint tại Mục 2 để nhận dữ liệu hồ sơ lô nông sản chi tiết hoặc xuất chuẩn hóa theo lược đồ quốc tế GS1 EPCIS 2.0.
                </p>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Card className="bg-card">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  Base URL API
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-xs">
                <div className="space-y-1">
                  <span className="text-[11px] font-semibold text-primary">● Production:</span>
                  <div className="font-mono bg-muted p-1.5 rounded border border-border break-all select-all font-semibold text-foreground">
                    https://agri-trace.online
                  </div>
                </div>
                <div className="space-y-1">
                  <span className="text-[11px] font-semibold text-amber-500">● Staging:</span>
                  <div className="font-mono bg-muted p-1.5 rounded border border-border break-all select-all text-foreground">
                    https://staging.agri-trace.online
                  </div>
                </div>
                <div className="space-y-1">
                  <span className="text-[11px] font-semibold text-sky-500">● Localhost:</span>
                  <div className="font-mono bg-muted p-1.5 rounded border border-border break-all select-all text-foreground">
                    http://localhost:8080
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-card">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  Authentication Header
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-1.5 text-xs">
                <div className="font-mono bg-muted p-2 rounded border border-border break-all select-all font-semibold text-foreground">
                  X-API-KEY: &lt;chuỗi_khóa&gt;
                </div>
                <p className="text-muted-foreground">
                  Đính kèm khóa API trong HTTP Request Header <code className="font-mono">X-API-KEY</code> hoặc <code className="font-mono">X-Api-Key</code>.
                </p>
              </CardContent>
            </Card>

            <Card className="bg-card">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  Rate Limiting
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-1.5 text-xs">
                <div className="font-mono bg-muted p-2 rounded border border-border font-semibold text-foreground">
                  Mặc định 30 lượt / giờ (Khóa thử)
                </div>
                <p className="text-muted-foreground">
                  Khóa thử nghiệm áp dụng hạn mức thấp (mặc định 30, tối đa 50 lượt/giờ). Vượt quá hạn mức trả về mã lỗi HTTP <code className="font-mono text-destructive">429 Too Many Requests</code>.
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Quy tắc Tiền tố Khóa & Phạm vi Sandbox */}
          <Card className="bg-card border-border overflow-hidden">
            <CardHeader className="bg-muted/30 pb-3">
              <div className="flex items-center gap-2">
                <Key className="w-4 h-4 text-primary" />
                <CardTitle className="text-sm font-semibold">
                  Quy tắc Tiền tố Khóa &amp; Phạm vi Thử nghiệm
                </CardTitle>
              </div>
              <CardDescription className="text-xs mt-1">
                Hướng dẫn chi tiết về cấu trúc khóa API và cơ chế cách ly an toàn dành cho đội ngũ kỹ thuật tích hợp.
              </CardDescription>
            </CardHeader>
            <CardContent className="p-4 sm:p-5 space-y-4 text-xs sm:text-sm">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-3.5 rounded-lg border border-primary/20 bg-primary/5 space-y-2">
                  <div className="font-semibold text-foreground flex items-center gap-2 text-xs">
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-primary/20 text-primary border border-primary/30">
                      nks_test_...
                    </span>
                    <span>Khóa Thử Nghiệm</span>
                  </div>
                  <p className="text-xs text-muted-foreground leading-relaxed">
                    Được cấp để đội kỹ thuật đối tác kết nối và kiểm thử API. Tự động kích hoạt cơ chế Sandbox, cách ly hoàn toàn khỏi dữ liệu thật.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1 list-disc list-inside">
                    <li>Hạn mức thấp: <strong>mặc định 30 lượt/giờ</strong> (tối đa 50 lượt/giờ).</li>
                    <li>Thời hạn ngắn: <strong>tối đa 15 ngày</strong> kể từ thời điểm cấp.</li>
                    <li>Response luôn có: <code className="font-mono text-primary font-semibold">&quot;is_test&quot;: true</code> và <code className="font-mono text-primary font-semibold">&quot;test_notice&quot;</code>.</li>
                  </ul>
                </div>

                <div className="p-3.5 rounded-lg border border-border bg-muted/30 space-y-2">
                  <div className="font-semibold text-foreground flex items-center gap-2 text-xs">
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-slate-700 text-slate-200 border border-slate-600">
                      nks_live_...
                    </span>
                    <span>Khóa Chính Thức</span>
                  </div>
                  <p className="text-xs text-muted-foreground leading-relaxed">
                    Dành cho hệ thống vận hành chính thức sau khi đối tác hoàn tất kiểm thử tích hợp và được Quản lý Hợp tác xã phê duyệt.
                  </p>
                  <ul className="text-xs text-muted-foreground space-y-1 list-disc list-inside">
                    <li>Hạn mức cao: thỏa thuận theo hợp đồng (500 – 5.000 - ... lượt/giờ).</li>
                    <li>Thời hạn dài: tùy biến theo kỳ hợp tác nông nghiệp.</li>
                    <li>Truy xuất dữ liệu thực tế của các lô thuộc quyền sở hữu của HTX cấp khóa.</li>
                  </ul>
                </div>
              </div>

              <div className="p-3.5 rounded-lg border border-amber-500/20 bg-amber-500/5 space-y-2 text-xs">
                <div className="font-semibold text-amber-600 dark:text-amber-400 flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>Quy định Phạm vi Dữ liệu của Khóa Thử nghiệm</span>
                </div>
                <p className="text-muted-foreground leading-relaxed">
                  Để đảm bảo an toàn và bảo mật thông tin nội bộ của các nông hộ, <strong>Khóa thử nghiệm chỉ được phép truy cập vào các mã dữ liệu mẫu chuẩn hóa sau:</strong>
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1 font-mono text-[11px]">
                  <div className="p-2 bg-background/80 rounded border border-border">
                    <span className="text-muted-foreground block text-[10px] uppercase font-sans">Lô sản xuất</span>
                    <strong className="text-primary font-semibold">sample-lot-001</strong>
                  </div>
                  <div className="p-2 bg-background/80 rounded border border-border">
                    <span className="text-muted-foreground block text-[10px] uppercase font-sans">Lô hàng GS1</span>
                    <strong className="text-primary font-semibold">sample-shipment-001</strong>
                  </div>
                  <div className="p-2 bg-background/80 rounded border border-border">
                    <span className="text-muted-foreground block text-[10px] uppercase font-sans">Tem truy xuất</span>
                    <strong className="text-primary font-semibold">TEST-TRACE-001</strong>
                  </div>
                </div>
                <p className="text-muted-foreground text-[11px] leading-relaxed pt-1">
                  Nếu gọi với bất kỳ mã lô hoặc ID nào khác, hệ thống sẽ từ chối với mã lỗi HTTP <code className="font-mono text-destructive font-semibold">403 Forbidden</code> kèm thông điệp: <em>&quot;Khóa thử nghiệm chỉ được phép truy cập mã lô &apos;sample-lot-001&apos;. Vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa API thật.&quot;</em>
                </p>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Section 2: Danh sách các Endpoints */}
        <section id="endpoints" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Layers className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              2. Danh sách Endpoints Tích hợp
            </h2>
          </div>

          <div className="space-y-6">
            {/* Khung cấu hình Khóa API */}
            <Card className="bg-card border-border overflow-hidden">
              <CardHeader className="bg-muted/30 pb-3">
                <div className="flex items-center gap-2">
                  <Code2 className="w-4 h-4 text-primary" />
                  <CardTitle className="text-sm font-semibold">
                    Ví dụ Gọi Thử nghiệm
                  </CardTitle>
                </div>
                <CardDescription className="text-xs mt-1">
                  Nhập khóa API thử nghiệm do HTX cấp để sinh và hiển thị tự động các ví dụ lệnh gọi tại từng endpoint bên dưới.
                </CardDescription>
              </CardHeader>

              {/* Ô nhập API Key thử nghiệm */}
              <div className="p-4 bg-muted/20 space-y-3 border-t border-border">
                <div className="flex items-start gap-2 p-3 rounded-lg bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-800/40 text-blue-900 dark:text-blue-300 text-xs leading-relaxed max-w-xl">
                  <Info className="w-4 h-4 shrink-0 text-blue-600 dark:text-blue-400 mt-0.5" />
                  <span>
                    Hãy thay thế khóa <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">&quot;Ví dụ&quot;</code> bằng API Key thử nghiệm hoặc API Key thật do Quản lý HTX hoặc Admin cấp.
                  </span>
                </div>
                <label
                  htmlFor="portal-api-key-input"
                  className="text-xs font-medium text-foreground flex items-center gap-1.5"
                >
                  <Key className="w-3.5 h-3.5 text-primary" />
                  <span>Khóa API thử nghiệm của bạn (do HTX cấp):</span>
                </label>
                <div className="flex items-center gap-2 max-w-xl">
                  <Input
                    id="portal-api-key-input"
                    type="text"
                    placeholder="Nhập khóa API (ví dụ: nks_test_...)"
                    value={customApiKey}
                    onChange={(e) => setCustomApiKey(e.target.value)}
                    className="font-mono text-xs sm:text-sm bg-background"
                  />
                  {customApiKey && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setCustomApiKey('')}
                      className="text-xs text-muted-foreground hover:text-foreground h-11 px-3"
                    >
                      Xóa
                    </Button>
                  )}
                </div>
                <p className="text-[11px] text-muted-foreground">
                  Lưu ý: Nếu chưa có khóa, vui lòng liên hệ tới quản trị viên/quản lý hợp tác xã để được cấp khóa thử nghiệm.
                </p>
              </div>
            </Card>


            {/* Endpoint 1 */}
            <Card className="bg-card border-border overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-border bg-muted/30 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-600 text-white uppercase tracking-wider">
                    GET
                  </span>
                  <code className="font-mono text-sm sm:text-base font-semibold text-foreground">
                    /api/publicapi/v1/lots/{'{lotId}'}
                  </code>
                </div>
                <span className="text-xs font-medium text-muted-foreground">
                  Tra cứu hồ sơ lô sản xuất
                </span>
              </div>
              <CardContent className="p-4 sm:p-6 space-y-4 text-xs sm:text-sm">
                <p className="text-muted-foreground">
                  Cho phép đối tác lấy thông tin cơ bản của lô sản xuất theo mã định danh. Với khóa thử nghiệm.
                </p>
                <div className="flex items-start gap-2 p-3 rounded-lg bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-800/40 text-blue-900 dark:text-blue-300 text-xs leading-relaxed">
                  <Info className="w-4 h-4 shrink-0 text-blue-600 dark:text-blue-400 mt-0.5" />
                  <span>
                    Hãy thay thế <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">{'{lotId}'}</code> bằng mã lô sản xuất bạn muốn truy cập, thay thế khóa <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">&quot;Ví dụ&quot;</code> bằng API Key thử nghiệm hoặc API Key thật do Quản lý HTX hoặc Admin cấp.
                  </span>
                </div>
                {customApiKey.trim() && (
                  <EndpointCodeSnippet
                    endpointPath="/api/publicapi/v1/lots/sample-lot-001"
                    copyId="code-sample-ep1"
                    responseCopyId="json-sample-ep1"
                    sampleResponseJson={SAMPLE_LOT_DOSSIER_JSON}
                    activeApiKey={activeApiKey}
                    copiedSection={copiedSection}
                    onCopy={copyToClipboard}
                  />
                )}
              </CardContent>
            </Card>

            {/* Endpoint 2 */}
            <Card className="bg-card border-border overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-border bg-muted/30 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-600 text-white uppercase tracking-wider">
                    GET
                  </span>
                  <code className="font-mono text-sm sm:text-base font-semibold text-foreground">
                    /api/v1/partner/production-lots/{'{lotId}'}/dossier
                  </code>
                </div>
                <span className="text-xs font-medium text-muted-foreground">
                  Hồ sơ truy xuất chuỗi giá trị nông sản chi tiết
                </span>
              </div>
              <CardContent className="p-4 sm:p-6 space-y-4 text-xs sm:text-sm">
                <p className="text-muted-foreground">
                  Truy xuất đầy đủ hồ sơ của lô sản xuất bao gồm: thông tin vùng trồng, giống cây trồng, nhật ký canh tác theo mốc, danh sách vật tư nông nghiệp đã sử dụng và kết quả kiểm nghiệm đạt chuẩn.
                </p>
                <div className="flex items-start gap-2 p-3 rounded-lg bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-800/40 text-blue-900 dark:text-blue-300 text-xs leading-relaxed">
                  <Info className="w-4 h-4 shrink-0 text-blue-600 dark:text-blue-400 mt-0.5" />
                  <span>
                    Hãy thay thế <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">{'{lotId}'}</code> bằng mã lô sản xuất bạn muốn truy cập, thay thế khóa <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">&quot;Ví dụ&quot;</code> bằng API Key thử nghiệm hoặc API Key thật do Quản lý HTX hoặc Admin cấp.
                  </span>
                </div>
                {customApiKey.trim() && (
                  <EndpointCodeSnippet
                    endpointPath="/api/v1/partner/production-lots/sample-lot-001/dossier"
                    copyId="code-sample-ep2"
                    responseCopyId="json-sample-ep2"
                    sampleResponseJson={SAMPLE_LOT_DOSSIER_JSON}
                    activeApiKey={activeApiKey}
                    copiedSection={copiedSection}
                    onCopy={copyToClipboard}
                  />
                )}
              </CardContent>
            </Card>

            {/* Endpoint 3 */}
            <Card className="bg-card border-border overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-border bg-muted/30 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-600 text-white uppercase tracking-wider">
                    GET
                  </span>
                  <code className="font-mono text-sm sm:text-base font-semibold text-foreground">
                    /api/v1/partner/shipments/{'{shipmentId}'}/dossier/gs1
                  </code>
                </div>
                <span className="text-xs font-medium text-muted-foreground">
                  Hồ sơ lô hàng xuất theo lược đồ GS1 EPCIS
                </span>
              </div>
              <CardContent className="p-4 sm:p-6 space-y-4 text-xs sm:text-sm">
                <p className="text-muted-foreground">
                  Xuất dữ liệu theo định dạng JSON-LD tuân thủ tiêu chuẩn GS1 EPCIS 2.0 (Electronic Product Code Information Services).
                </p>
                <div className="flex items-start gap-2 p-3 rounded-lg bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-800/40 text-blue-900 dark:text-blue-300 text-xs leading-relaxed">
                  <Info className="w-4 h-4 shrink-0 text-blue-600 dark:text-blue-400 mt-0.5" />
                  <span>
                    Hãy thay thế <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">{'{shipmentId}'}</code> bằng mã lô hàng bạn muốn truy cập, thay thế khóa <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900/50 px-1 py-0.5 rounded text-[11px] text-blue-800 dark:text-blue-200">&quot;Ví dụ&quot;</code> bằng API Key thử nghiệm hoặc API Key thật do Quản lý HTX hoặc Admin cấp.
                  </span>
                </div>
                {customApiKey.trim() && (
                  <EndpointCodeSnippet
                    endpointPath="/api/v1/partner/shipments/sample-shipment-001/dossier/gs1"
                    copyId="code-sample-ep3"
                    responseCopyId="json-sample-ep3"
                    sampleResponseJson={SAMPLE_GS1_DOSSIER_JSON}
                    responseBadge='★ Chuẩn hóa: GS1 EPCIS 2.0'
                    activeApiKey={activeApiKey}
                    copiedSection={copiedSection}
                    onCopy={copyToClipboard}
                  />
                )}
              </CardContent>
            </Card>

            {/* Khung Thông số chung: Tham số đường dẫn & HTTP Request Headers */}
            <Card className="bg-card border-border overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-border bg-muted/30 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <SlidersHorizontal className="w-4 h-4 text-primary" />
                  <span className="font-semibold text-sm sm:text-base text-foreground">
                    Tham số Đường dẫn (Path Parameters) & HTTP Request Headers
                  </span>
                </div>
                <span className="text-xs font-medium text-muted-foreground">
                  Quy chuẩn dữ liệu đầu vào áp dụng cho các Endpoint tích hợp
                </span>
              </div>
              <CardContent className="p-4 sm:p-6 space-y-6 text-xs sm:text-sm">
                {/* Tham số đường dẫn */}
                <div className="space-y-2">
                  <h4 className="font-semibold text-foreground text-xs uppercase tracking-wider">
                    Tham số đường dẫn (Path Parameters)
                  </h4>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs border border-border rounded-lg overflow-hidden">
                      <thead className="bg-muted text-muted-foreground font-semibold">
                        <tr>
                          <th className="p-2.5 border-b border-border">Tham số</th>
                          <th className="p-2.5 border-b border-border">Kiểu</th>
                          <th className="p-2.5 border-b border-border">Bắt buộc</th>
                          <th className="p-2.5 border-b border-border">Mô tả</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr className="border-b border-border/50">
                          <td className="p-2.5 font-mono text-primary font-semibold">lotId</td>
                          <td className="p-2.5 font-mono">string / UUID</td>
                          <td className="p-2.5 text-destructive font-semibold">Có</td>
                          <td className="p-2.5 text-muted-foreground">Mã UUID hoặc chuỗi định danh của lô sản xuất (Ví dụ: <code className="font-mono text-foreground font-semibold">sample-lot-001</code> với khóa thử nghiệm)</td>
                        </tr>
                        <tr>
                          <td className="p-2.5 font-mono text-primary font-semibold">shipmentId</td>
                          <td className="p-2.5 font-mono">string / UUID</td>
                          <td className="p-2.5 text-destructive font-semibold">Có</td>
                          <td className="p-2.5 text-muted-foreground">Mã UUID hoặc chuỗi định danh của lô hàng xuất (Ví dụ: <code className="font-mono text-foreground font-semibold">sample-shipment-001</code> với khóa thử nghiệm)</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* HTTP Request Headers */}
                <div className="space-y-2">
                  <h4 className="font-semibold text-foreground text-xs uppercase tracking-wider">
                    HTTP Request Headers
                  </h4>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs border border-border rounded-lg overflow-hidden">
                      <thead className="bg-muted text-muted-foreground font-semibold">
                        <tr>
                          <th className="p-2.5 border-b border-border">Header</th>
                          <th className="p-2.5 border-b border-border">Bắt buộc</th>
                          <th className="p-2.5 border-b border-border">Giá trị ví dụ</th>
                          <th className="p-2.5 border-b border-border">Mô tả</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr className="border-b border-border/50">
                          <td className="p-2.5 font-mono font-semibold">X-API-KEY</td>
                          <td className="p-2.5 text-destructive font-semibold">Có</td>
                          <td className="p-2.5 font-mono text-muted-foreground">nks_test_a1b2c3d4e5f6g7h8i9j0</td>
                          <td className="p-2.5 text-muted-foreground">Khóa API xác thực được cấp bởi Hợp tác xã hoặc quản trị viên</td>
                        </tr>
                        <tr>
                          <td className="p-2.5 font-mono font-semibold">Accept</td>
                          <td className="p-2.5 text-muted-foreground">Không</td>
                          <td className="p-2.5 font-mono text-muted-foreground">application/json</td>
                          <td className="p-2.5 text-muted-foreground">Định dạng nội dung phản hồi mong muốn (<code className="font-mono text-foreground">application/json</code> hoặc <code className="font-mono text-foreground">application/xml</code>)</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Section 3: Bảng ánh xạ trường GS1 */}
        <section id="gs1-mapping" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Layers className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              3. Bảng Ánh xạ Thuộc tính theo Chuẩn GS1 EPCIS
            </h2>
          </div>

          <Card className="bg-card border-border overflow-hidden">
            <CardContent className="p-4 sm:p-6">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs sm:text-sm border border-border rounded-lg overflow-hidden">
                  <thead className="bg-muted text-muted-foreground font-semibold">
                    <tr>
                      <th className="p-2.5 sm:p-3 border-b border-border">Thuộc tính Nguồn Gốc Số</th>
                      <th className="p-2.5 sm:p-3 border-b border-border">Thuộc tính GS1 EPCIS 2.0</th>
                      <th className="p-2.5 sm:p-3 border-b border-border">Định dạng / Mã chuẩn</th>
                      <th className="p-2.5 sm:p-3 border-b border-border">Mục đích & Ý nghĩa</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Mã lô sản xuất (GTIN)</td>
                      <td className="p-2.5 sm:p-3 font-mono text-primary font-semibold">epcList / itemGtin</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs">urn:epc:id:sgtin:8938501...</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Mã định danh sản phẩm theo GS1 toàn cầu</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Mã vùng trồng / Cơ sở</td>
                      <td className="p-2.5 sm:p-3 font-mono text-primary font-semibold">bizLocation / GLN</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs">urn:epc:id:sgln:8938501...</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Mã địa điểm toàn cầu xác định tọa độ nông trại, nhà kho</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Bước nghiệp vụ (Công đoạn)</td>
                      <td className="p-2.5 sm:p-3 font-mono text-primary font-semibold">bizStep</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs">urn:epcglobal:cbv:bizstep:...</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Thu hoạch, Đóng gói, Vận chuyển</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Trạng thái chất lượng</td>
                      <td className="p-2.5 sm:p-3 font-mono text-primary font-semibold">disposition</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs">urn:epcglobal:cbv:disp:...</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Đạt chuẩn, Đang kiểm nghiệm</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Thời điểm ghi nhận</td>
                      <td className="p-2.5 sm:p-3 font-mono text-primary font-semibold">eventTime</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs">ISO 8601 (UTC)</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Thời gian chính xác khi sự kiện diễn ra tại nông hộ</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Section 4: Bảng mã lỗi tổng hợp */}
        <section id="error-codes" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <AlertTriangle className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              4. Bảng Mã Lỗi Tổng hợp (HTTP Error Codes)
            </h2>
          </div>

          <Card className="bg-card border-border overflow-hidden">
            <CardContent className="p-4 sm:p-6">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs sm:text-sm border border-border rounded-lg overflow-hidden">
                  <thead className="bg-muted text-muted-foreground font-semibold">
                    <tr>
                      <th className="p-2.5 sm:p-3 border-b border-border">Mã HTTP</th>
                      <th className="p-2.5 sm:p-3 border-b border-border">Tên lỗi</th>
                      <th className="p-2.5 sm:p-3 border-b border-border">Nguyên nhân</th>
                      <th className="p-2.5 sm:p-3 border-b border-border">Thông điệp phản hồi gợi ý</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-emerald-600">200</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">OK</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Yêu cầu thành công, dữ liệu được trả về.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;success&quot;: true</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-amber-600">400</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Bad Request</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Thiếu tham số hoặc định dạng dữ liệu không hợp lệ.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Tham số không hợp lệ&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">401</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Unauthorized</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Khóa thử nghiệm đã hết hạn hiệu lực (sau tối đa 15 ngày).</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Khóa thử nghiệm đã hết hạn&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">401</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Unauthorized</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Khóa truy cập chính thức (Live Key) đã hết thời gian hiệu lực.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Khóa truy cập đã hết thời gian hiệu lực&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">401</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Unauthorized</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Khóa API không tồn tại trong hệ thống hoặc không đúng.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Khóa truy cập không hợp lệ&quot; / &quot;Khóa thử nghiệm không đúng. Vui lòng liên hệ...&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">401</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Unauthorized</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Khóa API đã bị Quản lý Hợp tác xã thu hồi hiệu lực.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Khóa truy cập đã bị thu hồi và không còn hiệu lực&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">401</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Unauthorized</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Request không gửi kèm tiêu đề HTTP xác thực bắt buộc.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Thiếu Header X-API-KEY&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">403</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Forbidden</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Khóa thử nghiệm cố truy cập mã lô hoặc ID ngoài phạm vi dữ liệu mẫu Sandbox.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Khóa thử nghiệm chỉ được phép truy cập mã lô \&quot;sample-lot-001\&quot;. Vui lòng liên hệ...&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-rose-600">403</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Forbidden</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Tài khoản không có quyền hạn Quản lý HTX khi gọi API cấp khóa.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Bạn không có quyền thực hiện thao tác này&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-amber-600">404</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Not Found</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Không tìm thấy lô sản xuất với mã đã chỉ định.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Không tìm thấy lô sản xuất yêu cầu&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-purple-600">429</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Too Many Requests</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Vượt quá hạn mức số lượt gọi trong 1 giờ (mặc định 30 lượt/giờ đối với khóa thử nghiệm - QTN-20).</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Khóa truy cập đã vượt quá hạn mức &#123;limit&#125; lượt gọi/giờ&quot;</td>
                    </tr>
                    <tr className="hover:bg-muted/30 transition-colors">
                      <td className="p-2.5 sm:p-3 font-mono font-bold text-destructive">500</td>
                      <td className="p-2.5 sm:p-3 font-semibold text-foreground">Internal Server Error</td>
                      <td className="p-2.5 sm:p-3 text-muted-foreground">Lỗi hệ thống máy chủ nội bộ.</td>
                      <td className="p-2.5 sm:p-3 font-mono text-xs text-muted-foreground">&quot;Lỗi máy chủ nội bộ. Vui lòng liên hệ quản trị.&quot;</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-border/60 bg-muted/20 py-8 text-center text-xs text-muted-foreground">
        <div className="max-w-7xl mx-auto px-4 space-y-2">
          <p>© 2026 Nguồn Gốc Số. Hệ sinh thái truy xuất nguồn gốc nông sản thông minh.</p>
          <p>Tương thích và mô phỏng chuẩn GS1 EPCIS 2.0 &amp; tiêu chuẩn xuất khẩu quốc tế.</p>
        </div>
      </footer>
    </div>
  );
};

export default DataPortalDocsPage;
