"use client";

import Link from "next/link";
import {
  HomeIcon,
  MagnifyingGlassIcon,
  ExclamationTriangleIcon,
} from "@heroicons/react/24/outline";
import { BrainCircuitIcon } from "lucide-react";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Navigation */}
      <nav className="bg-white border-b border-gray-200">
        <div className="mx-auto max-w-7xl px-6 sm:px-8 lg:px-12">
          <div className="flex h-16 items-center justify-between">
            <Link href="/" className="flex items-center space-x-2">
              <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                <BrainCircuitIcon className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-bold gradient-text">HeadKey</span>
            </Link>
            <Link href="/" className="btn-primary">
              Back to Home
            </Link>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-1 flex items-center justify-center px-6 py-12">
        <div className="max-w-md w-full text-center">
          {/* Error Icon */}
          <div className="mb-8">
            <div className="w-24 h-24 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <ExclamationTriangleIcon className="w-12 h-12 text-yellow-600" />
            </div>
            <h1 className="text-6xl font-bold text-gray-900 mb-2">404</h1>
            <h2 className="text-2xl font-semibold text-gray-700 mb-4">
              Page Not Found
            </h2>
          </div>

          {/* Description */}
          <div className="mb-8">
            <p className="text-lg text-gray-600 mb-6">
              Oops! The page you&apos;re looking for seems to have been forgotten by
              our memory system. Don&apos;t worry, our CIBFE architecture is working
              to retrieve it.
            </p>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
              <div className="flex items-center space-x-2 text-blue-800">
                <MagnifyingGlassIcon className="w-5 h-5" />
                <span className="text-sm font-medium">
                  Memory Search Status:
                </span>
              </div>
              <div className="mt-2 text-sm text-blue-700">
                Page not found in current memory index. Suggesting alternative
                routes...
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="space-y-4">
            <Link
              href="/"
              className="btn-primary w-full flex items-center justify-center"
            >
              <HomeIcon className="w-5 h-5 mr-2" />
              Return to Homepage
            </Link>

            <div className="grid grid-cols-3 gap-4">
              <Link href="/about" className="btn-secondary text-center">
                About Us
              </Link>
              <Link href="/contact" className="btn-secondary text-center">
                Contact
              </Link>
              <Link href="/#docs" className="btn-secondary text-center">
                Documentation
              </Link>
            </div>
          </div>

          {/* Popular Links */}
          <div className="mt-12 pt-8 border-t border-gray-200">
            <h3 className="text-sm font-medium text-gray-500 mb-4">
              Popular Pages
            </h3>
            <div className="space-y-2">
              <Link
                href="/#features"
                className="block text-blue-600 hover:text-blue-700 text-sm"
              >
                → Features & Capabilities
              </Link>
              <Link
                href="/#pricing"
                className="block text-blue-600 hover:text-blue-700 text-sm"
              >
                → Pricing Plans
              </Link>
              <Link
                href="/#architecture"
                className="block text-blue-600 hover:text-blue-700 text-sm"
              >
                → CIBFE Architecture
              </Link>
              <Link
                href="/contact"
                className="block text-blue-600 hover:text-blue-700 text-sm"
              >
                → Get Support
              </Link>
            </div>
          </div>

          {/* Help Text */}
          <div className="mt-8 text-xs text-gray-500">
            If you believe this is an error, please{" "}
            <a
              href="mailto:support@headkey.ai"
              className="text-blue-600 hover:text-blue-700"
            >
              contact our support team
            </a>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 py-6">
        <div className="mx-auto max-w-7xl px-6 text-center">
          <p className="text-sm text-gray-500">
            © 2025 HeadKey by Savantly. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
