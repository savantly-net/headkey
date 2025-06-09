"use client";

import { 
  UserGroupIcon, 
  GlobeAltIcon,
  LightBulbIcon,
  ShieldCheckIcon,
  BoltIcon
} from '@heroicons/react/24/outline';
import { BrainCircuitIcon } from 'lucide-react';
import ContactCTA from '@/components/ContactCTA';
import Link from 'next/link';

const values = [
  {
    title: "Innovation First",
    description:
      "We push the boundaries of AI memory management with cutting-edge research and development.",
    icon: LightBulbIcon,
    color: "text-yellow-600",
    bgColor: "bg-yellow-100",
  },
  {
    title: "Security & Privacy",
    description:
      "Enterprise-grade security with end-to-end encryption and comprehensive privacy controls.",
    icon: ShieldCheckIcon,
    color: "text-green-600",
    bgColor: "bg-green-100",
  },
  {
    title: "Performance Excellence",
    description:
      "Uncompromising performance standards with sub-millisecond response times at scale.",
    icon: BoltIcon,
    color: "text-blue-600",
    bgColor: "bg-blue-100",
  },
  {
    title: "Global Impact",
    description:
      "Building tools that enable AI systems worldwide to become more intelligent and efficient.",
    icon: GlobeAltIcon,
    color: "text-purple-600",
    bgColor: "bg-purple-100",
  },
];

const team = [
  {
    name: "Dr. Sarah Chen",
    role: "CEO & Co-Founder",
    bio: "Former AI Research Lead at Google DeepMind. PhD in Cognitive Science from MIT.",
    image: "/team/sarah.jpg",
  },
  {
    name: "Michael Rodriguez",
    role: "CTO & Co-Founder",
    bio: "Ex-Principal Engineer at OpenAI. Expert in distributed systems and neural architectures.",
    image: "/team/michael.jpg",
  },
  {
    name: "Dr. Emily Watson",
    role: "Head of Research",
    bio: "Leading researcher in memory systems and cognitive architectures. PhD from Stanford.",
    image: "/team/emily.jpg",
  },
  {
    name: "David Kim",
    role: "VP of Engineering",
    bio: "Former Engineering Manager at Anthropic. Specialist in scalable AI infrastructure.",
    image: "/team/david.jpg",
  },
];

const milestones = [
  {
    year: "2024",
    title: "Company Founded",
    description:
      "HeadKey was founded with the vision to revolutionize AI memory management.",
  },
  {
    year: "2024",
    title: "CIBFE Architecture",
    description:
      "Development of the Cognitive Ingestion & Belief Formation Engine architecture.",
  },
  {
    year: "2024",
    title: "Beta Development",
    description:
      "Core platform development and preparation for beta testing program.",
  },
  {
    year: "2024",
    title: "Beta Launch",
    description:
      "Opening beta access to developers and early adopters to validate the platform.",
  },
];

