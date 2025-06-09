'use client';

import { CheckIcon, XMarkIcon, StarIcon } from '@heroicons/react/24/outline';

interface Feature {
  name: string;
  description: string;
  headkey: boolean | string;
  traditional: boolean | string;
  langchain: boolean | string;
}

const features: Feature[] = [
  {
    name: 'Intelligent Forgetting',
    description: 'Automatically prunes irrelevant or outdated information',
    headkey: true,
    traditional: false,
    langchain: false
  },
  {
    name: 'Contextual Categorization',
    description: 'AI-powered semantic categorization with confidence scoring',
    headkey: true,
    traditional: 'Basic',
    langchain: 'Limited'
  },
  {
    name: 'Belief Reinforcement',
    description: 'Maintains coherent knowledge graphs and resolves conflicts',
    headkey: true,
    traditional: false,
    langchain: false
  },
  {
    name: 'Real-time Processing',
    description: 'Target response times for memory operations',
    headkey: '< 50ms (target)',
    traditional: '200-500ms',
    langchain: '100-300ms'
  },
  {
    name: 'Similarity Search',
    description: 'Advanced vector similarity with multiple search strategies',
    headkey: true,
    traditional: 'Basic',
    langchain: true
  },
  {
    name: 'Multi-modal Support',
    description: 'Support for text, images, and structured data',
    headkey: true,
    traditional: 'Text only',
    langchain: 'Limited'
  },
  {
    name: 'Scalability',
    description: 'Designed to handle large-scale memory operations',
    headkey: 'Designed for scale',
    traditional: 'Limited',
    langchain: 'Moderate'
  },
  {
    name: 'API Completeness',
    description: 'Comprehensive REST API with full CRUD operations',
    headkey: true,
    traditional: 'Basic',
    langchain: 'Moderate'
  },
  {
    name: 'Enterprise Security',
    description: 'End-to-end encryption, RBAC, and audit logging',
    headkey: true,
    traditional: false,
    langchain: 'Basic'
  },
  {
    name: 'Memory Compression',
    description: 'Intelligent compression to optimize storage efficiency',
    headkey: '4.2:1 (target)',
    traditional: false,
    langchain: false
  },
  {
    name: 'Conflict Resolution',
    description: 'Automatic detection and resolution of contradictory information',
    headkey: true,
    traditional: false,
    langchain: false
  },
  {
    name: 'Performance Analytics',
    description: 'Real-time metrics and performance monitoring',
    headkey: true,
    traditional: false,
    langchain: 'Basic'
  }
];

const solutions = [
  {
    name: 'HeadKey',
    description: 'CIBFE Architecture',
    highlight: true,
    price: 'Beta Access',
    color: 'blue'
  },
  {
    name: 'Traditional DBs',
    description: 'Vector Databases',
    highlight: false,
    price: '$300+/mo',
    color: 'gray'
  },
  {
    name: 'LangChain Memory',
    description: 'Basic Memory Chains',
    highlight: false,
    price: '$150+/mo',
    color: 'purple'
  }
];

const renderValue = (value: boolean | string, solutionColor: string) => {
  if (typeof value === 'boolean') {
    return value ? (
      <CheckIcon className={`w-5 h-5 text-${solutionColor}-600`} />
    ) : (
      <XMarkIcon className="w-5 h-5 text-red-500" />
    );
  }
  
  if (value === 'Basic' || value === 'Limited') {
    return (
      <span className="text-sm font-medium text-yellow-600 bg-yellow-100 px-2 py-1 rounded">
        {value}
      </span>
    );
  }
  
  return (
    <span className={`text-sm font-medium text-${solutionColor}-700`}>
      {value}
    </span>
  );
};

export default function FeatureComparison() {
  return (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="bg-gray-50 px-6 py-6 border-b border-gray-200">
        <div className="text-center">
          <h3 className="text-2xl font-bold text-gray-900 mb-2">Feature Comparison</h3>
          <p className="text-gray-600">
            See how HeadKey's CIBFE architecture compares to traditional solutions
          </p>
        </div>
      </div>

      {/* Comparison Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          {/* Solution Headers */}
          <thead className="bg-white border-b border-gray-200">
            <tr>
              <th className="px-6 py-4 text-left">
                <div className="text-sm font-medium text-gray-500">Features</div>
              </th>
              {solutions.map((solution) => (
                <th key={solution.name} className="px-6 py-4 text-center">
                  <div className={`relative ${solution.highlight ? 'bg-blue-50 border-2 border-blue-200 rounded-lg p-4 -m-2' : ''}`}>
                    {solution.highlight && (
                      <div className="absolute -top-2 left-1/2 transform -translate-x-1/2">
                        <span className="bg-blue-500 text-white px-3 py-1 rounded-full text-xs font-medium flex items-center">
                          <StarIcon className="w-3 h-3 mr-1" />
                          Recommended
                        </span>
                      </div>
                    )}
                    <div className={`text-lg font-bold ${solution.highlight ? 'text-blue-900' : 'text-gray-900'}`}>
                      {solution.name}
                    </div>
                    <div className={`text-sm ${solution.highlight ? 'text-blue-700' : 'text-gray-600'}`}>
                      {solution.description}
                    </div>
                    <div className={`text-lg font-semibold mt-2 ${solution.highlight ? 'text-blue-800' : 'text-gray-700'}`}>
                      {solution.price}
                    </div>
                  </div>
                </th>
              ))}
            </tr>
          </thead>

          {/* Features */}
          <tbody className="divide-y divide-gray-200">
            {features.map((feature, index) => (
              <tr key={feature.name} className={index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}>
                <td className="px-6 py-4">
                  <div>
                    <div className="text-sm font-medium text-gray-900">{feature.name}</div>
                    <div className="text-sm text-gray-600">{feature.description}</div>
                  </div>
                </td>
                <td className="px-6 py-4 text-center">
                  <div className="flex justify-center">
                    {renderValue(feature.headkey, 'blue')}
                  </div>
                </td>
                <td className="px-6 py-4 text-center">
                  <div className="flex justify-center">
                    {renderValue(feature.traditional, 'gray')}
                  </div>
                </td>
                <td className="px-6 py-4 text-center">
                  <div className="flex justify-center">
                    {renderValue(feature.langchain, 'purple')}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Footer */}
      <div className="bg-gray-50 px-6 py-4 border-t border-gray-200">
        <div className="text-center">
          <p className="text-sm text-gray-600 mb-4">
            Ready to join the future of AI memory management?
          </p>
          <div className="flex justify-center space-x-4">
            <button className="btn-primary">
              Join Beta Program
            </button>
            <button className="btn-secondary">
              Request Demo
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}