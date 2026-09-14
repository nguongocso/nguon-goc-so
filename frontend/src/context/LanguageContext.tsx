import React, { createContext, useContext, useEffect, useState } from 'react';
import { getTranslation, type Language, type translations } from '@/i18n/translations';

interface LanguageContextType {
  lang: Language;
  setLang: (lang: Language) => void;
  t: (key: keyof typeof translations.vi) => string;
}

const STORAGE_KEY = 'public_lookup_lang';

const LanguageContext = createContext<LanguageContextType>({
  lang: 'vi',
  setLang: () => {},
  t: (key) => getTranslation('vi', key),
});

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [lang, setLangState] = useState<Language>(() => {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved === 'vi' || saved === 'en') {
      return saved;
    }
    // Auto-detect browser language: if starts with 'en' -> 'en', else 'vi'
    if (typeof navigator !== 'undefined' && navigator.language && navigator.language.toLowerCase().startsWith('en')) {
      return 'en';
    }
    return 'vi';
  });

  const setLang = (newLang: Language) => {
    setLangState(newLang);
    sessionStorage.setItem(STORAGE_KEY, newLang);
  };

  useEffect(() => {
    sessionStorage.setItem(STORAGE_KEY, lang);
  }, [lang]);

  const t = (key: keyof typeof translations.vi) => getTranslation(lang, key);

  return (
    <LanguageContext.Provider value={{ lang, setLang, t }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = () => useContext(LanguageContext);
