"use client";

import Contact from "@/components/Contact";
import {
  ChatBubbleLeftRightIcon,
  ClockIcon,
  EnvelopeIcon,
  MapPinIcon,
  PhoneIcon,
  QuestionMarkCircleIcon,
} from "@heroicons/react/24/outline";
import { BrainCircuitIcon } from "lucide-react";

const contactInfo = [
  {
    icon: EnvelopeIcon,
    title: "Email Us",
    description: "Send us an email and we'll respond within 24 hours",
    value: "hello@headkey.ai",
    href: "mailto:hello@headkey.ai",
    color: "text-blue-600",
    bgColor: "bg-blue-100",
  },
  {
    icon: ChatBubbleLeftRightIcon,
    title: "Live Chat",
    description: "Chat with our team during business hours",
    value: "Start a conversation",
    href: "#",
    color: "text-green-600",
    bgColor: "bg-green-100",
  },
  {
    icon: QuestionMarkCircleIcon,
    title: "Help Center",
    description: "Browse our documentation and FAQs",
    value: "Visit Help Center",
    href: "#",
    color: "text-purple-600",
    bgColor: "bg-purple-100",
  },
];

const officeInfo = [
  {
    icon: MapPinIcon,
    title: "Headquarters",
    value: "Fort Worth, TX",
    description: "United States",
  },
  {
    icon: ClockIcon,
    title: "Business Hours",
    value: "9:00 AM - 6:00 PM CT",
    description: "Monday - Friday",
  },
  {
    icon: PhoneIcon,
    title: "Phone",
    value: "+1 (555) 123-4567",
    description: "Call during business hours",
  },
];

const faqs = [
  {
    question: "How do I get started with HeadKey?",
    answer:
      'Join our beta program by clicking the "Join Beta" button on our homepage. We\'ll send you access credentials and documentation to get started.',
  },
  {
    question: "What kind of support do you offer?",
    answer:
      "We provide comprehensive support including documentation, community forums, email support, and for enterprise customers, dedicated support channels.",
  },
  {
    question: "Is HeadKey ready for production use?",
    answer:
      "HeadKey is currently in beta. We're working with select partners to validate the platform before general availability. Join our beta to be among the first to experience the technology.",
  },
  {
    question: "What are the pricing plans?",
    answer:
      "We offer a free developer preview during beta, with professional and enterprise plans coming soon. Early beta participants will receive preferential pricing when we launch.",
  },
];

export default function ContactPage() {
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
                ← Back to Home
              </a>
              <button className="btn-primary">Join Beta</button>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="hero-bg py-16">
        <div className="mx-auto max-w-4xl container-padding text-center">
          <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 mb-6">
            Get in Touch
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-3xl mx-auto text-balance">
            Have questions about HeadKey's CIBFE architecture? Want to join our
            beta program? We'd love to hear from you. Our team is here to help.
          </p>
        </div>
      </section>

      {/* Contact Form */}
      <section className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          <Contact />
        </div>
      </section>

      <section className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          {/* FAQs */}
          <div className="space-y-8">
            {/* Quick FAQs */}
            <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-200">
              <h3 className="text-xl font-semibold text-gray-900 mb-6">
                Quick Questions
              </h3>
              <div className="space-y-6">
                {faqs.map((faq, index) => (
                  <div key={index}>
                    <h4 className="font-medium text-gray-900 mb-2">
                      {faq.question}
                    </h4>
                    <p className="text-sm text-gray-600 leading-relaxed">
                      {faq.answer}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Beta CTA */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-4xl container-padding text-center">
          <div className="bg-blue-50 rounded-2xl p-8 border border-blue-200">
            <BrainCircuitIcon className="w-12 h-12 text-blue-600 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-gray-900 mb-4">
              Ready to Get Started?
            </h2>
            <p className="text-lg text-gray-600 mb-6 max-w-2xl mx-auto">
              Join our beta program and be among the first to experience
              HeadKey's revolutionary CIBFE architecture for AI memory
              management.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button className="btn-primary">Join Beta Program</button>
              <button className="btn-secondary">Schedule Demo</button>
            </div>
          </div>
        </div>
      </section>

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
