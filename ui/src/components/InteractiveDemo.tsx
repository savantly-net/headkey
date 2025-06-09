'use client';

import { useState, useEffect } from 'react';
import { 
  PlayIcon, 
  PauseIcon, 
  ArrowPathIcon,
  CheckCircleIcon,
  ClockIcon,
  BoltIcon
} from '@heroicons/react/24/outline';
import { 
  BrainCircuitIcon, 
  DatabaseIcon, 
  SearchIcon, 
  FilterIcon,
  NetworkIcon,
  ZapIcon
} from 'lucide-react';

const demoSteps = [
  {
    id: 1,
    title: 'Information Ingestion',
    description: 'Raw data is received and validated',
    icon: BrainCircuitIcon,
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    duration: 1000,
    data: {
      input: 'User preference: Prefers coffee over tea in the morning',
      status: 'Processing...'
    }
  },
  {
    id: 2,
    title: 'Contextual Categorization',
    description: 'AI analyzes and categorizes the information',
    icon: FilterIcon,
    color: 'text-purple-600',
    bgColor: 'bg-purple-100',
    duration: 1500,
    data: {
      category: 'Personal Preferences',
      subcategory: 'Food & Beverage',
      confidence: '94%',
      tags: ['morning routine', 'beverages', 'preferences']
    }
  },
  {
    id: 3,
    title: 'Memory Encoding',
    description: 'Information is encoded and stored in memory',
    icon: DatabaseIcon,
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    duration: 800,
    data: {
      memoryId: 'mem_7x9k2m',
      vectorEmbedding: '[0.23, -0.45, 0.78, ...]',
      storageLocation: 'Memory Bank Alpha'
    }
  },
  {
    id: 4,
    title: 'Belief Update',
    description: 'System updates belief graph with new information',
    icon: NetworkIcon,
    color: 'text-indigo-600',
    bgColor: 'bg-indigo-100',
    duration: 1200,
    data: {
      beliefStrength: 'High',
      conflicts: 'None detected',
      connections: ['morning_habits', 'taste_preferences']
    }
  },
  {
    id: 5,
    title: 'Relevance Evaluation',
    description: 'Smart forgetting agent evaluates information relevance',
    icon: ZapIcon,
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    duration: 600,
    data: {
      relevanceScore: '8.7/10',
      retentionPeriod: '6 months',
      forgettingCandidate: false
    }
  },
  {
    id: 6,
    title: 'Ready for Retrieval',
    description: 'Information is indexed and ready for intelligent retrieval',
    icon: SearchIcon,
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    duration: 500,
    data: {
      indexed: true,
      searchKeywords: ['coffee', 'morning', 'preference'],
      retrievalLatency: '< 50ms'
    }
  }
];

