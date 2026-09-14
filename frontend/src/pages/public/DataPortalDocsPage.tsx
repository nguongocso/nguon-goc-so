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
  FileCode,
} from 'lucide-react';
import { Logo } from '@/components/common/Logo';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { useAuth } from '@/hooks/useAuth';

export const DataPortalDocsPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [copiedSection, setCopiedSection] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'curl' | 'fetch' | 'python'>('curl');

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

  const sampleCurl = `curl -X GET "https://agri-trace.online/api/publicapi/v1/lots/sample-lot-001" \\
  -H "Accept: application/json" \\
  -H "X-API-KEY: nks_test_sample_key_1234567890"`;

  const sampleFetch = `fetch("https://agri-trace.online/api/publicapi/v1/lots/sample-lot-001", {
  method: "GET",
  headers: {
    "Accept": "application/json",
    "X-API-KEY": "nks_test_sample_key_1234567890"
  }
})
  .then(response => response.json())
  .then(data => console.log(data))
  .catch(error => console.error("Lỗi:", error));`;

  const samplePython = `import requests

url = "https://agri-trace.online/api/publicapi/v1/lots/sample-lot-001"
headers = {
    "Accept": "application/json",
    "X-API-KEY": "nks_test_sample_key_1234567890"
}

response = requests.get(url, headers=headers)
data = response.json()
print("Kết quả:", data)`;


  const sampleJsonResponse = `{
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
    "testNotice": "Dữ liệu thử nghiệm (Sandbox Mode) - Không phải dữ liệu thực tế"
  },
  "timestamp": "2026-09-14T10:00:00.000Z"
}`;

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

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Card className="bg-card">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
                  Base URL API
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-1.5 text-xs">
                <div className="font-mono bg-muted p-2 rounded border border-border break-all select-all font-semibold text-foreground">
                  https://agri-trace.online
                </div>
                <p className="text-muted-foreground">
                  Hỗ trợ cả môi trường kiểm thử cục bộ: <code className="font-mono">http://localhost:8080</code>
                </p>
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
                  Tối đa 100 lượt / giờ (Sandbox)
                </div>
                <p className="text-muted-foreground">
                  Nếu vượt quá hạn mức, hệ thống trả về mã lỗi HTTP <code className="font-mono text-destructive">429 Too Many Requests</code>.
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Sandbox Explanations */}
          <div className="p-4 rounded-xl bg-blue-50/70 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 text-xs sm:text-sm leading-relaxed space-y-2">
            <div className="flex items-center gap-2 font-semibold text-blue-900 dark:text-blue-200">
              <ShieldCheck className="w-4 h-4 text-blue-600 dark:text-blue-400" />
              <span>Chế độ Thử nghiệm (Sandbox Mode - is_test: true)</span>
            </div>
            <p className="text-blue-800 dark:text-blue-300">
              Khi đối tác sử dụng khóa thử nghiệm (tiền tố <code className="font-mono font-bold">nks_test_</code>), hệ thống kích hoạt cơ chế <strong>Sandbox</strong>:
            </p>
            <ul className="list-disc list-inside space-y-1 text-blue-800 dark:text-blue-300 pl-2">
              <li>Mọi yêu cầu lấy thông tin lô (kể cả khi truyền mã lô thật) đều nhận về <strong>dữ liệu mẫu mô phỏng chuẩn GS1</strong>.</li>
              <li>Mọi phản hồi JSON luôn đính kèm cờ nhận diện <code className="font-mono font-semibold bg-blue-100 dark:bg-blue-900 px-1 py-0.5 rounded">&quot;is_test&quot;: true</code>.</li>
              <li>Bảo vệ toàn vẹn và tuyệt đối bí mật dữ liệu canh tác và khách hàng thật của HTX trong suốt quá trình đối tác kết nối tích hợp.</li>
            </ul>
          </div>
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
                  Tra cứu hồ sơ lô sản xuất (Công khai & Sandbox)
                </span>
              </div>
              <CardContent className="p-4 sm:p-6 space-y-4 text-xs sm:text-sm">
                <p className="text-muted-foreground">
                  Cho phép đối tác lấy thông tin cơ bản của lô sản xuất theo mã định danh. Với khóa thử nghiệm, API luôn trả về lô mẫu kèm cờ <code className="font-mono text-primary font-bold">is_test: true</code>.
                </p>

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
                          <td className="p-2.5 text-muted-foreground">Mã UUID hoặc chuỗi định danh của lô sản xuất</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

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
                        </tr>
                      </thead>
                      <tbody>
                        <tr className="border-b border-border/50">
                          <td className="p-2.5 font-mono font-semibold">X-API-KEY</td>
                          <td className="p-2.5 text-destructive font-semibold">Có</td>
                          <td className="p-2.5 font-mono text-muted-foreground">nks_test_a1b2c3d4e5f6g7h8i9j0</td>
                        </tr>
                        <tr>
                          <td className="p-2.5 font-mono font-semibold">Accept</td>
                          <td className="p-2.5 text-muted-foreground">Không</td>
                          <td className="p-2.5 font-mono text-muted-foreground">application/json</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
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
                  Xuất dữ liệu theo định dạng JSON-LD tuân thủ tiêu chuẩn GS1 EPCIS 2.0 (Electronic Product Code Information Services), phục vụ tích hợp tự động với phần mềm ERP quốc tế và hải quan kiểm dịch.
                </p>
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Section 3: Ví dụ gọi thử nghiệm (cURL & Code Samples) */}
        <section id="examples" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Code2 className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              3. Ví dụ Gọi Thử nghiệm (Quickstart Code Samples)
            </h2>
          </div>

          <Card className="bg-card border-border overflow-hidden">
            <CardHeader className="bg-muted/30 pb-3 flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-sm font-semibold">Lệnh gọi API mẫu với khóa Sandbox</CardTitle>
                <CardDescription className="text-xs">
                  Sử dụng lệnh bên dưới trong terminal hoặc chèn vào mã nguồn của bạn để thử nghiệm kết nối ngay.
                </CardDescription>
              </div>

              {/* Tabs chọn ngôn ngữ */}
              <div className="flex items-center gap-1 p-1 bg-muted rounded-lg border border-border">
                <button
                  type="button"
                  onClick={() => setActiveTab('curl')}
                  className={`px-2.5 py-1 rounded text-xs font-semibold transition-colors ${activeTab === 'curl'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                    }`}
                >
                  cURL
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('fetch')}
                  className={`px-2.5 py-1 rounded text-xs font-semibold transition-colors ${activeTab === 'fetch'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                    }`}
                >
                  JavaScript
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('python')}
                  className={`px-2.5 py-1 rounded text-xs font-semibold transition-colors ${activeTab === 'python'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                    }`}
                >
                  Python
                </button>
              </div>
            </CardHeader>

            <CardContent className="p-0 relative">
              <div className="p-4 bg-slate-950 text-slate-100 font-mono text-xs sm:text-sm overflow-x-auto leading-relaxed">
                <pre>
                  {activeTab === 'curl' && sampleCurl}
                  {activeTab === 'fetch' && sampleFetch}
                  {activeTab === 'python' && samplePython}
                </pre>
              </div>
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="absolute top-3 right-3 bg-slate-900/80 hover:bg-slate-800 text-slate-200 border-slate-700 gap-1 text-xs"
                onClick={() => {
                  const textToCopy =
                    activeTab === 'curl'
                      ? sampleCurl
                      : activeTab === 'fetch'
                        ? sampleFetch
                        : samplePython;
                  copyToClipboard(textToCopy, 'code-sample');
                }}
              >
                {copiedSection === 'code-sample' ? (
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
            </CardContent>
          </Card>
        </section>

        {/* Section 4: Ví dụ Response mẫu (Sandbox JSON with is_test: true) */}
        <section id="sample-response" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <FileCode className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              4. Dữ liệu Phản hồi Mẫu (Sandbox Response Payload)
            </h2>
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Định dạng phản hồi: <code className="font-mono text-foreground">application/json</code> (Status: 200 OK)</span>
              <span className="font-medium text-blue-600 dark:text-blue-400">
                ★ Chú ý trường nhận diện: &quot;is_test&quot;: true
              </span>
            </div>

            <div className="relative rounded-xl border border-slate-800 bg-slate-950 p-4 font-mono text-xs sm:text-sm text-slate-200 overflow-x-auto leading-relaxed">
              <Button
                type="button"
                size="sm"
                variant="outline"
                className="absolute top-3 right-3 bg-slate-900/80 hover:bg-slate-800 text-slate-200 border-slate-700 gap-1 text-xs"
                onClick={() => copyToClipboard(sampleJsonResponse, 'json-sample')}
              >
                {copiedSection === 'json-sample' ? (
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
                {sampleJsonResponse}
              </pre>
            </div>
          </div>
        </section>

        {/* Section 5: Bảng ánh xạ trường GS1 */}
        <section id="gs1-mapping" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Layers className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              5. Bảng Ánh xạ Thuộc tính theo Chuẩn GS1 EPCIS
            </h2>
          </div>

          <div className="overflow-x-auto border border-border rounded-xl">
            <table className="w-full text-left text-xs sm:text-sm">
              <thead className="bg-muted text-foreground font-semibold border-b border-border">
                <tr>
                  <th className="p-3">Thuộc tính Nguồn Gốc Số</th>
                  <th className="p-3">Thuộc tính GS1 EPCIS 2.0</th>
                  <th className="p-3">Định dạng / Mã chuẩn</th>
                  <th className="p-3">Mục đích & Ý nghĩa</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-semibold text-foreground">Mã lô sản xuất (GTIN)</td>
                  <td className="p-3 font-mono text-primary font-semibold">epcList / itemGtin</td>
                  <td className="p-3 font-mono text-xs">urn:epc:id:sgtin:8938501...</td>
                  <td className="p-3 text-muted-foreground">Mã định danh sản phẩm theo GS1 toàn cầu</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-semibold text-foreground">Mã vùng trồng / Cơ sở</td>
                  <td className="p-3 font-mono text-primary font-semibold">bizLocation / GLN</td>
                  <td className="p-3 font-mono text-xs">urn:epc:id:sgln:8938501...</td>
                  <td className="p-3 text-muted-foreground">Mã địa điểm toàn cầu xác định tọa độ nông trại, nhà kho</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-semibold text-foreground">Bước nghiệp vụ (Công đoạn)</td>
                  <td className="p-3 font-mono text-primary font-semibold">bizStep</td>
                  <td className="p-3 font-mono text-xs">urn:epcglobal:cbv:bizstep:...</td>
                  <td className="p-3 text-muted-foreground">Thu hoạch (harvesting), Đóng gói (packing), Vận chuyển (shipping)</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-semibold text-foreground">Trạng thái chất lượng</td>
                  <td className="p-3 font-mono text-primary font-semibold">disposition</td>
                  <td className="p-3 font-mono text-xs">urn:epcglobal:cbv:disp:...</td>
                  <td className="p-3 text-muted-foreground">Đạt chuẩn (active/passed), Đang kiểm nghiệm (in_progress)</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-semibold text-foreground">Thời điểm ghi nhận</td>
                  <td className="p-3 font-mono text-primary font-semibold">eventTime</td>
                  <td className="p-3 font-mono text-xs">ISO 8601 (UTC)</td>
                  <td className="p-3 text-muted-foreground">Thời gian chính xác khi sự kiện diễn ra tại nông hộ</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* Section 6: Bảng mã lỗi tổng hợp */}
        <section id="error-codes" className="space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <AlertTriangle className="w-5 h-5 text-primary" />
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">
              6. Bảng Mã Lỗi Tổng hợp (HTTP Error Codes)
            </h2>
          </div>

          <div className="overflow-x-auto border border-border rounded-xl">
            <table className="w-full text-left text-xs sm:text-sm">
              <thead className="bg-muted text-foreground font-semibold border-b border-border">
                <tr>
                  <th className="p-3">Mã HTTP</th>
                  <th className="p-3">Tên lỗi</th>
                  <th className="p-3">Nguyên nhân</th>
                  <th className="p-3">Thông điệp phản hồi gợi ý</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-emerald-600">200</td>
                  <td className="p-3 font-semibold text-foreground">OK</td>
                  <td className="p-3 text-muted-foreground">Yêu cầu thành công, dữ liệu được trả về.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;success&quot;: true</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-amber-600">400</td>
                  <td className="p-3 font-semibold text-foreground">Bad Request</td>
                  <td className="p-3 text-muted-foreground">Thiếu tham số hoặc định dạng dữ liệu không hợp lệ.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;Tham số không hợp lệ&quot;</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-rose-600">401</td>
                  <td className="p-3 font-semibold text-foreground">Unauthorized</td>
                  <td className="p-3 text-muted-foreground">Khóa API không hợp lệ, đã bị thu hồi hoặc đã hết hạn.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;API Key đã hết hạn hoặc không tồn tại&quot;</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-rose-600">403</td>
                  <td className="p-3 font-semibold text-foreground">Forbidden</td>
                  <td className="p-3 text-muted-foreground">Khóa API không có quyền truy cập tài nguyên được yêu cầu.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;Bạn không có quyền truy cập dữ liệu này&quot;</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-amber-600">404</td>
                  <td className="p-3 font-semibold text-foreground">Not Found</td>
                  <td className="p-3 text-muted-foreground">Không tìm thấy lô sản xuất với mã đã chỉ định.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;Không tìm thấy lô sản xuất yêu cầu&quot;</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-purple-600">429</td>
                  <td className="p-3 font-semibold text-foreground">Too Many Requests</td>
                  <td className="p-3 text-muted-foreground">Vượt quá hạn mức số lượt gọi trong 1 giờ quy định.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;Vượt quá hạn mức yêu cầu API&quot;</td>
                </tr>
                <tr className="hover:bg-muted/30">
                  <td className="p-3 font-mono font-bold text-destructive">500</td>
                  <td className="p-3 font-semibold text-foreground">Internal Server Error</td>
                  <td className="p-3 text-muted-foreground">Lỗi hệ thống máy chủ nội bộ.</td>
                  <td className="p-3 font-mono text-xs text-muted-foreground">&quot;Lỗi máy chủ nội bộ. Vui lòng liên hệ quản trị.&quot;</td>
                </tr>
              </tbody>
            </table>
          </div>
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
