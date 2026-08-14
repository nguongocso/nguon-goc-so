import React from "react";

interface PublicBackgroundProps {
  children: React.ReactNode;
}

export function PublicBackground({ children }: PublicBackgroundProps) {
  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50 via-white to-green-50 flex flex-col items-center justify-center relative overflow-hidden py-10">
      {/* Decorative Background Elements */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute -top-20 -left-20 w-80 h-80 bg-emerald-200/40 rounded-full blur-3xl" />
        <div className="absolute top-1/3 -right-20 w-96 h-96 bg-green-100/50 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-1/4 w-64 h-64 bg-lime-200/30 rounded-full blur-3xl" />
      </div>
      {children}
    </div>
  );
}