export default function InteractiveDemo() {
  const [currentStep, setCurrentStep] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [completedSteps, setCompletedSteps] = useState<Set<number>>(new Set());

  useEffect(() => {
    let timeout: NodeJS.Timeout;
    
    if (isPlaying && currentStep < demoSteps.length) {
      const step = demoSteps[currentStep];
      timeout = setTimeout(() => {
        setCompletedSteps(prev => new Set([...prev, step.id]));
        setCurrentStep(prev => prev + 1);
      }, step.duration);
    } else if (currentStep >= demoSteps.length) {
      setIsPlaying(false);
    }

    return () => clearTimeout(timeout);
  }, [currentStep, isPlaying]);

  const handlePlay = () => {
    if (currentStep >= demoSteps.length) {
      // Reset demo
      setCurrentStep(0);
      setCompletedSteps(new Set());
    }
    setIsPlaying(true);
  };

  const handlePause = () => {
    setIsPlaying(false);
  };

  const handleReset = () => {
    setIsPlaying(false);
    setCurrentStep(0);
    setCompletedSteps(new Set());
  };

  const handleStepClick = (stepIndex: number) => {
    if (!isPlaying) {
      setCurrentStep(stepIndex);
      setCompletedSteps(new Set(demoSteps.slice(0, stepIndex).map(s => s.id)));
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="bg-gray-50 px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">CIBFE Pipeline Demo</h3>
            <p className="text-sm text-gray-600">Watch how HeadKey processes information through all six modules</p>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={isPlaying ? handlePause : handlePlay}
              className="inline-flex items-center px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              {isPlaying ? (
                <>
                  <PauseIcon className="w-4 h-4 mr-2" />
                  Pause
                </>
              ) : (
                <>
                  <PlayIcon className="w-4 h-4 mr-2" />
                  {currentStep >= demoSteps.length ? 'Restart' : 'Play'}
                </>
              )}
            </button>
            <button
              onClick={handleReset}
              className="inline-flex items-center px-3 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
            >
              <ArrowPathIcon className="w-4 h-4 mr-2" />
              Reset
            </button>
          </div>
        </div>
      </div>

      {/* Pipeline Visualization */}
      <div className="p-6">
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
          {demoSteps.map((step, index) => {
            const isActive = currentStep === index && isPlaying;
            const isCompleted = completedSteps.has(step.id);
            const isClickable = !isPlaying;

            return (
              <div
                key={step.id}
                onClick={() => handleStepClick(index)}
                className={`
                  relative p-4 rounded-xl border-2 transition-all duration-300 cursor-pointer
                  ${isActive 
                    ? 'border-blue-500 bg-blue-50 shadow-lg scale-105' 
                    : isCompleted 
                      ? 'border-green-500 bg-green-50' 
                      : 'border-gray-200 bg-white hover:border-gray-300'
                  }
                  ${isClickable ? 'hover:shadow-md' : ''}
                `}
              >
                {/* Status Indicator */}
                <div className="absolute top-2 right-2">
                  {isActive ? (
                    <div className="w-3 h-3 bg-blue-500 rounded-full animate-pulse"></div>
                  ) : isCompleted ? (
                    <CheckCircleIcon className="w-5 h-5 text-green-500" />
                  ) : (
                    <ClockIcon className="w-5 h-5 text-gray-400" />
                  )}
                </div>

                {/* Icon */}
                <div className={`inline-flex p-2 rounded-lg ${step.bgColor} mb-3`}>
                  <step.icon className={`w-5 h-5 ${step.color}`} />
                </div>

                {/* Content */}
                <h4 className="font-semibold text-gray-900 mb-2 text-sm">{step.title}</h4>
                <p className="text-xs text-gray-600 mb-3">{step.description}</p>

                {/* Progress Bar */}
                <div className="w-full bg-gray-200 rounded-full h-1 mb-3">
                  <div 
                    className={`h-1 rounded-full transition-all duration-300 ${
                      isCompleted ? 'bg-green-500 w-full' : 
                      isActive ? 'bg-blue-500 animate-pulse' : 'bg-gray-300 w-0'
                    }`}
                    style={isActive ? { width: '100%', animationDuration: `${step.duration}ms` } : {}}
                  ></div>
                </div>

                {/* Step Number */}
                <div className="text-xs font-bold text-gray-500">
                  Step {step.id}
                </div>
              </div>
            );
          })}
        </div>

        {/* Current Step Details */}
        {currentStep < demoSteps.length && (
          <div className="mt-8 p-6 bg-gray-50 rounded-xl">
            <div className="flex items-center mb-4">
              <div className="flex items-center space-x-2">
                <BoltIcon className="w-5 h-5 text-blue-600" />
                <h4 className="text-lg font-semibold text-gray-900">
                  Current: {demoSteps[currentStep].title}
                </h4>
              </div>
              {isPlaying && (
                <div className="ml-auto flex items-center space-x-2 text-sm text-gray-600">
                  <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
                  Processing...
                </div>
              )}
            </div>
            
            <div className="grid md:grid-cols-2 gap-4 text-sm">
              {Object.entries(demoSteps[currentStep].data).map(([key, value]) => (
                <div key={key} className="flex justify-between items-center py-2 border-b border-gray-200">
                  <span className="font-medium text-gray-700 capitalize">
                    {key.replace(/([A-Z])/g, ' $1').trim()}:
                  </span>
                  <span className="text-gray-900 font-mono text-xs">
                    {Array.isArray(value) ? value.join(', ') : value.toString()}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Completion Message */}
        {currentStep >= demoSteps.length && completedSteps.size === demoSteps.length && (
          <div className="mt-8 p-6 bg-green-50 rounded-xl border border-green-200">
            <div className="flex items-center space-x-3">
              <CheckCircleIcon className="w-8 h-8 text-green-600" />
              <div>
                <h4 className="text-lg font-semibold text-green-900">Pipeline Complete!</h4>
                <p className="text-green-700">
                  Information has been successfully processed through all six CIBFE modules and is ready for intelligent retrieval.
                </p>
              </div>
            </div>
            <div className="mt-4 flex items-center space-x-4 text-sm text-green-700">
              <div className="flex items-center space-x-1">
                <ClockIcon className="w-4 h-4" />
                <span>Total Time: {demoSteps.reduce((acc, step) => acc + step.duration, 0)}ms</span>
              </div>
              <div className="flex items-center space-x-1">
                <DatabaseIcon className="w-4 h-4" />
                <span>Memory ID: mem_7x9k2m</span>
              </div>
              <div className="flex items-center space-x-1">
                <SearchIcon className="w-4 h-4" />
                <span>Retrieval Ready</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}