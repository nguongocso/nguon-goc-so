import React from 'react';
import { useLanguage } from '@/context/LanguageContext';
import { Globe } from 'lucide-react';

export const LanguageSwitcher: React.FC = () => {
  const { lang, setLang } = useLanguage();

  return (
    <div className="inline-flex items-center gap-0.5 rounded-full bg-slate-100/90 p-0.5 border border-slate-200 shadow-2xs">
      <div className="pl-2 pr-0.5 text-slate-400 select-none">
        <Globe className="size-3.5" />
      </div>
      <button
        type="button"
        onClick={() => setLang('vi')}
        className={`rounded-full px-2.5 py-1 text-xs font-semibold transition-all duration-150 ${
          lang === 'vi'
            ? 'bg-white text-emerald-700 shadow-xs scale-100 ring-1 ring-slate-200/60'
            : 'text-slate-500 hover:text-slate-800'
        }`}
        title="Tiếng Việt"
      >
        VI
      </button>
      <button
        type="button"
        onClick={() => setLang('en')}
        className={`rounded-full px-2.5 py-1 text-xs font-semibold transition-all duration-150 ${
          lang === 'en'
            ? 'bg-white text-emerald-700 shadow-xs scale-100 ring-1 ring-slate-200/60'
            : 'text-slate-500 hover:text-slate-800'
        }`}
        title="English"
      >
        EN
      </button>
    </div>
  );
};
