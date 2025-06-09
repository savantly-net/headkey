'use client';

import { useState } from 'react';
import { 
  PlayIcon, 
  ClipboardDocumentIcon, 
  CheckIcon,
  CommandLineIcon,
  ServerIcon,
  DocumentTextIcon,
  CogIcon
} from '@heroicons/react/24/outline';
import { ChevronDownIcon, ChevronRightIcon } from '@heroicons/react/24/solid';
import ContactCTA from '@/components/ContactCTA';

const apiEndpoints = [
  {
    id: 'ingest',
    method: 'POST',
    path: '/api/v1/memory/ingest',
    title: 'Ingest Information',
    description: 'Add new information to the memory system',
    category: 'Memory Operations',
    requestBody: {
      agentId: 'agent_123',
      content: 'User prefers coffee over tea in the morning',
      metadata: {
        source: 'user_interaction',
        timestamp: '2024-01-15T10:30:00Z',
        priority: 'normal'
      }
    },
    response: {
      success: true,
      memoryId: 'mem_7x9k2m',
      message: 'Information ingested successfully',
      processingTime: '234ms'
    }
  },
  {
    id: 'categorize',
    method: 'POST',
    path: '/api/v1/memory/categorize',
    title: 'Categorize Information',
    description: 'Classify and tag information contextually',
    category: 'Memory Operations',
    requestBody: {
      content: 'User prefers coffee over tea in the morning',
      agentId: 'agent_123'
    },
    response: {
      primary: 'Personal Preferences',
      secondary: 'Food & Beverage',
      tags: ['morning routine', 'beverages', 'preferences'],
      confidence: 0.94,
      metadata: {
        categoryId: 'cat_food_bev_001',
        processingModel: 'cibfe-categorizer-v2'
      }
    }
  },
  {
    id: 'retrieve',
    method: 'GET',
    path: '/api/v1/memory/retrieve',
    title: 'Retrieve Memories',
    description: 'Query and retrieve relevant memories',
    category: 'Memory Operations',
    queryParams: {
      query: 'morning beverage preferences',
      agentId: 'agent_123',
      limit: '10',
      similarityThreshold: '0.7'
    },
    response: {
      memories: [
        {
          id: 'mem_7x9k2m',
          content: 'User prefers coffee over tea in the morning',
          category: 'Personal Preferences',
          relevanceScore: 0.96,
          createdAt: '2024-01-15T10:30:00Z'
        }
      ],
      totalCount: 1,
      queryTime: '45ms'
    }
  },
  {
    id: 'forget',
    method: 'DELETE',
    path: '/api/v1/memory/forget',
    title: 'Forget Information',
    description: 'Remove or deprecate specific memories',
    category: 'Memory Operations',
    requestBody: {
      memoryId: 'mem_7x9k2m',
      agentId: 'agent_123',
      reason: 'outdated_preference'
    },
    response: {
      success: true,
      message: 'Memory successfully removed',
      memoryId: 'mem_7x9k2m',
      forgottenAt: '2024-01-15T15:45:00Z'
    }
  },
  {
    id: 'health',
    method: 'GET',
    path: '/api/v1/health',
    title: 'Health Check',
    description: 'Check system health and status',
    category: 'System Monitoring',
    response: {
      status: 'healthy',
      version: '2.1.0',
      uptime: '15d 8h 23m',
      modules: {
        ingestion: 'operational',
        categorization: 'operational',
        encoding: 'operational',
        beliefUpdate: 'operational',
        forgetting: 'operational',
        retrieval: 'operational'
      },
      performance: {
        avgResponseTime: '67ms',
        memoryUtilization: '43%',
        activeConnections: 127
      }
    }
  },
  {
    id: 'metrics',
    method: 'GET',
    path: '/api/v1/metrics',
    title: 'System Metrics',
    description: 'Get detailed performance metrics',
    category: 'System Monitoring',
    response: {
      requests: {
        total: 1248397,
        successful: 1247891,
        errors: 506,
        rate: '2.4k/min'
      },
      memory: {
        totalRecords: 156789,
        storageUsed: '2.3GB',
        compressionRatio: '4.2:1'
      },
      performance: {
        p50ResponseTime: '34ms',
        p95ResponseTime: '127ms',
        p99ResponseTime: '289ms'
      }
    }
  }
];

const methodColors = {
  GET: 'bg-green-100 text-green-800',
  POST: 'bg-blue-100 text-blue-800',
  PUT: 'bg-yellow-100 text-yellow-800',
  DELETE: 'bg-red-100 text-red-800'
};

