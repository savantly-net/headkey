'use client';

import { useState, useEffect } from 'react';
import { 
  ChartBarIcon,
  ClockIcon,
  CpuChipIcon,
  BoltIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  MinusIcon,
  CircleStackIcon
} from '@heroicons/react/24/outline';

interface MetricData {
  value: number;
  unit: string;
  change: number;
  trend: 'up' | 'down' | 'stable';
  description: string;
}

interface PerformanceMetrics {
  responseTime: MetricData;
  throughput: MetricData;
  memoryUsage: MetricData;
  accuracy: MetricData;
  uptime: MetricData;
  apiCalls: MetricData;
}

const initialMetrics: PerformanceMetrics = {
  responseTime: {
    value: 42,
    unit: 'ms',
    change: -15,
    trend: 'down',
    description: 'Average API response time'
  },
  throughput: {
    value: 12500,
    unit: 'req/min',
    change: 8,
    trend: 'up',
    description: 'Requests processed per minute'
  },
  memoryUsage: {
    value: 2.3,
    unit: 'GB',
    change: 0,
    trend: 'stable',
    description: 'Memory storage utilized'
  },
  accuracy: {
    value: 97.8,
    unit: '%',
    change: 2.1,
    trend: 'up',
    description: 'Categorization accuracy rate'
  },
  uptime: {
    value: 99.9,
    unit: '%',
    change: 0,
    trend: 'stable',
    description: 'System availability'
  },
  apiCalls: {
    value: 1.2,
    unit: 'M',
    change: 23,
    trend: 'up',
    description: 'Total API calls today'
  }
};

const metricConfigs = [
  {
    key: 'responseTime' as keyof PerformanceMetrics,
    title: 'Response Time',
    icon: ClockIcon,
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    borderColor: 'border-blue-200'
  },
  {
    key: 'throughput' as keyof PerformanceMetrics,
    title: 'Throughput',
    icon: BoltIcon,
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    borderColor: 'border-yellow-200'
  },
  {
    key: 'memoryUsage' as keyof PerformanceMetrics,
    title: 'Memory Usage',
    icon: CircleStackIcon,
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    borderColor: 'border-green-200'
  },
  {
    key: 'accuracy' as keyof PerformanceMetrics,
    title: 'Accuracy',
    icon: ChartBarIcon,
    color: 'text-purple-600',
    bgColor: 'bg-purple-100',
    borderColor: 'border-purple-200'
  },
  {
    key: 'uptime' as keyof PerformanceMetrics,
    title: 'Uptime',
    icon: CpuChipIcon,
    color: 'text-indigo-600',
    bgColor: 'bg-indigo-100',
    borderColor: 'border-indigo-200'
  },
  {
    key: 'apiCalls' as keyof PerformanceMetrics,
    title: 'API Calls',
    icon: ArrowTrendingUpIcon,
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    borderColor: 'border-red-200'
  }
];

