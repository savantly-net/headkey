"use client";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import Link from "next/link";
import { DashboardStats } from "@/components/dashboard-stats";
import { Upload, Search, BarChart3, ArrowRight } from "lucide-react";

export default function HomePage() {
  const quickActions = [
    {
      title: "Ingest Memory",
      description: "Add new memories to the system",
      href: "/ingest",
      icon: Upload,
      color: "bg-blue-500",
    },
    {
      title: "Search Memories",
      description: "Find and retrieve stored memories",
      href: "/search",
      icon: Search,
      color: "bg-green-500",
    },
    {
      title: "System Monitor",
      description: "View detailed system health and metrics",
      href: "/monitor",
      icon: BarChart3,
      color: "bg-purple-500",
    },
  ];

  return (
    <div className="container mx-auto py-8 space-y-8">
      {/* Dashboard Stats */}
      <DashboardStats />

      {/* Quick Actions */}
      <div className="space-y-4">
        <h2 className="text-2xl font-bold">Quick Actions</h2>
        <div className="grid gap-4 md:grid-cols-3">
          {quickActions.map((action) => (
            <Link key={action.href} href={action.href}>
              <Card className="hover:shadow-lg transition-shadow cursor-pointer">
                <CardHeader className="pb-3">
                  <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-lg ${action.color}`}>
                      <action.icon className="h-5 w-5 text-white" />
                    </div>
                    <CardTitle className="text-lg">{action.title}</CardTitle>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground">{action.description}</p>
                  <div className="flex items-center mt-3 text-sm font-medium text-primary">
                    Get started
                    <ArrowRight className="ml-1 h-4 w-4" />
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </div>

      {/* System Architecture Overview */}
      <Card>
        <CardHeader>
          <CardTitle>CIBFE Architecture Overview</CardTitle>
          <CardDescription>
            Cognitive Ingestion & Belief Formation Engine modules
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            <div className="space-y-2">
              <h4 className="font-semibold">Information Ingestion (IIM)</h4>
              <p className="text-sm text-muted-foreground">
                Orchestrates the complete ingestion pipeline
              </p>
            </div>
            <div className="space-y-2">
              <h4 className="font-semibold">Contextual Categorization (CCE)</h4>
              <p className="text-sm text-muted-foreground">
                Content categorization and intelligent tagging
              </p>
            </div>
            <div className="space-y-2">
              <h4 className="font-semibold">Memory Encoding (MES)</h4>
              <p className="text-sm text-muted-foreground">
                Persistent storage and efficient retrieval
              </p>
            </div>
            <div className="space-y-2">
              <h4 className="font-semibold">Belief Reinforcement (BRCA)</h4>
              <p className="text-sm text-muted-foreground">
                Belief consistency and conflict management
              </p>
            </div>
            <div className="space-y-2">
              <h4 className="font-semibold">Relevance Evaluation (REFA)</h4>
              <p className="text-sm text-muted-foreground">
                Memory lifecycle and forgetting mechanisms
              </p>
            </div>
            <div className="space-y-2">
              <h4 className="font-semibold">Retrieval & Response (RRE)</h4>
              <p className="text-sm text-muted-foreground">
                Search and response generation engine
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
