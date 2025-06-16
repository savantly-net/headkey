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
import { Skeleton } from "@/components/ui/skeleton";
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
  Brain,
  Clock,
  Zap,
  BarChart3,
  Wifi,
  WifiOff,
} from "lucide-react";

interface DashboardStatsProps {
  autoRefresh?: boolean;
  refreshInterval?: number;
}

export function DashboardStats({
  autoRefresh = true,
  refreshInterval = 30000,
}: DashboardStatsProps) {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [statistics, setStatistics] = useState<SystemStatistics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  const fetchDashboardData = async () => {
    console.log("Fetching dashboard data...");
    try {
      const [healthData, statsData] = await Promise.all([
        systemApi.getComprehensiveHealth(),
        systemApi.getStatistics(),
      ]);

      console.log("Health data:", healthData);
      console.log("Statistics data:", statsData);

      setHealth(healthData);
      setStatistics(statsData);
      setLastUpdate(new Date());
    } catch (error) {
      console.error("Failed to fetch dashboard data:", error);
      const apiError = handleApiError(error);
      toast.error(`Failed to fetch dashboard data: ${apiError.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();

    if (autoRefresh) {
      const interval = setInterval(fetchDashboardData, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [autoRefresh, refreshInterval]);

  const formatUptime = (seconds: number) => {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (days > 0) {
      return `${days}d ${hours}h ${minutes}m`;
    } else if (hours > 0) {
      return `${hours}h ${minutes}m`;
    } else {
      return `${minutes}m`;
    }
  };

  const formatNumber = (num?: number) => {
    return num ? num.toLocaleString() : "0";
  };

  const getSystemStatus = () => {
    if (!health)
      return {
        status: "checking",
        icon: AlertCircle,
        color: "text-yellow-600",
      };

    if (health.status === "UP") {
      return { status: "online", icon: CheckCircle, color: "text-green-600" };
    } else {
      return { status: "offline", icon: XCircle, color: "text-red-600" };
    }
  };

  const getStatusBadge = () => {
    const systemStatus = getSystemStatus();

    switch (systemStatus.status) {
      case "checking":
        return (
          <Badge
            variant="secondary"
            className="bg-yellow-100 text-yellow-800 border-yellow-200"
          >
            <AlertCircle className="h-3 w-3 mr-1" />
            Checking...
          </Badge>
        );
      case "online":
        return (
          <Badge
            variant="default"
            className="bg-green-100 text-green-800 border-green-200"
          >
            <Wifi className="h-3 w-3 mr-1" />
            Online
          </Badge>
        );
      case "offline":
        return (
          <Badge
            variant="destructive"
            className="bg-red-100 text-red-800 border-red-200"
          >
            <WifiOff className="h-3 w-3 mr-1" />
            Offline
          </Badge>
        );
      default:
        return null;
    }
  };

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <Card key={i}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <Skeleton className="h-4 w-[100px]" />
              <Skeleton className="h-4 w-4" />
            </CardHeader>
            <CardContent>
              <Skeleton className="h-8 w-[60px] mb-2" />
              <Skeleton className="h-3 w-[120px]" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with refresh button */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <h2 className="text-2xl font-bold">System Dashboard</h2>
          {getStatusBadge()}
        </div>
        <div className="flex items-center gap-2">
          {lastUpdate && (
            <span className="text-sm text-muted-foreground">
              Last updated: {lastUpdate.toLocaleTimeString()}
            </span>
          )}
          <Button
            variant="outline"
            size="sm"
            onClick={fetchDashboardData}
            disabled={isLoading}
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${isLoading ? "animate-spin" : ""}`}
            />
            Refresh
          </Button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
        {/* Total Memories */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Total Memories
            </CardTitle>
            <Brain className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics
                ? formatNumber(statistics.memorySystem.totalMemories)
                : "0"}
            </div>
            <p className="text-xs text-muted-foreground">
              Stored in memory system
            </p>
          </CardContent>
        </Card>

        {/* Total Beliefs */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Active Beliefs
            </CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics?.beliefStorage?.activeBeliefs != null
                ? formatNumber(statistics.beliefStorage.activeBeliefs)
                : statistics?.beliefStorage?.operationCount != null
                  ? formatNumber(statistics.beliefStorage.operationCount)
                  : "N/A"}
            </div>
            <p className="text-xs text-muted-foreground">
              {statistics?.beliefStorage?.totalBeliefs != null
                ? `${formatNumber(statistics.beliefStorage.totalBeliefs)} total`
                : statistics?.beliefStorage?.operationCount != null
                  ? "Operations completed"
                  : "Beliefs not available"}
            </p>
          </CardContent>
        </Card>

        {/* Total Operations */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              Total Operations
            </CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics?.memorySystem?.totalOperations != null
                ? formatNumber(statistics.memorySystem.totalOperations)
                : statistics?.memorySystem?.operationCount != null
                  ? formatNumber(statistics.memorySystem.operationCount)
                  : "0"}
            </div>
            <p className="text-xs text-muted-foreground">
              All-time operations count
            </p>
          </CardContent>
        </Card>

        {/* System Uptime */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">System Status</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics?.memorySystem?.uptimeSeconds != null
                ? formatUptime(statistics.memorySystem.uptimeSeconds)
                : statistics?.memorySystem?.clusterHealthy != null
                  ? statistics.memorySystem.clusterHealthy
                    ? "Healthy"
                    : "Unhealthy"
                  : "Unknown"}
            </div>
            <p className="text-xs text-muted-foreground">
              {statistics?.memorySystem?.uptimeSeconds != null
                ? "Since last restart"
                : "System status"}
            </p>
          </CardContent>
        </Card>

        {/* Belief Conflicts / Errors */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">
              {statistics?.beliefStorage?.unresolvedConflicts != null
                ? "Conflicts"
                : "Errors"}
            </CardTitle>
            <AlertCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics?.beliefStorage?.unresolvedConflicts != null ? (
                <span
                  className={
                    statistics.beliefStorage.unresolvedConflicts > 0
                      ? "text-amber-600"
                      : "text-green-600"
                  }
                >
                  {formatNumber(statistics.beliefStorage.unresolvedConflicts)}
                </span>
              ) : statistics?.beliefStorage?.errorCount != null ? (
                <span
                  className={
                    statistics.beliefStorage.errorCount > 0
                      ? "text-red-600"
                      : "text-green-600"
                  }
                >
                  {formatNumber(statistics.beliefStorage.errorCount)}
                </span>
              ) : (
                "N/A"
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {statistics?.beliefStorage?.totalConflicts != null
                ? `${formatNumber(statistics.beliefStorage.totalConflicts)} total conflicts`
                : statistics?.beliefStorage?.errorCount != null
                  ? "Operation errors"
                  : "Issues not available"}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Detailed Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        {/* Belief Storage Details */}
        {statistics?.beliefStorage && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Zap className="h-5 w-5" />
                Belief Storage
              </CardTitle>
              <CardDescription>
                Detailed belief system statistics
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {statistics.beliefStorage.totalBeliefs != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Total Beliefs:</span>
                  <span className="font-medium">
                    {formatNumber(statistics.beliefStorage.totalBeliefs)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.activeBeliefs != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Active Beliefs:</span>
                  <span className="font-medium">
                    {formatNumber(statistics.beliefStorage.activeBeliefs)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.totalConflicts != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Total Conflicts:</span>
                  <span className="font-medium">
                    {formatNumber(statistics.beliefStorage.totalConflicts)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.unresolvedConflicts != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Unresolved Conflicts:</span>
                  <span className="font-medium text-amber-600">
                    {formatNumber(statistics.beliefStorage.unresolvedConflicts)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.operationCount != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Total Operations:</span>
                  <span className="font-medium">
                    {formatNumber(statistics.beliefStorage.operationCount)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.errorCount != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Error Count:</span>
                  <span className="font-medium text-red-600">
                    {formatNumber(statistics.beliefStorage.errorCount)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.agentCount != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Agent Count:</span>
                  <span className="font-medium">
                    {formatNumber(statistics.beliefStorage.agentCount)}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.storageType && (
                <div className="flex justify-between">
                  <span className="text-sm">Storage Type:</span>
                  <span className="font-medium text-xs">
                    {statistics.beliefStorage.storageType}
                  </span>
                </div>
              )}
              {statistics.beliefStorage.clusterHealthy != null && (
                <div className="flex justify-between">
                  <span className="text-sm">Cluster Status:</span>
                  <span
                    className={`font-medium ${statistics.beliefStorage.clusterHealthy ? "text-green-600" : "text-red-600"}`}
                  >
                    {statistics.beliefStorage.clusterHealthy
                      ? "Healthy"
                      : "Unhealthy"}
                  </span>
                </div>
              )}
            </CardContent>
          </Card>
        )}
        {/* Memory System Details */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Brain className="h-5 w-5" />
              Memory System
            </CardTitle>
            <CardDescription>Detailed memory system statistics</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex justify-between">
              <span className="text-sm">Total Searches:</span>
              <span className="font-medium">
                {statistics
                  ? formatNumber(statistics.memorySystem.totalSearches)
                  : "0"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm">Total Updates:</span>
              <span className="font-medium">
                {statistics
                  ? formatNumber(statistics.memorySystem.totalUpdates)
                  : "0"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm">Total Deletes:</span>
              <span className="font-medium">
                {statistics
                  ? formatNumber(statistics.memorySystem.totalDeletes)
                  : "0"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm">Batch Size:</span>
              <span className="font-medium">
                {statistics ? statistics.memorySystem.batchSize : "0"}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Database & Operations */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Database className="h-5 w-5" />
              Database & Operations
            </CardTitle>
            <CardDescription>
              Database status and operation statistics
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {statistics?.database && (
              <>
                <div className="flex justify-between">
                  <span className="text-sm">Database Status:</span>
                  <Badge
                    variant={
                      statistics.database.entityManagerFactoryOpen
                        ? "default"
                        : "destructive"
                    }
                  >
                    {statistics.database.entityManagerFactoryOpen
                      ? "Active"
                      : "Inactive"}
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm">Database Type:</span>
                  <span className="font-medium text-xs">
                    {statistics.database.configuredDatabaseKind || "Unknown"}
                  </span>
                </div>
              </>
            )}
            {statistics?.elasticsearch && (
              <>
                <div className="flex justify-between">
                  <span className="text-sm">Cluster Status:</span>
                  <Badge
                    variant={
                      statistics.elasticsearch.clusterAvailable
                        ? "default"
                        : "destructive"
                    }
                  >
                    {statistics.elasticsearch.clusterAvailable
                      ? "Available"
                      : "Unavailable"}
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm">Elasticsearch Host:</span>
                  <span className="font-medium text-xs">
                    {statistics.elasticsearch.host}:
                    {statistics.elasticsearch.port}
                  </span>
                </div>
              </>
            )}
            {statistics?.beliefStorage?.totalStoreOperations != null && (
              <div className="flex justify-between">
                <span className="text-sm">Store Operations:</span>
                <span className="font-medium">
                  {formatNumber(statistics.beliefStorage.totalStoreOperations)}
                </span>
              </div>
            )}
            {statistics?.beliefStorage?.totalQueryOperations != null && (
              <div className="flex justify-between">
                <span className="text-sm">Query Operations:</span>
                <span className="font-medium">
                  {formatNumber(statistics.beliefStorage.totalQueryOperations)}
                </span>
              </div>
            )}
            {statistics?.beliefStorage?.totalSearchOperations != null && (
              <div className="flex justify-between">
                <span className="text-sm">Search Operations:</span>
                <span className="font-medium">
                  {formatNumber(statistics.beliefStorage.totalSearchOperations)}
                </span>
              </div>
            )}
            {statistics?.beliefStorage?.searchTimeout != null && (
              <div className="flex justify-between">
                <span className="text-sm">Search Timeout:</span>
                <span className="font-medium">
                  {statistics.beliefStorage.searchTimeout}ms
                </span>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Search Strategy */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5" />
              Search Strategy
            </CardTitle>
            <CardDescription>
              Current similarity search configuration
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex justify-between">
              <span className="text-sm">Strategy:</span>
              <span className="font-medium">
                {statistics?.strategy.name || "Unknown"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm">Vector Search:</span>
              <Badge
                variant={
                  statistics?.strategy.supportsVectorSearch
                    ? "default"
                    : "secondary"
                }
              >
                {statistics?.strategy.supportsVectorSearch
                  ? "Supported"
                  : "Not Supported"}
              </Badge>
            </div>
            <div className="flex justify-between">
              <span className="text-sm">Similarity Threshold:</span>
              <span className="font-medium">
                {statistics
                  ? statistics.memorySystem.similarityThreshold
                  : "0.0"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm">Max Results:</span>
              <span className="font-medium">
                {statistics
                  ? formatNumber(
                      statistics.memorySystem.maxSimilaritySearchResults,
                    )
                  : "0"}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Performance Indicators */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5" />
            Performance Indicators
          </CardTitle>
          <CardDescription>
            System performance and configuration status
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3">
            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <p className="text-sm font-medium">Second Level Cache</p>
                <p className="text-xs text-muted-foreground">
                  Hibernate caching
                </p>
              </div>
              <Badge
                variant={
                  statistics?.memorySystem.secondLevelCacheEnabled
                    ? "default"
                    : "secondary"
                }
              >
                {statistics?.memorySystem.secondLevelCacheEnabled
                  ? "Enabled"
                  : "Disabled"}
              </Badge>
            </div>

            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <p className="text-sm font-medium">Managed Entities</p>
                <p className="text-xs text-muted-foreground">
                  JPA entity types
                </p>
              </div>
              <span className="font-bold">
                {statistics ? statistics.memorySystem.managedTypes : "0"}
              </span>
            </div>

            <div className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <p className="text-sm font-medium">System Started</p>
                <p className="text-xs text-muted-foreground">
                  Startup timestamp
                </p>
              </div>
              <span className="text-sm font-medium">
                {statistics
                  ? new Date(
                      statistics?.memorySystem?.startTime || 0,
                    ).toLocaleDateString()
                  : "Unknown"}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
