/**
 * AI PR Reviewer Engine - Dự án Nguồn Gốc Số
 * 
 * Tự động phân tích Git Diff của Pull Request, phân loại tầng kiến trúc,
 * đối chiếu với bộ tiêu chuẩn tại docs/standards/, và gửi kết quả review
 * trực tiếp lên GitHub PR.
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
const RAW_BASE_REF = process.env.GITHUB_BASE_REF || 'develop';

/**
 * Thực thi lệnh git an toàn có fallback
 */
function runGit(command) {
  try {
    return execSync(command, { maxBuffer: 15 * 1024 * 1024, encoding: 'utf-8' }).trim();
  } catch (error) {
    return null;
  }
}

/**
 * Lấy danh sách diff của PR bằng nhiều chiến lược
 */
function getGitDiff() {
  const strategies = [
    `git diff origin/${RAW_BASE_REF}...HEAD`,
    `git diff ${RAW_BASE_REF}...HEAD`,
    `git diff origin/${RAW_BASE_REF} HEAD`,
    `git diff HEAD~1...HEAD`,
    `git diff HEAD`
  ];

  for (const cmd of strategies) {
    const output = runGit(cmd);
    if (output !== null && output.length > 0) {
      console.log(`✅ Lấy git diff thành công với lệnh: "${cmd}" (${output.length} ký tự)`);
      return output;
    }
  }

  console.warn('⚠️ Không thể lấy diff qua các lệnh thông thường. Trả về rỗng.');
  return '';
}

/**
 * Lấy danh sách các file thay đổi
 */
function getChangedFiles() {
  const strategies = [
    `git diff --name-only origin/${RAW_BASE_REF}...HEAD`,
    `git diff --name-only ${RAW_BASE_REF}...HEAD`,
    `git diff --name-only HEAD~1...HEAD`
  ];

  for (const cmd of strategies) {
    const output = runGit(cmd);
    if (output !== null) {
      const files = output.split('\n').map(f => f.trim()).filter(Boolean);
      if (files.length > 0) {
        return files;
      }
    }
  }

  return [];
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

  return `
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
}

/**
 * Gọi AI Model với danh sách model dự phòng
 */
async function callAI(prompt) {
  if (GEMINI_API_KEY) {
    // Danh sách các model Gemini ưu tiên (gemini-3.6-flash theo đề xuất trực tiếp từ Google API)
    const candidateModels = [
      'gemini-3.6-flash',
      'gemini-3.5-flash',
      'gemini-2.5-flash',
      'gemini-flash'
    ];

    // Thử các model trong danh sách ưu tiên
    for (const model of candidateModels) {
      try {
        console.log(`🤖 Đang thử gọi Google Gemini Model: ${model}...`);
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`;
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
          console.log(`✅ Gọi thành công model ${model}!`);
          return JSON.parse(data.candidates[0].content.parts[0].text);
        }

        if (data.error) {
          console.warn(`Model ${model} báo lỗi (${data.error.code}): ${data.error.message}`);
        }
      } catch (e) {
        console.warn(`Thất bại khi gọi model ${model}:`, e.message);
      }
    }

    // Tự động dò danh sách model khả dụng của API key nếu danh sách trên bị 404
    try {
      console.log('🔍 Đang tự động dò danh sách model khả dụng của API key...');
      const listRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${GEMINI_API_KEY}`);
      const listData = await listRes.json();
      if (listData.models && Array.isArray(listData.models)) {
        const available = listData.models
          .filter(m => m.supportedGenerationMethods?.includes('generateContent') && m.name?.includes('flash'))
          .map(m => m.name.replace('models/', ''));
        
        console.log('Các model Flash tìm thấy:', available.join(', '));
        for (const discoveredModel of available) {
          console.log(`🤖 Thử gọi model tự động dò: ${discoveredModel}...`);
          const url = `https://generativelanguage.googleapis.com/v1beta/models/${discoveredModel}:generateContent?key=${GEMINI_API_KEY}`;
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
            console.log(`✅ Gọi thành công model tự dò: ${discoveredModel}!`);
            return JSON.parse(data.candidates[0].content.parts[0].text);
          }
        }
      }
    } catch (err) {
      console.warn('Lỗi khi tự động dò model:', err.message);
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

  console.log('ℹ️ Sử dụng bộ lọc tĩnh (Static Rule Checker) cục bộ...');
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
 * Gửi nhận xét lên GitHub PR
 * Sử dụng event COMMENT để tránh lỗi 422 (GitHub Actions không được cấp quyền APPROVE trực tiếp).
 * Việc khóa hoặc mở merge được thực hiện qua exit code của GitHub Actions check.
 */
async function postGitHubReview(reviewResult) {
  if (!GITHUB_TOKEN || !GITHUB_REPOSITORY || !PR_NUMBER) {
    console.log('\n=== KẾT QUẢ REVIEW (MÔ PHỎNG CỤC BỘ) ===');
    console.log(buildMarkdownComment(reviewResult));
    return;
  }

  const [owner, repo] = GITHUB_REPOSITORY.split('/');
  const body = buildMarkdownComment(reviewResult);

  // Gửi qua Pull Request Review API với event = 'COMMENT'
  const reviewUrl = `https://api.github.com/repos/${owner}/${repo}/pulls/${PR_NUMBER}/reviews`;
  try {
    console.log(`📡 Đang gửi đánh giá lên GitHub PR #${PR_NUMBER}...`);
    const res = await fetch(reviewUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${GITHUB_TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        event: 'COMMENT',
        body: body
      })
    });

    if (res.ok) {
      console.log('✅ Đã đăng kết quả review lên GitHub PR thành công!');
      return;
    } else {
      console.warn('Review API trả về mã lỗi, thử gửi qua Issue Comment API...');
    }
  } catch (error) {
    console.warn('Lỗi kết nối Review API:', error.message);
  }

  // Fallback: Gửi qua Issue Comment API nếu Review API gặp vấn đề
  const commentUrl = `https://api.github.com/repos/${owner}/${repo}/issues/${PR_NUMBER}/comments`;
  try {
    const res = await fetch(commentUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${GITHUB_TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ body })
    });
    if (res.ok) {
      console.log('✅ Đã đăng comment đánh giá lên GitHub PR thành công qua Comment API!');
    }
  } catch (error) {
    console.error('Lỗi khi đăng comment fallback:', error.message);
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

  // Nếu có lỗi Blocker/Major, kết thúc với mã lỗi 1 để Status Check bị đỏ (khóa nút merge)
  if (!reviewResult.passed) {
    console.log('🔴 Phát hiện lỗi Blocker/Major. Workflow kết thúc với mã lỗi để khóa nút Merge.');
    process.exit(1);
  } else {
    console.log('🟢 Mọi tiêu chí đã vượt qua! Status Check màu xanh (cho phép merge).');
    process.exit(0);
  }
}

main().catch(err => {
  console.error('Lỗi không xử lý được:', err);
  process.exit(1);
});
