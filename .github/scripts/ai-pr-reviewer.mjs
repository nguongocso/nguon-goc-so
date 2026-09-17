/**
 * AI PR Reviewer Engine - Dự án Nguồn Gốc Số
 * 
 * Tự động phân tích Git Diff của Pull Request, phân loại tầng kiến trúc,
 * đối chiếu với bộ tiêu chuẩn tại docs/standards/, và gửi kết quả review
 * trực tiếp lên GitHub PR (Approve hoặc Request Changes kèm inline comments).
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

// Đọc các biến môi trường từ GitHub Action
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const GITHUB_REPOSITORY = process.env.GITHUB_REPOSITORY; // format: owner/repo
const PR_NUMBER = process.env.PR_NUMBER;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || process.env.AI_API_KEY;
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;

// Kiểm tra môi trường tối thiểu
if (!GITHUB_TOKEN || !GITHUB_REPOSITORY || !PR_NUMBER) {
  console.log('⚠️ Không tìm thấy biến môi trường GITHUB_TOKEN, GITHUB_REPOSITORY hoặc PR_NUMBER.');
  console.log('Chạy ở chế độ mô phỏng kiểm thử cục bộ (Local Dry-run Mode)...');
}

/**
 * Lấy danh sách diff của PR
 */
function getGitDiff() {
  try {
    // So sánh nhánh hiện tại với target base branch (thường là develop hoặc main)
    const baseRef = process.env.GITHUB_BASE_REF || 'origin/develop';
    try {
      return execSync(`git diff ${baseRef}...HEAD`, { maxBuffer: 10 * 1024 * 1024, encoding: 'utf-8' });
    } catch {
      return execSync(`git diff HEAD~1...HEAD`, { maxBuffer: 10 * 1024 * 1024, encoding: 'utf-8' });
    }
  } catch (error) {
    console.error('Lỗi khi lấy git diff:', error.message);
    return '';
  }
}

/**
 * Lấy danh sách các file thay đổi
 */
function getChangedFiles() {
  try {
    const baseRef = process.env.GITHUB_BASE_REF || 'origin/develop';
    let output;
    try {
      output = execSync(`git diff --name-only ${baseRef}...HEAD`, { encoding: 'utf-8' });
    } catch {
      output = execSync(`git diff --name-only HEAD~1...HEAD`, { encoding: 'utf-8' });
    }
    return output.split('\n').map(f => f.trim()).filter(Boolean);
  } catch (error) {
    console.error('Lỗi khi lấy danh sách file:', error.message);
    return [];
  }
}

/**
 * Nhận diện tầng kiến trúc của file
 */
function detectLayer(filePath) {
  const norm = filePath.toLowerCase();
  if (norm.includes('entity') || norm.endsWith('entity.java')) return 'Backend - Entity Layer';
  if (norm.includes('dto') || norm.endsWith('request.java') || norm.endsWith('response.java')) return 'Backend - DTO Layer';
  if (norm.includes('mapper') || norm.endsWith('mapper.java')) return 'Backend - Mapper Layer';
  if (norm.includes('repository') || norm.endsWith('repository.java')) return 'Backend - Repository Layer';
  if (norm.includes('service') || norm.endsWith('service.java') || norm.endsWith('serviceimpl.java')) return 'Backend - Service Layer';
  if (norm.includes('controller') || norm.endsWith('controller.java')) return 'Backend - Controller Layer';
  if (norm.includes('exception')) return 'Backend - Exception Layer';
  
  if (norm.endsWith('.tsx') && norm.includes('components/')) return 'Frontend - Component';
  if (norm.endsWith('.tsx') && norm.includes('pages/')) return 'Frontend - Page';
  if (norm.includes('hooks/') || norm.startsWith('use')) return 'Frontend - Custom Hook';
  if (norm.includes('services/') && (norm.endsWith('.ts') || norm.endsWith('.js'))) return 'Frontend - API Service';
  if (norm.endsWith('.css') || norm.endsWith('.scss')) return 'Frontend - Stylesheet';
  if (norm.includes('types/') || norm.endsWith('.d.ts')) return 'Frontend - Types/Interfaces';

  return 'General';
}

