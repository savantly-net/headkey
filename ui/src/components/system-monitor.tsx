"use client";

import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

import { Separator } from "@/components/ui/separator";
import { toast } from "sonner";
import {
  systemApi,
  handleApiError,
  type SystemHealth,
  type SystemStatistics,
} from "@/lib/api";
import {
  Activity,
  Database,
  CheckCircle,
  AlertCircle,
  XCircle,
  RefreshCw,
  TrendingUp,
  Users,
  Brain,
  Zap,
  BarChart3,
  Settings,
} from "lucide-react";

interface SystemMonitorProps {
  autoRefresh?: boolean;
  refreshInterval?: number; // in milliseconds
}

export function SystemMonitor({
  autoRefresh = true,
  refreshInterval = 30000,
}: SystemMonitorProps) {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [statistics, setStatistics] = useState<SystemStatistics | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  const fetchSystemData = async () => {
    setIsLoading(true);
    try {
      const [healthData, statsData] = await Promise.all([
        systemApi.getComprehensiveHealth(),
        systemApi.getStatistics(),
      ]);

      setHealth(healthData);
      setStatistics(statsData);
      setLastUpdate(new Date());
    } catch (error) {
      console.error("Failed to fetch system data:", error);
      const apiError = handleApiError(error);
      toast.error(`Failed to fetch system data: ${apiError.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSystemData();

    if (autoRefresh) {
      const interval = setInterval(fetchSystemData, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [autoRefresh, refreshInterval]);

  const getStatusIcon = (status: "UP" | "DOWN") => {
    switch (status) {
      case "UP":
        return <CheckCircle className="h-5 w-5 text-green-600" />;
      case "DOWN":
        return <XCircle className="h-5 w-5 text-red-600" />;
      default:
        return <AlertCircle className="h-5 w-5 text-yellow-600" />;
    }
  };

  const getStatusBadge = (status: "UP" | "DOWN") => {
    const variant = status === "UP" ? "default" : "destructive";
    const className =
      status === "UP"
        ? "bg-green-100 text-green-800 border-green-200"
        : "bg-red-100 text-red-800 border-red-200";

    return (
      <Badge variant={variant} className={className}>
        {status}
      </Badge>
    );
  };

  const formatNumber = (num: number | undefined): string => {
    if (num === undefined || num === null) return "0";
    if (num >= 1000000) {
      return `${(num / 1000000).toFixed(1)}M`;
    } else if (num >= 1000) {
      return `${(num / 1000).toFixed(1)}K`;
    }
    return num.toString();
  };

  const formatDuration = (seconds: number | undefined): string => {
    if (seconds === undefined || seconds === null) return "0s";
    if (seconds < 60) {
      return `${seconds}s`;
    } else if (seconds < 3600) {
      return `${(seconds / 60).toFixed(1)}m`;
    }
    return `${(seconds / 3600).toFixed(1)}h`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold flex items-center gap-2">
            <Activity className="h-6 w-6" />
            System Monitor
          </h2>
          {lastUpdate && (
            <p className="text-sm text-gray-500 mt-1">
              Last updated: {lastUpdate.toLocaleTimeString()}
            </p>
          )}
        </div>
        <Button
          onClick={fetchSystemData}
          disabled={isLoading}
          variant="outline"
          size="sm"
          className="button-soft"
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${isLoading ? "animate-spin" : ""}`}
          />
          Refresh
        </Button>
      </div>

      {/* System Health */}
      <Card className="card-soft shadow-soft border-soft">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {health && getStatusIcon(health.status)}
            System Health
          </CardTitle>
          <CardDescription>
            Overall system status and component health
          </CardDescription>
        </CardHeader>
        <CardContent>
          {health ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="font-medium">Overall Status</span>
                {getStatusBadge(health.status)}
              </div>

              {health.components && (
                <div className="space-y-3">
                  <Separator className="divider-soft" />
                  <h4 className="font-medium">Components</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {Object.entries(health.components).map(
                      ([name, component]) => (
                        <div
                          key={name}
                          className="flex items-center justify-between p-3 bg-gray-50 rounded-soft shadow-soft border-soft"
                        >
                          <div className="flex items-center gap-2">
                            {getStatusIcon(component.status)}
                            <span className="text-sm font-medium capitalize">
                              {name.replace("_", " ")}
                            </span>
                          </div>
                          {getStatusBadge(component.status)}
                        </div>
                      ),
                    )}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center justify-center py-8">
              {isLoading ? (
                <div className="flex items-center gap-2">
                  <RefreshCw className="h-4 w-4 animate-spin" />
                  <span>Loading health data...</span>
                </div>
              ) : (
                <span className="text-gray-500">No health data available</span>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* System Statistics */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total Memories */}
          <Card className="card-soft shadow-soft border-soft">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Brain className="h-4 w-4 text-blue-600" />
                Total Memories
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {formatNumber(statistics.memorySystem.totalMemories)}
              </div>
              <p className="text-xs text-gray-500 mt-1">Stored in system</p>
            </CardContent>
          </Card>

          {/* Total Operations */}
          <Card className="card-soft shadow-soft border-soft">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Users className="h-4 w-4 text-green-600" />
                Total Operations
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {formatNumber(statistics.memorySystem.totalOperations)}
              </div>
              <p className="text-xs text-gray-500 mt-1">System operations</p>
            </CardContent>
          </Card>

          {/* Uptime */}
          <Card className="card-soft shadow-soft border-soft">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-purple-600" />
                System Uptime
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {formatDuration(statistics.memorySystem.uptimeSeconds)}
              </div>
              <p className="text-xs text-gray-500 mt-1">Running time</p>
            </CardContent>
          </Card>

          {/* Cache Status */}
          <Card className="card-soft shadow-soft border-soft">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Zap className="h-4 w-4 text-yellow-600" />
                Second Level Cache
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {statistics.memorySystem.secondLevelCacheEnabled
                  ? "Enabled"
                  : "Disabled"}
              </div>
              <p className="text-xs text-gray-500 mt-1">Cache status</p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* System Information */}
      {statistics && (
        <Card className="card-soft shadow-medium border-soft">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5" />
              System Information
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {/* Database Information */}
              <div className="space-y-3">
                <h4 className="font-medium flex items-center gap-2">
                  <Database className="h-4 w-4" />
                  Database
                </h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Type</span>
                    <span className="font-medium capitalize">
                      {statistics.database.configuredDatabaseKind}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">
                      Entity Manager
                    </span>
                    <span className="font-medium">
                      {statistics.database.entityManagerFactoryOpen
                        ? "Open"
                        : "Closed"}
                    </span>
                  </div>
                </div>
              </div>

              {/* Memory System Stats */}
              <div className="space-y-3">
                <h4 className="font-medium flex items-center gap-2">
                  <TrendingUp className="h-4 w-4" />
                  Memory Operations
                </h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">
                      Total Searches
                    </span>
                    <span className="font-medium">
                      {formatNumber(statistics.memorySystem.totalSearches)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Total Updates</span>
                    <span className="font-medium">
                      {formatNumber(statistics.memorySystem.totalUpdates)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Total Deletes</span>
                    <span className="font-medium">
                      {formatNumber(statistics.memorySystem.totalDeletes)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Search Strategy */}
              <div className="space-y-3">
                <h4 className="font-medium flex items-center gap-2">
                  <Zap className="h-4 w-4" />
                  Search Strategy
                </h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Strategy</span>
                    <span className="font-medium text-xs">
                      {statistics.strategy.name}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Vector Search</span>
                    <span className="font-medium">
                      {statistics.strategy.supportsVectorSearch
                        ? "Supported"
                        : "Not Supported"}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Max Results</span>
                    <span className="font-medium">
                      {formatNumber(
                        statistics.memorySystem.maxSimilaritySearchResults,
                      )}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">
                      Similarity Threshold
                    </span>
                    <span className="font-medium">
                      {statistics.memorySystem.similarityThreshold}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Batch Size</span>
                    <span className="font-medium">
                      {formatNumber(statistics.memorySystem.batchSize)}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* System Configuration Details */}
      {statistics && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="card-soft shadow-soft border-soft">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Settings className="h-5 w-5" />
                Configuration
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="bg-gray-50 p-3 rounded-soft shadow-soft border-soft">
                  <h5 className="font-medium mb-2">System Start Time</h5>
                  <p className="text-sm text-gray-600">
                    {new Date(
                      statistics.memorySystem.startTime,
                    ).toLocaleString()}
                  </p>
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="font-medium">Managed Types:</span>
                    <span className="ml-2">
                      {statistics.memorySystem.managedTypes}
                    </span>
                  </div>
                  <div>
                    <span className="font-medium">Cache Enabled:</span>
                    <span className="ml-2">
                      {statistics.memorySystem.secondLevelCacheEnabled
                        ? "Yes"
                        : "No"}
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="card-soft shadow-soft border-soft">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Database className="h-5 w-5" />
                Database Status
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="font-medium">Connection Status</span>
                  <Badge
                    variant={
                      statistics.database.entityManagerFactoryOpen
                        ? "default"
                        : "destructive"
                    }
                  >
                    {statistics.database.entityManagerFactoryOpen
                      ? "Connected"
                      : "Disconnected"}
                  </Badge>
                </div>
                <div className="bg-gray-50 p-3 rounded-lg">
                  <h5 className="font-medium mb-2">Database Type</h5>
                  <p className="text-sm text-gray-600 capitalize">
                    {statistics.database.configuredDatabaseKind}
                  </p>
                </div>
                <div className="bg-blue-50 p-3 rounded-soft shadow-soft border-soft">
                  <h5 className="font-medium mb-2">Search Capabilities</h5>
                  <p className="text-sm text-blue-700">
                    {statistics.strategy.supportsVectorSearch
                      ? "Vector search operations supported"
                      : "Standard similarity search only"}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}

export default SystemMonitor;