export default function ApiExplorer() {
  const [selectedEndpoint, setSelectedEndpoint] = useState(apiEndpoints[0]);
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(['request', 'response']));
  const [copiedText, setCopiedText] = useState<string | null>(null);

  const toggleSection = (section: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(section)) {
      newExpanded.delete(section);
    } else {
      newExpanded.add(section);
    }
    setExpandedSections(newExpanded);
  };

  const copyToClipboard = async (text: string, type: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedText(type);
      setTimeout(() => setCopiedText(null), 2000);
    } catch (err) {
      console.error('Failed to copy text: ', err);
    }
  };

  const generateCurl = (endpoint: any) => {
    let curl = `curl -X ${endpoint.method} \\\n  "${window.location.origin}${endpoint.path}"`;
    
    if (endpoint.method !== 'GET') {
      curl += ` \\\n  -H "Content-Type: application/json"`;
      if (endpoint.requestBody) {
        curl += ` \\\n  -d '${JSON.stringify(endpoint.requestBody, null, 2)}'`;
      }
    } else if (endpoint.queryParams) {
      const params = new URLSearchParams(endpoint.queryParams).toString();
      curl = curl.replace(endpoint.path, `${endpoint.path}?${params}`);
    }
    
    curl += ` \\\n  -H "Authorization: Bearer YOUR_API_KEY"`;
    return curl;
  };

  const categories = [...new Set(apiEndpoints.map(ep => ep.category))];

  return (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="bg-gray-50 px-6 py-4 border-b border-gray-200">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
            <CommandLineIcon className="w-6 h-6 text-blue-600" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-gray-900">API Explorer</h3>
            <p className="text-sm text-gray-600">Interactive documentation for HeadKey REST API (Beta)</p>
          </div>
        </div>
      </div>

      <div className="flex">
        {/* Sidebar */}
        <div className="w-80 border-r border-gray-200 bg-gray-50">
          <div className="p-4">
            <div className="mb-4">
              <div className="flex items-center space-x-2 text-sm text-gray-600 mb-2">
                <ServerIcon className="w-4 h-4" />
                <span>Base URL: /api/v1</span>
              </div>
              <div className="text-xs text-gray-500">
                Beta API - All endpoints require authentication via Bearer token
              </div>
            </div>

            {categories.map(category => (
              <div key={category} className="mb-6">
                <h4 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">
                  {category}
                </h4>
                <div className="space-y-1">
                  {apiEndpoints
                    .filter(ep => ep.category === category)
                    .map(endpoint => (
                      <button
                        key={endpoint.id}
                        onClick={() => setSelectedEndpoint(endpoint)}
                        className={`w-full text-left p-3 rounded-lg transition-all duration-200 ${
                          selectedEndpoint.id === endpoint.id
                            ? 'bg-blue-100 border border-blue-200'
                            : 'hover:bg-white hover:shadow-sm border border-transparent'
                        }`}
                      >
                        <div className="flex items-center space-x-3">
                          <span className={`px-2 py-1 text-xs font-semibold rounded ${methodColors[endpoint.method as keyof typeof methodColors]}`}>
                            {endpoint.method}
                          </span>
                          <div className="flex-1 min-w-0">
                            <div className="text-sm font-medium text-gray-900 truncate">
                              {endpoint.title}
                            </div>
                            <div className="text-xs text-gray-500 truncate">
                              {endpoint.path}
                            </div>
                          </div>
                        </div>
                      </button>
                    ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Main Content */}
        <div className="flex-1 p-6">
          {/* Endpoint Header */}
          <div className="mb-6">
            <div className="flex items-center space-x-3 mb-3">
              <span className={`px-3 py-1 text-sm font-semibold rounded ${methodColors[selectedEndpoint.method as keyof typeof methodColors]}`}>
                {selectedEndpoint.method}
              </span>
              <code className="text-lg font-mono text-gray-900">{selectedEndpoint.path}</code>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">{selectedEndpoint.title}</h2>
            <p className="text-gray-600">{selectedEndpoint.description}</p>
          </div>

          {/* Try It Button */}
          <div className="mb-6">
            <button className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
              <PlayIcon className="w-4 h-4 mr-2" />
              Try It Out (Beta)
            </button>
          </div>

          {/* Request Section */}
          {(selectedEndpoint.requestBody || selectedEndpoint.queryParams) && (
            <div className="mb-6">
              <button
                onClick={() => toggleSection('request')}
                className="flex items-center space-x-2 text-lg font-semibold text-gray-900 mb-4 hover:text-blue-600 transition-colors"
              >
                {expandedSections.has('request') ? (
                  <ChevronDownIcon className="w-5 h-5" />
                ) : (
                  <ChevronRightIcon className="w-5 h-5" />
                )}
                <span>Request</span>
              </button>

              {expandedSections.has('request') && (
                <div className="bg-gray-50 rounded-lg p-4">
                  {selectedEndpoint.queryParams && (
                    <div className="mb-4">
                      <h4 className="text-sm font-semibold text-gray-700 mb-2">Query Parameters</h4>
                      <div className="space-y-2">
                        {Object.entries(selectedEndpoint.queryParams).map(([key, value]) => (
                          <div key={key} className="flex items-center space-x-4 text-sm">
                            <code className="font-mono text-blue-600 bg-blue-50 px-2 py-1 rounded min-w-0 flex-shrink-0">
                              {key}
                            </code>
                            <span className="text-gray-600">=</span>
                            <code className="font-mono text-gray-900 bg-white px-2 py-1 rounded border flex-1">
                              {value}
                            </code>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {selectedEndpoint.requestBody && (
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <h4 className="text-sm font-semibold text-gray-700">Request Body</h4>
                        <button
                          onClick={() => copyToClipboard(JSON.stringify(selectedEndpoint.requestBody, null, 2), 'request')}
                          className="flex items-center space-x-1 text-xs text-gray-500 hover:text-gray-700 transition-colors"
                        >
                          {copiedText === 'request' ? (
                            <CheckIcon className="w-4 h-4 text-green-500" />
                          ) : (
                            <ClipboardDocumentIcon className="w-4 h-4" />
                          )}
                          <span>{copiedText === 'request' ? 'Copied!' : 'Copy'}</span>
                        </button>
                      </div>
                      <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto text-sm">
                        <code>{JSON.stringify(selectedEndpoint.requestBody, null, 2)}</code>
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Response Section */}
          <div className="mb-6">
            <button
              onClick={() => toggleSection('response')}
              className="flex items-center space-x-2 text-lg font-semibold text-gray-900 mb-4 hover:text-blue-600 transition-colors"
            >
              {expandedSections.has('response') ? (
                <ChevronDownIcon className="w-5 h-5" />
              ) : (
                <ChevronRightIcon className="w-5 h-5" />
              )}
              <span>Response</span>
            </button>

            {expandedSections.has('response') && (
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-semibold text-gray-700">200 - Success</h4>
                  <button
                    onClick={() => copyToClipboard(JSON.stringify(selectedEndpoint.response, null, 2), 'response')}
                    className="flex items-center space-x-1 text-xs text-gray-500 hover:text-gray-700 transition-colors"
                  >
                    {copiedText === 'response' ? (
                      <CheckIcon className="w-4 h-4 text-green-500" />
                    ) : (
                      <ClipboardDocumentIcon className="w-4 h-4" />
                    )}
                    <span>{copiedText === 'response' ? 'Copied!' : 'Copy'}</span>
                  </button>
                </div>
                <pre className="bg-gray-900 text-green-400 p-4 rounded-lg overflow-x-auto text-sm">
                  <code>{JSON.stringify(selectedEndpoint.response, null, 2)}</code>
                </pre>
              </div>
            )}
          </div>

          {/* cURL Example */}
          <div className="mb-6">
            <button
              onClick={() => toggleSection('curl')}
              className="flex items-center space-x-2 text-lg font-semibold text-gray-900 mb-4 hover:text-blue-600 transition-colors"
            >
              {expandedSections.has('curl') ? (
                <ChevronDownIcon className="w-5 h-5" />
              ) : (
                <ChevronRightIcon className="w-5 h-5" />
              )}
              <span>cURL Example</span>
            </button>

            {expandedSections.has('curl') && (
              <div className="bg-gray-50 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-semibold text-gray-700">Command Line</h4>
                  <button
                    onClick={() => copyToClipboard(generateCurl(selectedEndpoint), 'curl')}
                    className="flex items-center space-x-1 text-xs text-gray-500 hover:text-gray-700 transition-colors"
                  >
                    {copiedText === 'curl' ? (
                      <CheckIcon className="w-4 h-4 text-green-500" />
                    ) : (
                      <ClipboardDocumentIcon className="w-4 h-4" />
                    )}
                    <span>{copiedText === 'curl' ? 'Copied!' : 'Copy'}</span>
                  </button>
                </div>
                <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto text-sm">
                  <code>{generateCurl(selectedEndpoint)}</code>
                </pre>
              </div>
            )}
          </div>

          {/* Contact CTA */}
          <ContactCTA 
            title="Need API Access?"
            description="Join our beta program to get early access to the HeadKey API and help us build the future of AI memory management."
            variant="compact"
          />
        </div>
      </div>
    </div>
  );
}