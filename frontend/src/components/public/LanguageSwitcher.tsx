import { Globe } from 'lucide-react';

import { useLanguage } from '@/context/LanguageContext';

/**
 * Nút chuyển đổi ngôn ngữ (VI / EN) cho giao diện tra cứu công khai.
 */
export function LanguageSwitcher() {
  const { lang, setLang } = useLanguage();

  return (
    <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg border border-slate-200 text-xs font-medium">
      <Globe className="h-3.5 w-3.5 text-slate-500 ml-1.5 mr-0.5" />
      <button
        type="button"
        onClick={() => setLang('vi')}
        className={`px-2 py-1 rounded-md transition-all ${
          lang === 'vi'
            ? 'bg-white text-emerald-700 font-bold shadow-sm'
            : 'text-slate-600 hover:text-slate-900'
        }`}
        title="Tiếng Việt"
      >
        VI
      </button>
      <span className="text-slate-300">|</span>
      <button
        type="button"
        onClick={() => setLang('en')}
        className={`px-2 py-1 rounded-md transition-all ${
          lang === 'en'
            ? 'bg-white text-emerald-700 font-bold shadow-sm'
            : 'text-slate-600 hover:text-slate-900'
        }`}
        title="English"
      >
        EN
      </button>
    </div>
  );
}

export default LanguageSwitcher;
