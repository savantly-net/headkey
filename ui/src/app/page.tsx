"use client";

import Link from "next/link";
import {
  ArrowRightIcon,
  BoltIcon,
  ShieldCheckIcon,
  CpuChipIcon,
  CloudIcon,
  CogIcon,
  CheckIcon,
  PlayIcon,
} from "@heroicons/react/24/outline";
import {
  BrainCircuitIcon,
  DatabaseIcon,
  SearchIcon,
  FilterIcon,
  NetworkIcon,
  ZapIcon,
} from "lucide-react";
import InteractiveDemo from "@/components/InteractiveDemo";
import ApiExplorer from "@/components/ApiExplorer";
import PerformanceMetrics from "@/components/PerformanceMetrics";
import FeatureComparison from "@/components/FeatureComparison";
import { SocialLinks } from "@/components/SocialLinks";

const navigation = [
  { name: "Features", href: "#features" },
  { name: "Architecture", href: "#architecture" },
  { name: "Pricing", href: "#pricing" },
  { name: "Documentation", href: "#docs" },
  { name: "About", href: "/about" },
  { name: "Contact", href: "/contact" },
];

const features = [
  {
    name: "Intelligent Information Ingestion",
    description:
      "Automatically processes and validates incoming data from multiple sources with advanced filtering and transformation capabilities.",
    icon: BrainCircuitIcon,
    color: "text-blue-600",
    bgColor: "bg-blue-100",
  },
  {
    name: "Contextual Categorization",
    description:
      "AI-powered categorization engine that organizes information with semantic understanding and contextual awareness.",
    icon: FilterIcon,
    color: "text-purple-600",
    bgColor: "bg-purple-100",
  },
  {
    name: "Memory Encoding System",
    description:
      "Advanced encoding algorithms that transform information into optimized memory structures for efficient storage and retrieval.",
    icon: DatabaseIcon,
    color: "text-green-600",
    bgColor: "bg-green-100",
  },
  {
    name: "Belief Reinforcement",
    description:
      "Intelligent conflict detection and resolution system that maintains coherent knowledge graphs and belief systems.",
    icon: NetworkIcon,
    color: "text-indigo-600",
    bgColor: "bg-indigo-100",
  },
  {
    name: "Smart Forgetting Agent",
    description:
      "Automated relevance evaluation that intelligently prunes outdated information while preserving critical knowledge.",
    icon: ZapIcon,
    color: "text-yellow-600",
    bgColor: "bg-yellow-100",
  },
  {
    name: "Powerful Retrieval Engine",
    description:
      "Lightning-fast similarity search with contextual ranking and multi-modal query support for precise information retrieval.",
    icon: SearchIcon,
    color: "text-red-600",
    bgColor: "bg-red-100",
  },
];

const architectureFeatures = [
  {
    title: "Cloud-Native Architecture",
    description:
      "Built on 12-factor app principles with microservices design for ultimate scalability and reliability.",
    icon: CloudIcon,
  },
  {
    title: "SOLID Design Principles",
    description:
      "Modular, maintainable codebase following object-oriented best practices for easy extension and testing.",
    icon: CogIcon,
  },
  {
    title: "Enterprise Security",
    description:
      "End-to-end encryption, role-based access control, and comprehensive audit logging for enterprise compliance.",
    icon: ShieldCheckIcon,
  },
  {
    title: "High Performance",
    description:
      "Optimized algorithms and caching strategies deliver sub-millisecond response times at enterprise scale.",
    icon: BoltIcon,
  },
];

