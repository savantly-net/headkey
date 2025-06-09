import type { Metadata } from "next";
import "./globals.css";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "HeadKey - AI Memory Management System",
  description: "Revolutionary AI memory management platform that enables intelligent information retention, categorization, and retrieval for AI agents. Built on the CIBFE architecture.",
  keywords: "AI memory, memory management, AI agents, cognitive ingestion, belief formation, machine learning, artificial intelligence",
  authors: [{ name: "HeadKey Team" }],
  robots: "index, follow",
  openGraph: {
    title: "HeadKey - AI Memory Management System",
    description: "Revolutionary AI memory management platform for AI agents",
    type: "website",
    locale: "en_US",
  },
  twitter: {
    card: "summary_large_image",
    title: "HeadKey - AI Memory Management System",
    description: "Revolutionary AI memory management platform for AI agents",
  },
};

export function generateViewport() {
  return {
    width: 'device-width',
    initialScale: 1,
  }
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="scroll-smooth">
      <body className="antialiased bg-gray-50 text-gray-900">
        <main className="min-h-screen">
          {children}
        </main>
        <Toaster />
      </body>
    </html>
  );
}