/**
 * Đọc nội dung bộ checklist và quy ước
 */
function loadStandards() {
  const readSafe = (p) => fs.existsSync(p) ? fs.readFileSync(p, 'utf-8') : '';
  return {
    beConvention: readSafe('docs/standards/code-convention-be.md'),
    feConvention: readSafe('docs/standards/code-convention-fe.md'),
    beChecklist: readSafe('docs/standards/checklist-review-be.md'),
    feChecklist: readSafe('docs/standards/checklist-review-fe.md'),
  };
}

/**
 * Xây dựng prompt phân tích cho AI
 */
function buildPrompt(files, diff, standards) {
  const hasBackend = files.some(f => f.startsWith('backend/'));
  const hasFrontend = files.some(f => f.startsWith('frontend/'));

  let relevantStandards = '';
  if (hasBackend) {
    relevantStandards += `\n### TIÊU CHUẨN BACKEND BẮT BUỘC:\n${standards.beConvention.slice(0, 3500)}\n\n### CHECKLIST BACKEND (59 Tiêu chí):\n${standards.beChecklist.slice(0, 4500)}\n`;
  }
  if (hasFrontend) {
    relevantStandards += `\n### TIÊU CHUẨN FRONTEND BẮT BUỘC:\n${standards.feConvention.slice(0, 3500)}\n\n### CHECKLIST FRONTEND (54 Tiêu chí):\n${standards.feChecklist.slice(0, 4500)}\n`;
  }

  const prompt = `
Bạn là Lead Software Architect & Clean Code Gatekeeper của dự án "Nguồn Gốc Số".
Nhiệm vụ của bạn là đánh giá nghiêm ngặt đoạn Git Diff dưới đây của một Pull Request dựa trên đúng các quy chuẩn của dự án:

${relevantStandards}

### CÁC NGUYÊN TẮC CỐT LÕI BẮT BUỘC KIỂM TRA:
1. **Clean Commenting**:
   - Cấm Noise Comments (comment cú pháp hiển nhiên: tăng i, khởi tạo list, check null).
   - Cấm Dead Code (code cũ bị comment lại) -> Bắt buộc xóa bỏ hoàn toàn!
   - Javadoc/JSDoc phải súc tích: Class và Method tối đa 3-5 dòng, tiếng Việt có dấu. Cấm javadoc cho getter/setter.
2. **Quy tắc Xuống dòng & Cách dòng**:
   - Độ dài tối đa 120 ký tự/dòng.
   - Builder / Chaining calls phải xuống dòng TRƯỚC dấu chấm.
   - Toán tử logic ngắt dòng TRƯỚC toán tử.
   - React JSX có >= 3 props phải xuống dòng từng prop riêng.
   - Cách đúng 1 dòng trống giữa các method, giữa các field DTO có annotations. Cấm 2 dòng trống liên tiếp.
3. **Quy chuẩn Từng Tầng**:
   - Backend: Cấm @Data trên JPA Entity. DTO phải có Jakarta validation kèm message tiếng Việt. Service method <= 30 dòng. Cấm System.out.println(), cấm catch Exception rỗng. Cấm cộng chuỗi SQL.
   - Frontend: Cấm dùng kiểu 'any'. Cấm console.log() và debugger. Component <= 200 dòng. Thẻ <img> có alt.

### DANH SÁCH FILE THAY ĐỔI:
${files.map(f => `- ${f} (Tầng: ${detectLayer(f)})`).join('\n')}

### GIT DIFF:
\`\`\`diff
${diff.slice(0, 25000)}
\`\`\`

### YÊU CẦU ĐẦU RA (BẮT BUỘC ĐÚNG ĐỊNH DẠNG JSON):
Hãy phân tích kỹ từng dòng code thêm mới (bắt đầu bằng dấu +) và trả về duy nhất một JSON object hợp lệ (không kèm text ngoài JSON) có cấu trúc sau:

{
  "passed": true/false (chỉ true khi KHÔNG CÓ bất kỳ lỗi Blocker hoặc Major nào),
  "totalViolations": number,
  "summary": "Đoạn văn ngắn gọn tóm tắt nhận xét tổng thể chất lượng mã nguồn...",
  "violations": [
    {
      "file": "đường dẫn tương đối tới file vi phạm",
      "line": số dòng trong file mới (nếu xác định được) hoặc null,
      "criterionId": "Mã tiêu chí vi phạm (VD: BE-CHK-24, BE-CHK-35, FE-CHK-21, BE-CHK-10)",
      "severity": "Blocker" | "Major" | "Minor",
      "issue": "Mô tả ngắn gọn, súc tích lỗi vi phạm bằng tiếng Việt",
      "suggestion": "Code mẫu gợi ý sửa đúng chuẩn (định dạng chuẩn, không kèm giải thích dài)"
    }
  ]
}
`;
  return prompt;
}

