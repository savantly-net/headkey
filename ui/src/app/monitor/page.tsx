"use client";

import { SystemMonitor } from "@/components/system-monitor";
import { Separator } from "@/components/ui/separator";
import { Activity } from "lucide-react";

export default function MonitorPage() {
  return (
    <div className="container-padding max-w-7xl mx-auto py-8">
      <div className="glass-container p-6 rounded-soft shadow-medium mb-8">
        <div className="flex items-center mb-4">
          <Activity className="h-6 w-6 mr-2 text-blue-600" />
          <h1 className="text-3xl font-bold">System Monitor</h1>
        </div>
        <p className="text-gray-600">
          View comprehensive system health, performance metrics, and operational statistics.
        </p>
      </div>
      <Separator className="mb-8 divider-soft" />
      
      <div className="panel-soft p-6 rounded-soft">
        <SystemMonitor />
      </div>
    </div>
  );
}