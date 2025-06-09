"use client";

import {
  CheckCircleIcon,
  EnvelopeIcon,
  ClockIcon,
  ArrowLeftIcon,
  ChatBubbleLeftRightIcon,
} from "@heroicons/react/24/outline";
import { BrainCircuitIcon } from "lucide-react";

export default function ThankYouPage() {
  return (
    <div className="bg-white min-h-screen">
      {/* Navigation */}
      <nav className="bg-white border-b border-gray-200">
        <div className="mx-auto max-w-7xl px-6 sm:px-8 lg:px-12">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center">
              <a href="/" className="flex items-center space-x-2">
                <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                  <BrainCircuitIcon className="w-5 h-5 text-white" />
                </div>
                <span className="text-xl font-bold gradient-text">HeadKey</span>
              </a>
            </div>
            <div className="flex items-center space-x-4">
              <a href="/" className="btn-ghost">
                <ArrowLeftIcon className="w-4 h-4 mr-2" />
                Back to Home
              </a>
              <button className="btn-primary">Join Beta</button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-1 flex items-center justify-center px-6 py-12">
        <div className="max-w-2xl w-full text-center">
          {/* Success Icon */}
          <div className="mb-8">
            <div className="w-24 h-24 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <CheckCircleIcon className="w-12 h-12 text-green-600" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Thank You!
            </h1>
            <h2 className="text-xl text-gray-600 mb-8">
              Your message has been received
            </h2>
          </div>

          {/* Success Message */}
          <div className="mb-12">
            <p className="text-lg text-gray-600 mb-6 leading-relaxed">
              We've successfully received your message and our team will review
              it shortly. We typically respond within 24 hours during business
              days.
            </p>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-8">
              <div className="flex items-center justify-center space-x-2 text-blue-800 mb-3">
                <EnvelopeIcon className="w-5 h-5" />
                <span className="font-medium">What happens next?</span>
              </div>
              <div className="text-sm text-blue-700 space-y-2">
                <p>• Our team will review your inquiry</p>
                <p>• You'll receive a response within 24 hours</p>
                <p>• For urgent matters, we'll prioritize accordingly</p>
              </div>
            </div>
          </div>

          {/* Next Steps */}
          <div className="space-y-6 mb-12">
            <div className="grid md:grid-cols-2 gap-6">
              <div className="bg-white border border-gray-200 rounded-xl p-6 text-left">
                <div className="flex items-center space-x-3 mb-4">
                  <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                    <BrainCircuitIcon className="w-5 h-5 text-purple-600" />
                  </div>
                  <h3 className="font-semibold text-gray-900">
                    Explore HeadKey
                  </h3>
                </div>
                <p className="text-gray-600 mb-4 text-sm">
                  Learn more about our revolutionary CIBFE architecture and how
                  it can transform your AI applications.
                </p>
                <a
                  href="/"
                  className="text-purple-600 hover:text-purple-700 text-sm font-medium"
                >
                  Visit Homepage →
                </a>
              </div>

              <div className="bg-white border border-gray-200 rounded-xl p-6 text-left">
                <div className="flex items-center space-x-3 mb-4">
                  <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                    <ChatBubbleLeftRightIcon className="w-5 h-5 text-blue-600" />
                  </div>
                  <h3 className="font-semibold text-gray-900">
                    Join Beta Program
                  </h3>
                </div>
                <p className="text-gray-600 mb-4 text-sm">
                  Get early access to HeadKey's beta platform and be among the
                  first to experience our technology.
                </p>
                <button className="text-blue-600 hover:text-blue-700 text-sm font-medium">
                  Join Beta →
                </button>
              </div>
            </div>
          </div>

          {/* Response Time Info */}
          <div className="bg-gray-50 rounded-lg p-6 mb-8">
            <div className="flex items-center justify-center space-x-2 text-gray-700 mb-3">
              <ClockIcon className="w-5 h-5" />
              <span className="font-medium">Response Times</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
              <div className="text-center">
                <div className="font-semibold text-gray-900">
                  General Inquiries
                </div>
                <div className="text-gray-600">Within 24 hours</div>
              </div>
              <div className="text-center">
                <div className="font-semibold text-gray-900">Beta Access</div>
                <div className="text-gray-600">Within 12 hours</div>
              </div>
              <div className="text-center">
                <div className="font-semibold text-gray-900">
                  Technical Support
                </div>
                <div className="text-gray-600">Within 8 hours</div>
              </div>
            </div>
          </div>

          {/* Alternative Contact */}
          <div className="text-center">
            <p className="text-sm text-gray-500 mb-4">
              Need immediate assistance? You can also reach us directly:
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center text-sm">
              <a
                href="mailto:hello@headkey.ai"
                className="flex items-center space-x-2 text-blue-600 hover:text-blue-700"
              >
                <EnvelopeIcon className="w-4 h-4" />
                <span>hello@headkey.ai</span>
              </a>
              <span className="hidden sm:block text-gray-300">|</span>
              <a
                href="/contact"
                className="flex items-center space-x-2 text-blue-600 hover:text-blue-700"
              >
                <ChatBubbleLeftRightIcon className="w-4 h-4" />
                <span>Live Chat</span>
              </a>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-300">
        <div className="mx-auto max-w-7xl container-padding py-12">
          <div className="text-center">
            <div className="flex items-center justify-center space-x-2 mb-4">
              <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                <BrainCircuitIcon className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-bold text-white">HeadKey</span>
            </div>
            <p className="text-gray-400 mb-6">
              Revolutionary AI memory management system built on the CIBFE
              architecture.
            </p>
            <div className="text-sm text-gray-500">
              © 2025 HeadKey by Savantly. All rights reserved.
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