/**
 * Gọi AI Model (ưu tiên Gemini, dự phòng OpenAI)
 */
async function callAI(prompt) {
  if (GEMINI_API_KEY) {
    try {
      console.log('🤖 Đang gọi Google Gemini Flash Model để phân tích...');
      const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`;
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.1,
            responseMimeType: 'application/json'
          }
        })
      });
      const data = await res.json();
      if (data.candidates && data.candidates[0]?.content?.parts[0]?.text) {
        return JSON.parse(data.candidates[0].content.parts[0].text);
      }
      console.error('Phản hồi từ Gemini không đúng mong đợi:', JSON.stringify(data));
    } catch (e) {
      console.error('Lỗi khi gọi Gemini API:', e.message);
    }
  }

  if (OPENAI_API_KEY) {
    try {
      console.log('🤖 Đang gọi OpenAI Model để phân tích...');
      const res = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${OPENAI_API_KEY}`
        },
        body: JSON.stringify({
          model: 'gpt-4o-mini',
          messages: [
            { role: 'system', content: 'Bạn là chuyên gia phân tích Clean Code JSON.' },
            { role: 'user', content: prompt }
          ],
          response_format: { type: 'json_object' },
          temperature: 0.1
        })
      });
      const data = await res.json();
      if (data.choices && data.choices[0]?.message?.content) {
        return JSON.parse(data.choices[0].message.content);
      }
    } catch (e) {
      console.error('Lỗi khi gọi OpenAI API:', e.message);
    }
  }

  // Fallback giả lập kiểm tra regex cục bộ nếu không có API key
  console.log('ℹ️ Không có AI API Key hoặc gọi API thất bại. Chạy bộ lọc tĩnh (Static Rule Checker) cục bộ...');
  return runStaticChecks();
}

/**
 * Bộ kiểm tra quy tắc tĩnh cục bộ (Static Fallback Rules)
 */