export default function PerformanceMetrics() {
  const [metrics, setMetrics] = useState<PerformanceMetrics>(initialMetrics);
  const [isLive, setIsLive] = useState(false);

  useEffect(() => {
    let interval: NodeJS.Timeout;

    if (isLive) {
      interval = setInterval(() => {
        setMetrics(prev => {
          const newMetrics = { ...prev };
          
          // Simulate real-time updates with small random variations
          Object.keys(newMetrics).forEach(key => {
            const metric = newMetrics[key as keyof PerformanceMetrics];
            const variation = (Math.random() - 0.5) * 0.1; // ±5% variation
            
            switch (key) {
              case 'responseTime':
                metric.value = Math.max(20, Math.min(100, metric.value + variation * 10));
                break;
              case 'throughput':
                metric.value = Math.max(8000, Math.min(15000, metric.value + variation * 1000));
                break;
              case 'memoryUsage':
                metric.value = Math.max(1.5, Math.min(4.0, metric.value + variation * 0.2));
                break;
              case 'accuracy':
                metric.value = Math.max(95, Math.min(99.5, metric.value + variation * 0.5));
                break;
              case 'uptime':
                metric.value = Math.max(99.0, Math.min(100, metric.value + variation * 0.02));
                break;
              case 'apiCalls':
                metric.value = Math.max(0.8, Math.min(2.0, metric.value + variation * 0.1));
                break;
            }
            
            metric.value = Math.round(metric.value * 100) / 100;
          });
          
          return newMetrics;
        });
      }, 2000);
    }

    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [isLive]);

  const formatValue = (value: number, unit: string) => {
    if (unit === 'M') {
      return `${value.toFixed(1)}${unit}`;
    } else if (unit === '%') {
      return `${value.toFixed(1)}${unit}`;
    } else if (unit === 'GB') {
      return `${value.toFixed(1)} ${unit}`;
    } else if (unit === 'req/min') {
      return `${value.toLocaleString()} ${unit}`;
    } else {
      return `${Math.round(value)} ${unit}`;
    }
  };

  const getTrendIcon = (trend: 'up' | 'down' | 'stable') => {
    switch (trend) {
      case 'up':
        return ArrowTrendingUpIcon;
      case 'down':
        return ArrowTrendingDownIcon;
      case 'stable':
        return MinusIcon;
      default:
        return MinusIcon;
    }
  };

  const getTrendColor = (trend: 'up' | 'down' | 'stable', isGoodWhenUp: boolean = true) => {
    if (trend === 'stable') return 'text-gray-500';
    
    const isPositive = isGoodWhenUp ? trend === 'up' : trend === 'down';
    return isPositive ? 'text-green-500' : 'text-red-500';
  };

  return (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="bg-gray-50 px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <ChartBarIcon className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">Performance Metrics</h3>
              <p className="text-sm text-gray-600">Development environment performance monitoring</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2">
              <div className={`w-2 h-2 rounded-full ${isLive ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}`}></div>
              <span className="text-sm text-gray-600">{isLive ? 'Live' : 'Static'}</span>
            </div>
            <button
              onClick={() => setIsLive(!isLive)}
              className={`px-3 py-1 text-sm font-medium rounded-lg transition-colors ${
                isLive 
                  ? 'bg-red-100 text-red-700 hover:bg-red-200' 
                  : 'bg-green-100 text-green-700 hover:bg-green-200'
              }`}
            >
              {isLive ? 'Stop Live' : 'Go Live'}
            </button>
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="p-6">
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {metricConfigs.map((config) => {
            const metric = metrics[config.key];
            const TrendIcon = getTrendIcon(metric.trend);
            const isResponseTime = config.key === 'responseTime';
            
            return (
              <div
                key={config.key}
                className={`p-6 rounded-xl border-2 ${config.borderColor} bg-white hover:shadow-lg transition-all duration-300 ${
                  isLive ? 'animate-pulse' : ''
                }`}
              >
                {/* Header */}
                <div className="flex items-center justify-between mb-4">
                  <div className={`p-2 rounded-lg ${config.bgColor}`}>
                    <config.icon className={`w-5 h-5 ${config.color}`} />
                  </div>
                  <div className={`flex items-center space-x-1 text-sm ${
                    getTrendColor(metric.trend, !isResponseTime)
                  }`}>
                    <TrendIcon className="w-4 h-4" />
                    <span>{Math.abs(metric.change)}%</span>
                  </div>
                </div>

                {/* Value */}
                <div className="mb-2">
                  <div className="text-2xl font-bold text-gray-900 mb-1">
                    {formatValue(metric.value, metric.unit)}
                  </div>
                  <div className="text-lg font-semibold text-gray-700">
                    {config.title}
                  </div>
                </div>

                {/* Description */}
                <div className="text-sm text-gray-600">
                  {metric.description}
                </div>

                {/* Progress Bar for percentage-based metrics */}
                {(metric.unit === '%') && (
                  <div className="mt-3">
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div 
                        className={`h-2 rounded-full transition-all duration-500 ${
                          config.key === 'accuracy' ? 'bg-purple-500' : 
                          config.key === 'uptime' ? 'bg-indigo-500' : 'bg-gray-500'
                        }`}
                        style={{ width: `${metric.value}%` }}
                      ></div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Summary Stats */}
        <div className="mt-8 grid md:grid-cols-3 gap-6">
          <div className="bg-blue-50 rounded-xl p-6 border border-blue-200">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <BoltIcon className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <div className="text-sm text-blue-600 font-medium">Target Performance</div>
                <div className="text-2xl font-bold text-blue-900">15,000+ req/min</div>
                <div className="text-sm text-blue-700">Designed capacity</div>
              </div>
            </div>
          </div>

          <div className="bg-green-50 rounded-xl p-6 border border-green-200">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <ClockIcon className="w-6 h-6 text-green-600" />
              </div>
              <div>
                <div className="text-sm text-green-600 font-medium">Response Time Goal</div>
                <div className="text-2xl font-bold text-green-900">&lt; 50ms</div>
                <div className="text-sm text-green-700">P95 latency target</div>
              </div>
            </div>
          </div>

          <div className="bg-purple-50 rounded-xl p-6 border border-purple-200">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                <CircleStackIcon className="w-6 h-6 text-purple-600" />
              </div>
              <div>
                <div className="text-sm text-purple-600 font-medium">Memory Efficiency</div>
                <div className="text-2xl font-bold text-purple-900">4.2:1</div>
                <div className="text-sm text-purple-700">Target compression ratio</div>
              </div>
            </div>
          </div>
        </div>

        {/* Status Indicators */}
        <div className="mt-8 bg-gray-50 rounded-xl p-6">
          <h4 className="text-lg font-semibold text-gray-900 mb-4">System Status</h4>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[
              { name: 'Information Ingestion', status: 'operational', latency: '12ms' },
              { name: 'Categorization Engine', status: 'operational', latency: '34ms' },
              { name: 'Memory Encoding', status: 'operational', latency: '8ms' },
              { name: 'Belief Update System', status: 'operational', latency: '45ms' },
              { name: 'Forgetting Agent', status: 'operational', latency: '15ms' },
              { name: 'Retrieval Engine', status: 'operational', latency: '23ms' }
            ].map((module) => (
              <div key={module.name} className="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-200">
                <div className="flex items-center space-x-3">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <span className="text-sm font-medium text-gray-900">{module.name}</span>
                </div>
                <span className="text-xs text-gray-500">{module.latency}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Footer Note */}
        <div className="mt-6 text-center text-sm text-gray-500">
          Development environment metrics • Simulated performance data • 
          <a href="#" className="text-blue-600 hover:text-blue-700 ml-1">Join beta for real metrics →</a>
        </div>
      </div>
    </div>
  );
}