export default function AboutPage() {
  return (
    <div className="bg-white min-h-screen">
      {/* Navigation */}
      <nav className="bg-white border-b border-gray-200">
        <div className="mx-auto max-w-7xl px-6 sm:px-8 lg:px-12">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center">
              <Link href="/" className="flex items-center space-x-2">
                <div className="w-8 h-8 gradient-bg rounded-lg flex items-center justify-center">
                  <BrainCircuitIcon className="w-5 h-5 text-white" />
                </div>
                <span className="text-xl font-bold gradient-text">HeadKey</span>
              </Link>
            </div>
            <div className="flex items-center space-x-4">
              <Link href="/" className="btn-ghost">
                ← Back to Home
              </Link>
              <Link href="/contact" className="btn-ghost">
                Contact
              </Link>
              <Link href="/contact" className="btn-primary">Get Started</Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="section-padding hero-bg">
        <div className="mx-auto max-w-4xl container-padding text-center">
          <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 mb-6">
            Pioneering the Future of
            <span className="block gradient-text">AI Memory Management</span>
          </h1>
          <p className="text-xl text-gray-600 mb-8 text-balance leading-relaxed">
            At HeadKey, we&apos;re building the next generation of memory systems
            that enable AI agents to think, learn, and remember with
            unprecedented intelligence and efficiency.
          </p>
        </div>
      </section>

      {/* Mission & Vision */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            <div>
              <h2 className="text-3xl font-bold text-gray-900 mb-6">
                Our Mission
              </h2>
              <p className="text-lg text-gray-600 mb-6 leading-relaxed">
                We believe that intelligent memory management is the key to
                unlocking the full potential of AI systems. Our mission is to
                provide the most advanced, scalable, and reliable memory
                infrastructure for AI applications worldwide.
              </p>
              <p className="text-lg text-gray-600 leading-relaxed">
                Through our revolutionary CIBFE architecture, we&apos;re enabling AI
                agents to not just store information, but to truly understand,
                contextualize, and intelligently manage their knowledge over
                time.
              </p>
            </div>
            <div className="relative">
              <div className="aspect-square bg-gradient-to-br from-blue-50 to-purple-50 rounded-2xl p-8">
                <div className="h-full flex items-center justify-center">
                  <div className="text-center">
                    <BrainCircuitIcon className="w-24 h-24 text-blue-600 mx-auto mb-6 pulse-glow" />
                    <div className="text-xl font-semibold text-gray-900 mb-2">
                      CIBFE Architecture
                    </div>
                    <div className="text-gray-600">
                      6 Integrated Cognitive Modules
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Values */}
      <section className="section-padding bg-gray-50">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">
              Our Values
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              The principles that guide everything we do and shape our approach
              to building the future of AI memory.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {values.map((value) => (
              <div
                key={value.title}
                className="card-hover bg-white rounded-xl p-6 shadow-sm border border-gray-200"
              >
                <div
                  className={`inline-flex p-3 rounded-lg ${value.bgColor} mb-4`}
                >
                  <value.icon className={`w-6 h-6 ${value.color}`} />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-3">
                  {value.title}
                </h3>
                <p className="text-gray-600 leading-relaxed">
                  {value.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Team */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-7xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">
              Meet Our Team
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              World-class experts in AI, distributed systems, and cognitive
              science working together to revolutionize memory management.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {team.map((member) => (
              <div
                key={member.name}
                className="card-hover bg-white rounded-xl p-6 shadow-sm border border-gray-200 text-center"
              >
                <div className="w-20 h-20 bg-gray-300 rounded-full mx-auto mb-4 flex items-center justify-center">
                  <UserGroupIcon className="w-10 h-10 text-gray-600" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-1">
                  {member.name}
                </h3>
                <div className="text-blue-600 font-medium mb-3">
                  {member.role}
                </div>
                <p className="text-sm text-gray-600 leading-relaxed">
                  {member.bio}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Timeline */}
      <section className="section-padding bg-gray-50">
        <div className="mx-auto max-w-4xl container-padding">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">
              Our Journey
            </h2>
            <p className="text-xl text-gray-600">
              Key milestones in HeadKey&apos;s mission to transform AI memory
              management.
            </p>
          </div>

          <div className="space-y-8">
            {milestones.map((milestone, index) => (
              <div key={milestone.year} className="flex items-start space-x-6">
                <div className="flex-shrink-0">
                  <div className="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold">
                    {index + 1}
                  </div>
                </div>
                <div className="flex-1 bg-white rounded-lg p-6 shadow-sm border border-gray-200">
                  <div className="flex items-center space-x-3 mb-2">
                    <span className="text-blue-600 font-bold text-lg">
                      {milestone.year}
                    </span>
                    <h3 className="text-xl font-semibold text-gray-900">
                      {milestone.title}
                    </h3>
                  </div>
                  <p className="text-gray-600">{milestone.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Contact CTA */}
      <section className="section-padding bg-white">
        <div className="mx-auto max-w-4xl container-padding">
          <ContactCTA
            title="Join Our Mission"
            description="We&apos;re always looking for talented individuals who share our passion for advancing AI technology. Get in touch to explore opportunities or learn more about HeadKey."
          />
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