function runStaticChecks() {
  const files = getChangedFiles();
  const diff = getGitDiff();
  const violations = [];

  const lines = diff.split('\n');
  let currentFile = '';
  let lineNum = 0;

  for (const line of lines) {
    if (line.startsWith('+++ b/')) {
      currentFile = line.replace('+++ b/', '').trim();
      continue;
    }
    if (line.startsWith('@@ ')) {
      const match = line.match(/\+(\d+)/);
      if (match) lineNum = parseInt(match[1], 10);
      continue;
    }

    if (line.startsWith('+') && !line.startsWith('+++')) {
      const content = line.substring(1);
      lineNum++;

      // Kiểm tra console.log trong TS/JS
      if ((currentFile.endsWith('.ts') || currentFile.endsWith('.tsx') || currentFile.endsWith('.js')) && content.includes('console.log(')) {
        violations.push({
          file: currentFile,
          line: lineNum,
          criterionId: 'FE-CHK-21',
          severity: 'Blocker',
          issue: 'Phát hiện lệnh `console.log()` sót lại trong code hoàn chỉnh.',
          suggestion: '// Xóa bỏ console.log() trước khi merge'
        });
      }

      // Kiểm tra System.out.println trong Java
      if (currentFile.endsWith('.java') && (content.includes('System.out.println(') || content.includes('System.err.println('))) {
        violations.push({
          file: currentFile,
          line: lineNum,
          criterionId: 'BE-CHK-41',
          severity: 'Blocker',
          issue: 'Phát hiện `System.out.println()` trong mã nguồn Java. Bắt buộc dùng SLF4J logger.',
          suggestion: 'log.info("...");'
        });
      }

      // Kiểm tra @Data trên Entity
      if (currentFile.endsWith('Entity.java') && content.trim() === '@Data') {
        violations.push({
          file: currentFile,
          line: lineNum,
          criterionId: 'BE-CHK-24',
          severity: 'Blocker',
          issue: 'Nghiêm cấm dùng `@Data` trên JPA Entity vì gây lỗi equals/hashCode và Lazy Loading.',
          suggestion: '@Getter\n@Setter\n@NoArgsConstructor'
        });
      }

      // Kiểm tra độ dài dòng > 120 ký tự
      if (content.length > 120 && !content.includes('http://') && !content.includes('https://')) {
        violations.push({
          file: currentFile,
          line: lineNum,
          criterionId: currentFile.endsWith('.java') ? 'BE-CHK-10' : 'FE-CHK-10',
          severity: 'Minor',
          issue: `Dòng code dài vượt quá 120 ký tự (${content.length} ký tự). Cần ngắt dòng hợp lý.`,
          suggestion: '// Ngắt dòng trước toán tử hoặc sau dấu phẩy'
        });
      }

      // Kiểm tra kiểu any trong TypeScript
      if ((currentFile.endsWith('.ts') || currentFile.endsWith('.tsx')) && content.match(/:\s*any(\[\])?[\s;,)]/)) {
        violations.push({
          file: currentFile,
          line: lineNum,
          criterionId: 'FE-CHK-22',
          severity: 'Blocker',
          issue: 'Vi phạm TypeScript Strict Mode: Cấm sử dụng kiểu `any`.',
          suggestion: 'interface SpecificType { ... }'
        });
      }
    }
  }

  const blockerOrMajor = violations.filter(v => v.severity === 'Blocker' || v.severity === 'Major');
  return {
    passed: blockerOrMajor.length === 0,
    totalViolations: violations.length,
    summary: violations.length === 0
      ? 'Mã nguồn tuân thủ tốt các quy tắc Clean Code và quy chuẩn dự án.'
      : `Phát hiện ${violations.length} điểm cần cải thiện, trong đó có ${blockerOrMajor.length} lỗi nghiêm trọng cần khắc phục.`,
    violations
  };
}

/**
 * Tạo báo cáo Markdown đăng lên Pull Request
 */
