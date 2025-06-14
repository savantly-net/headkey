"use client";

import { SystemMonitor } from "@/components/system-monitor";

export default function MonitorPage() {
  return (
    <div className="container-padding max-w-7xl mx-auto">
      <div className="p-6 mb-8">
        <SystemMonitor />
      </div>
    </div>
  );
}