const pricingTiers = [
  {
    name: "Developer Preview",
    price: "Free",
    period: "",
    description: "Early access for developers and researchers",
    features: [
      "Up to 10,000 memory records",
      "5 API requests per second",
      "All core CIBFE modules",
      "Community support",
      "Documentation access",
      "Beta features preview",
    ],
    cta: "Join Beta",
    popular: false,
  },
  {
    name: "Professional",
    price: "Coming Soon",
    period: "",
    description: "For production applications and growing teams",
    features: [
      "Up to 100,000 memory records",
      "50 API requests per second",
      "Advanced AI categorization",
      "Priority support",
      "Custom integrations",
      "Analytics dashboard",
      "Multi-environment deployment",
    ],
    cta: "Join Waitlist",
    popular: true,
  },
  {
    name: "Enterprise",
    price: "Contact Us",
    period: "",
    description: "Custom solutions for large organizations",
    features: [
      "Unlimited memory records",
      "Unlimited API requests",
      "Custom AI models",
      "Dedicated support",
      "On-premise deployment",
      "Advanced analytics",
      "SLA guarantees",
      "Custom integrations",
    ],
    cta: "Contact Sales",
    popular: false,
  },
];

const designPrinciples = [
  {
    title: "Intelligent Architecture",
    description:
      "Built with six specialized modules that work together to create the most advanced AI memory system available.",
    icon: BrainCircuitIcon,
    color: "text-blue-600",
    bgColor: "bg-blue-100",
  },
  {
    title: "Enterprise-Ready",
    description:
      "Designed from the ground up with enterprise security, scalability, and reliability requirements in mind.",
    icon: ShieldCheckIcon,
    color: "text-green-600",
    bgColor: "bg-green-100",
  },
  {
    title: "Developer-First",
    description:
      "Comprehensive APIs, detailed documentation, and intuitive interfaces that developers love to work with.",
    icon: CogIcon,
    color: "text-purple-600",
    bgColor: "bg-purple-100",
  },
];

const capabilities = [
  { label: "Response Time", value: "< 50ms" },
  { label: "Memory Modules", value: "6" },
  { label: "Supported Formats", value: "Multi-modal" },
  { label: "Uptime Target", value: "99.9%" },
];