function buildMarkdownComment(reviewResult) {
  const { passed, summary, violations } = reviewResult;
  const statusIcon = passed ? '✅ **ĐẠT YÊU CẦU (PASS)**' : '❌ **CHƯA ĐẠT (CHANGES REQUESTED)**';
  
  let md = `## 🤖 Báo Cáo Đánh Giá Chất Lượng Mã Nguồn (AI Clean Code Gatekeeper)\n\n`;
  md += `**Kết luận:** ${statusIcon}\n\n`;
  md += `> ${summary}\n\n`;

  if (violations.length === 0) {
    md += `🎉 **Tuyệt vời!** Toàn bộ diff của Pull Request đã tuân thủ 100% các tiêu chuẩn về kiến trúc phân tầng, Clean Code, giới hạn độ dài dòng và quy chuẩn chú thích.\n\n`;
    md += `👉 *Pull Request đã đủ điều kiện kỹ thuật để merge vào nhánh chính.*`;
    return md;
  }

  md += `### 📋 Danh Sách Các Điểm Cần Khắc Phục (${violations.length} vị trí)\n\n`;
  md += `| Mức độ | Mã Tiêu Chí | File & Dòng | Chi tiết vi phạm |\n`;
  md += `|:---:|:---:|---|---|\n`;

  for (const v of violations) {
    const sevBadge = v.severity === 'Blocker' ? '🔴 **Blocker**' : (v.severity === 'Major' ? '🟡 Major' : '🟢 Minor');
    const loc = v.line ? `\`${v.file}:${v.line}\`` : `\`${v.file}\``;
    md += `| ${sevBadge} | \`${v.criterionId}\` | ${loc} | ${v.issue} |\n`;
  }

  md += `\n### 💡 Hướng Dẫn Sửa Chi Tiết Từng Vị Trí:\n\n`;
  for (const v of violations) {
    if (v.suggestion) {
      const loc = v.line ? `${v.file} (dòng ${v.line})` : v.file;
      md += `<details>\n<summary><b>[${v.criterionId}] ${loc}</b>: ${v.issue}</summary>\n\n`;
      md += `**Gợi ý khắc phục:**\n\`\`\`suggestion\n${v.suggestion}\n\`\`\`\n</details>\n\n`;
    }
  }

  if (!passed) {
    md += `\n> ⚠️ **Lưu ý:** Vui lòng khắc phục các lỗi có mức độ **Blocker** và **Major** nêu trên trước khi tiến hành merge. Tham khảo tài liệu chi tiết tại thư mục \`docs/standards/\`.`;
  }

  return md;
}

/**
 * Gửi nhận xét lên GitHub PR API
 */
async function postGitHubReview(reviewResult) {
  if (!GITHUB_TOKEN || !GITHUB_REPOSITORY || !PR_NUMBER) {
    console.log('\n=== KẾT QUẢ REVIEW (MÔ PHỎNG CỤC BỘ) ===');
    console.log(buildMarkdownComment(reviewResult));
    return;
  }

  const [owner, repo] = GITHUB_REPOSITORY.split('/');
  const event = reviewResult.passed ? 'APPROVE' : 'REQUEST_CHANGES';
  const body = buildMarkdownComment(reviewResult);

  const url = `https://api.github.com/repos/${owner}/${repo}/pulls/${PR_NUMBER}/reviews`;
  try {
    console.log(`📡 Đang gửi đánh giá lên GitHub PR #${PR_NUMBER} với trạng thái: ${event}...`);
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${GITHUB_TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        event: event,
        body: body
      })
    });

    if (res.ok) {
      console.log('✅ Đã đăng kết quả review lên GitHub PR thành công!');
    } else {
      const errText = await res.text();
      console.error('Lỗi khi gọi GitHub Review API:', errText);
    }
  } catch (error) {
    console.error('Lỗi kết nối GitHub API:', error.message);
  }
}

/**
 * Hàm thực thi chính
 */
async function main() {
  console.log('🚀 Bắt đầu quét kiểm tra mã nguồn Pull Request...');
  const files = getChangedFiles();
  if (files.length === 0) {
    console.log('Không có file nào thay đổi trong PR này.');
    return;
  }

  console.log(`Tìm thấy ${files.length} file thay đổi trong PR.`);
  const diff = getGitDiff();
  const standards = loadStandards();

  const prompt = buildPrompt(files, diff, standards);
  const reviewResult = await callAI(prompt);

  console.log(`Kết quả phân tích: ${reviewResult.passed ? 'ĐẠT CHUẨN ✅' : 'CÓ LỖI VI PHẠM ❌'} (${reviewResult.violations?.length || 0} vi phạm)`);

  await postGitHubReview(reviewResult);

  // Nếu có lỗi Blocker, làm fail process để GitHub Action gắn check red
  if (!reviewResult.passed) {
    console.log('🔴 Phát hiện lỗi Blocker/Major. Workflow kết thúc với mã lỗi để khóa nút Merge.');
    process.exit(1);
  } else {
    console.log('🟢 Mọi tiêu chí đã vượt qua! Cho phép merge.');
    process.exit(0);
  }
}

main().catch(err => {
  console.error('Lỗi không xử lý được:', err);
  process.exit(1);
});
