'use client';

import { 
  EnvelopeIcon,
  ArrowRightIcon,
  ChatBubbleLeftRightIcon
} from '@heroicons/react/24/outline';

interface ContactCTAProps {
  title?: string;
  description?: string;
  variant?: 'default' | 'compact' | 'inline';
  showBackground?: boolean;
}

export default function ContactCTA({ 
  title = "Have Questions?",
  description = "Get in touch with our team and learn how HeadKey can transform your AI applications.",
  variant = 'default',
  showBackground = true
}: ContactCTAProps) {
  
  if (variant === 'inline') {
    return (
      <div className="flex flex-col sm:flex-row items-center justify-between p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <div className="flex items-center space-x-3 mb-4 sm:mb-0">
          <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
            <ChatBubbleLeftRightIcon className="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <div className="font-medium text-gray-900">{title}</div>
            <div className="text-sm text-gray-600">Ready to get started?</div>
          </div>
        </div>
        <div className="flex space-x-3">
          <a href="/contact" className="btn-secondary">
            Contact Us
          </a>
          <button className="btn-primary">
            Join Beta
          </button>
        </div>
      </div>
    );
  }

  if (variant === 'compact') {
    return (
      <div className={`text-center p-6 rounded-xl ${showBackground ? 'bg-gray-50 border border-gray-200' : ''}`}>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-600 mb-4 text-sm">{description}</p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <a href="/contact" className="btn-secondary">
            <EnvelopeIcon className="w-4 h-4 mr-2" />
            Contact Us
          </a>
          <button className="btn-primary">
            Join Beta Program
            <ArrowRightIcon className="w-4 h-4 ml-2" />
          </button>
        </div>
      </div>
    );
  }

  // Default variant
  return (
    <div className={`text-center p-8 rounded-2xl ${showBackground ? 'bg-blue-50 border border-blue-200' : ''}`}>
      <div className="flex justify-center mb-6">
        <div className="w-16 h-16 bg-blue-100 rounded-xl flex items-center justify-center">
          <ChatBubbleLeftRightIcon className="w-8 h-8 text-blue-600" />
        </div>
      </div>
      
      <h3 className="text-2xl font-bold text-gray-900 mb-4">{title}</h3>
      <p className="text-lg text-gray-600 mb-8 max-w-2xl mx-auto">{description}</p>
      
      <div className="flex flex-col sm:flex-row gap-4 justify-center">
        <a href="/contact" className="btn-secondary text-lg px-6 py-3">
          <EnvelopeIcon className="w-5 h-5 mr-2" />
          Contact Our Team
        </a>
        <button className="btn-primary text-lg px-6 py-3 group">
          Join Beta Program
          <ArrowRightIcon className="w-5 h-5 ml-2 group-hover:translate-x-1 transition-transform" />
        </button>
      </div>
      
      <div className="mt-6 text-sm text-gray-500">
        <p>
          Email us at{' '}
          <a href="mailto:hello@headkey.ai" className="text-blue-600 hover:text-blue-700">
            hello@headkey.ai
          </a>
          {' '}or schedule a demo call
        </p>
      </div>
    </div>
  );
}