export default function HomePage() {
  return (
    <div className="bg-white">
      {/* Navigation */}
      <nav className="fixed top-0 w-full bg-white/80 backdrop-blur-md border-b border-gray-200 z-50">
        <div className="mx-auto max-w-7xl px-6 sm:px-8 lg:px-12">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                  <BrainCircuitIcon className="w-5 h-5 text-white" />
                </div>
                <span className="text-xl font-bold gradient-text">HeadKey</span>
              </div>
            </div>
            <div className="hidden md:block">
              <div className="ml-10 flex items-baseline space-x-8">
                {navigation.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    className="text-gray-700 hover:text-blue-600 px-3 py-2 text-sm font-medium transition-colors"
                  >
                    {item.name}
                  </a>
                ))}
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <button className="btn-ghost">Sign In</button>
              <Link href="/contact" className="btn-primary">Get Started</Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative hero-bg pt-32 pb-20 overflow-hidden">
        <div className="absolute inset-0 tech-grid opacity-30"></div>
        <div className="relative mx-auto max-w-7xl container-padding">
          <div className="text-center">
            <div className="mb-8 fade-in">
              <span className="inline-flex items-center rounded-full bg-blue-100 px-4 py-2 text-sm font-medium text-blue-800">
                <BoltIcon className="w-4 h-4 mr-2" />
                Now Available: CIBFE Architecture v2.0
              </span>
            </div>

            <h1 className="fade-in fade-in-delay-1 text-4xl sm:text-6xl lg:text-7xl font-extrabold text-gray-900 mb-8">
              AI Memory Management
              <span className="block gradient-text">Reimagined</span>
            </h1>

            <p className="fade-in fade-in-delay-2 mx-auto max-w-3xl text-xl sm:text-2xl text-gray-600 mb-12 text-balance leading-relaxed">
              Revolutionary memory system for AI agents that intelligently
              retains, categorizes, and retrieves information. Built on the
              advanced CIBFE architecture for enterprise-scale performance.
            </p>

            <div className="fade-in fade-in-delay-3 flex flex-col sm:flex-row gap-4 justify-center items-center">
              <Link href="/contact" className="btn-primary text-lg px-8 py-4 group">
            Join Beta Program
            <ArrowRightIcon className="ml-2 w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </Link>
          <Link href="/contact" className="btn-secondary text-lg px-8 py-4 group">
            <PlayIcon className="mr-2 w-5 h-5" />
            Watch Demo
          </Link>
            </div>

            <div className="mt-16 fade-in fade-in-delay-3">
              <p className="text-sm text-gray-500 mb-8">
                Built for the next generation of AI applications
              </p>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-8 opacity-60">
                {capabilities.map((capability) => (
                  <div key={capability.label} className="text-center">
                    <div className="text-2xl font-bold text-gray-900">
                      {capability.value}
                    </div>
                    <div className="text-sm text-gray-600">
                      {capability.label}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Floating Elements */}
        <div className="absolute top-1/4 left-10 floating">
          <div className="w-20 h-20 bg-blue-200 rounded-full opacity-20"></div>
        </div>
        <div className="absolute top-1/3 right-10 floating-delay">
          <div className="w-16 h-16 bg-purple-200 rounded-full opacity-20"></div>
        </div>
        <div className="absolute bottom-1/4 left-1/4 floating">
          <div className="w-12 h-12 bg-green-200 rounded-full opacity-20"></div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="section-padding bg-white">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              Six Powerful Modules, One Intelligent System
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto text-balance">
              HeadKey&apos;s CIBFE architecture provides comprehensive memory
              management through six specialized modules that work together to
              create the most advanced AI memory system available.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={feature.name}
                className="card-hover bg-white border border-gray-200 rounded-xl p-8 shadow-sm"
                style={{ animationDelay: `${index * 0.1}s` }}
              >
                <div
                  className={`inline-flex p-3 rounded-lg ${feature.bgColor} mb-6`}
                >
                  <feature.icon className={`w-6 h-6 ${feature.color}`} />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">
                  {feature.name}
                </h3>
                <p className="text-gray-600 leading-relaxed">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Architecture Section */}
      <section id="architecture" className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <div>
              <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-6">
                Enterprise-Grade Architecture
              </h2>
              <p className="text-xl text-gray-600 mb-8 leading-relaxed">
                Built with modern cloud-native principles and SOLID design
                patterns, HeadKey delivers unmatched reliability, scalability,
                and maintainability.
              </p>

              <div className="space-y-6">
                {architectureFeatures.map((feature) => (
                  <div
                    key={feature.title}
                    className="flex items-start space-x-4"
                  >
                    <div className="flex-shrink-0 w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                      <feature.icon className="w-6 h-6 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900 mb-2">
                        {feature.title}
                      </h3>
                      <p className="text-gray-600">{feature.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="relative">
              <div className="aspect-square bg-gradient-to-br from-blue-50 to-purple-50 rounded-2xl p-8 relative overflow-hidden">
                <div className="absolute inset-4 border-2 border-dashed border-blue-300 rounded-xl"></div>
                <div className="relative h-full flex items-center justify-center">
                  <div className="text-center">
                    <CpuChipIcon className="w-20 h-20 text-blue-600 mx-auto mb-4 pulse-glow" />
                    <div className="text-lg font-semibold text-gray-900">
                      CIBFE Core
                    </div>
                    <div className="text-sm text-gray-600">
                      6 Integrated Modules
                    </div>
                  </div>
                </div>

                {/* Floating module indicators */}
                <div className="absolute top-8 left-8 w-12 h-12 bg-white rounded-lg shadow-sm flex items-center justify-center floating">
                  <DatabaseIcon className="w-6 h-6 text-green-600" />
                </div>
                <div className="absolute top-8 right-8 w-12 h-12 bg-white rounded-lg shadow-sm flex items-center justify-center floating-delay">
                  <SearchIcon className="w-6 h-6 text-purple-600" />
                </div>
                <div className="absolute bottom-8 left-8 w-12 h-12 bg-white rounded-lg shadow-sm flex items-center justify-center floating">
                  <NetworkIcon className="w-6 h-6 text-indigo-600" />
                </div>
                <div className="absolute bottom-8 right-8 w-12 h-12 bg-white rounded-lg shadow-sm flex items-center justify-center floating-delay">
                  <ZapIcon className="w-6 h-6 text-yellow-600" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Interactive Demo Section */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              See HeadKey in Action
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto text-balance">
              Watch how the CIBFE architecture processes information through all
              six modules in real-time. Experience the power of intelligent
              memory management.
            </p>
          </div>

          <InteractiveDemo />
        </div>
      </section>

      {/* API Explorer Section */}
      <section id="docs" className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              Powerful REST API
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto text-balance">
              Integrate HeadKey into your applications with our comprehensive
              REST API. Explore endpoints, test requests, and see responses in
              real-time.
            </p>
          </div>

          <ApiExplorer />
        </div>
      </section>

      {/* Performance Metrics Section */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              Enterprise-Grade Performance
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto text-balance">
              Monitor real-time system performance with comprehensive metrics
              and analytics. HeadKey delivers consistent, reliable performance
              at enterprise scale.
            </p>
          </div>

          <PerformanceMetrics />
        </div>
      </section>

      {/* Design Principles Section */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              Built on Proven Principles
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              HeadKey&apos;s CIBFE architecture is designed around core principles
              that ensure reliability, performance, and scalability for
              enterprise AI applications.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {designPrinciples.map((principle, index) => (
              <div key={index} className="card-hover bg-gray-50 rounded-xl p-8">
                <div
                  className={`inline-flex p-3 rounded-lg ${principle.bgColor} mb-6`}
                >
                  <principle.icon className={`w-6 h-6 ${principle.color}`} />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">
                  {principle.title}
                </h3>
                <p className="text-gray-600 leading-relaxed">
                  {principle.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section id="pricing" className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              Early Access Pricing
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              Get early access to HeadKey&apos;s revolutionary CIBFE architecture.
              Join our beta program and help shape the future of AI memory
              management.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 max-w-5xl mx-auto">
            {pricingTiers.map((tier) => (
              <div
                key={tier.name}
                className={`card-hover bg-white rounded-xl p-8 shadow-sm border-2 ${
                  tier.popular ? "border-blue-500 relative" : "border-gray-200"
                }`}
              >
                {tier.popular && (
                  <div className="absolute -top-4 left-1/2 transform -translate-x-1/2">
                    <span className="bg-blue-500 text-white px-4 py-2 rounded-full text-sm font-medium">
                      Most Popular
                    </span>
                  </div>
                )}

                <div className="text-center mb-8">
                  <h3 className="text-xl font-semibold text-gray-900 mb-2">
                    {tier.name}
                  </h3>
                  <div className="flex items-baseline justify-center mb-2">
                    <span className="text-4xl font-bold text-gray-900">
                      {tier.price}
                    </span>
                    <span className="text-gray-600 ml-1">{tier.period}</span>
                  </div>
                  <p className="text-gray-600 text-sm">{tier.description}</p>
                </div>

                <ul className="space-y-4 mb-8">
                  {tier.features.map((feature) => (
                    <li key={feature} className="flex items-center">
                      <CheckIcon className="w-5 h-5 text-green-500 mr-3 flex-shrink-0" />
                      <span className="text-gray-700">{feature}</span>
                    </li>
                  ))}
                </ul>

                <Link
                  href="/contact"
                  className={`w-full py-3 px-6 rounded-lg font-semibold transition-all duration-200 ${
                    tier.popular ? "btn-primary" : "btn-secondary"
                  } text-center block`}
                >
                  {tier.cta}
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Feature Comparison Section */}
      <section className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
              Why Choose HeadKey?
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto text-balance">
              See how HeadKey&apos;s revolutionary CIBFE architecture outperforms
              traditional memory solutions with advanced features designed for
              modern AI applications.
            </p>
          </div>

          <FeatureComparison />
        </div>
      </section>

      {/* CTA Section */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-4xl container-padding text-center">
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-6">
            Ready to Shape the Future of AI Memory?
          </h2>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto text-balance">
            Join our exclusive beta program and be among the first to experience
            HeadKey&apos;s revolutionary CIBFE architecture. Help us build the future
            of AI memory management.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <Link href="/contact" className="btn-primary text-lg px-8 py-4 group">
              Join Beta Program
              <ArrowRightIcon className="ml-2 w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </Link>
            <Link href="/contact" className="btn-ghost text-lg px-8 py-4">
              Schedule Demo
            </Link>
          </div>
          <p className="text-sm text-gray-500 mt-6">
            No credit card required • Early access • Limited beta spots
            available
          </p>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-300">
        <div className="mx-auto max-w-7xl container-padding py-16">
          <div className="grid md:grid-cols-4 gap-8">
            <div className="md:col-span-2">
              <div className="flex items-center space-x-2 mb-4">
                <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                  <BrainCircuitIcon className="w-5 h-5 text-white" />
                </div>
                <span className="text-xl font-bold text-white">HeadKey</span>
              </div>
              <p className="text-gray-400 mb-6 max-w-md">
                Revolutionary AI memory management system built on the CIBFE
                architecture. Intelligent, scalable, and enterprise-ready.
              </p>
              <div className="flex space-x-4">
                <SocialLinks />
              </div>
            </div>

            <div>
              <h3 className="text-white font-semibold mb-4">Product</h3>
              <ul className="space-y-2">
                <li>
                  <a
                    href="#features"
                    className="hover:text-white transition-colors"
                  >
                    Features
                  </a>
                </li>
                <li>
                  <a
                    href="#pricing"
                    className="hover:text-white transition-colors"
                  >
                    Pricing
                  </a>
                </li>
                <li>
                  <a
                    href="#docs"
                    className="hover:text-white transition-colors"
                  >
                    API Docs
                  </a>
                </li>
                <li>
                  <a
                    href="#architecture"
                    className="hover:text-white transition-colors"
                  >
                    Architecture
                  </a>
                </li>
              </ul>
            </div>

            <div>
              <h3 className="text-white font-semibold mb-4">Company</h3>
              <ul className="space-y-2">
                <li>
                  <Link
                    href="/about"
                    className="hover:text-white transition-colors"
                  >
                    About
                  </Link>
                </li>
                <li>
                  <a href="#" className="hover:text-white transition-colors">
                    Blog
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-white transition-colors">
                    Careers
                  </a>
                </li>
                <li>
                  <Link
                    href="/contact"
                    className="hover:text-white transition-colors"
                  >
                    Contact
                  </Link>
                </li>
              </ul>
              <div className="mt-6">
                <p className="text-sm text-gray-400 mb-2">Get in touch</p>
                <a
                  href="mailto:support@savantly.net"
                  className="text-sm text-blue-400 hover:text-blue-300 transition-colors"
                >
                  support@savantly.net
                </a>
              </div>
            </div>
          </div>

          <div className="border-t border-gray-800 mt-12 pt-8 flex flex-col md:flex-row justify-between items-center">
            <p className="text-sm text-gray-400">
              © 2025 HeadKey by Savantly. All rights reserved.
            </p>
            <div className="flex space-x-6 mt-4 md:mt-0">
              <a
                href="#"
                className="text-sm text-gray-400 hover:text-white transition-colors"
              >
                Privacy
              </a>
              <a
                href="#"
                className="text-sm text-gray-400 hover:text-white transition-colors"
              >
                Terms
              </a>
              <a
                href="#"
                className="text-sm text-gray-400 hover:text-white transition-colors"
              >
                Security
              </a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
