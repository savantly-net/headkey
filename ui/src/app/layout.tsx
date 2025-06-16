import type { Metadata } from "next";
import "./globals.css";
import { Toaster } from "@/components/toast-config";
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";

export const metadata: Metadata = {
  title: "HeadKey - AI Memory Management System",
  description:
    "Revolutionary AI memory management platform that enables intelligent information retention, categorization, and retrieval for AI agents. Built on the CIBFE architecture.",
  keywords:
    "AI memory, memory management, AI agents, cognitive ingestion, belief formation, machine learning, artificial intelligence",
  authors: [{ name: "Savantly Team" }, { name: "Jeremy Branham" }],
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
    width: "device-width",
    initialScale: 1,
  };
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="scroll-smooth">
      <body className="antialiased bg-gray-50 text-gray-900">
        <SidebarProvider>
          <div className="min-h-screen flex w-full">
            <AppSidebar />
            <main className="flex-1 flex flex-col">
              <div className="sticky flex items-center gap-4 p-4 border-b border-gray-200 bg-white">
                <SidebarTrigger />
                <h3 className="font-semibold">
                  HeadKey - AI Memory Management
                </h3>
              </div>
              <div className="flex-1 p-6">{children}</div>
            </main>
            <Toaster />
          </div>
        </SidebarProvider>
      </body>
    </html>
  );
}
