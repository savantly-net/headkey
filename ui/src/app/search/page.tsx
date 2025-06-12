"use client";

import { useState } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { toast } from "sonner";
import { memoryApi, handleApiError } from "@/lib/api";
import {
  Search,
  Brain,
  Calendar,
  Tag,
  ArrowLeft,
  Loader2,
  AlertCircle,
  FileText,
} from "lucide-react";
import Link from "next/link";

interface SearchResult {
  memory_id: string;
  content: string;
  relevance_score: number;
  category: string;
  metadata: Record<string, unknown>;
  created_at: string;
}

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [agentId, setAgentId] = useState("default-agent");
  const [isLoading, setIsLoading] = useState(false);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!query.trim()) {
      toast.error("Please enter a search query");
      return;
    }

    if (!agentId.trim()) {
      toast.error("Please enter an agent ID");
      return;
    }

    setIsLoading(true);
    console.log("Searching memories:", { query, agentId });

    try {
      const response = await memoryApi.search({
        agent_id: agentId,
        query: query,
        limit: 20,
        similarity_threshold: 0.1,
      });

      console.log("Search response:", response);

      setResults(response.results || []);
      setTotalCount(response.total_count || 0);
      setHasSearched(true);

      toast.success(`Found ${response.total_count || 0} memories`);
    } catch (error) {
      console.error("Search failed:", error);
      const apiError = handleApiError(error);
      toast.error(`Search failed: ${apiError.message}`);
      setResults([]);
      setTotalCount(0);
      setHasSearched(true);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateString;
    }
  };

  const getRelevanceColor = (score: number) => {
    if (score >= 0.8) return "bg-green-100 text-green-800";
    if (score >= 0.6) return "bg-blue-100 text-blue-800";
    if (score >= 0.4) return "bg-yellow-100 text-yellow-800";
    return "bg-gray-100 text-gray-800";
  };

  return (
    <div className="container mx-auto py-8 space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link href="/">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Dashboard
              </Button>
            </Link>
          </div>
          <h1 className="text-3xl font-bold">Memory Search</h1>
          <p className="text-muted-foreground">
            Search and retrieve memories from the HeadKey system
          </p>
        </div>
        <Search className="h-8 w-8 text-muted-foreground" />
      </div>

      {/* Search Form */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Brain className="h-5 w-5" />
            Search Memories
          </CardTitle>
          <CardDescription>
            Enter your search query to find relevant memories stored in the
            system
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="agentId">Agent ID</Label>
                <Input
                  id="agentId"
                  placeholder="Enter agent ID (e.g., default-agent)"
                  value={agentId}
                  onChange={(e) => setAgentId(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="query">Search Query</Label>
                <Input
                  id="query"
                  placeholder="What are you looking for?"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  required
                />
              </div>
            </div>
            <Button type="submit" disabled={isLoading} className="w-full">
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Searching...
                </>
              ) : (
                <>
                  <Search className="h-4 w-4 mr-2" />
                  Search Memories
                </>
              )}
            </Button>
          </form>
        </CardContent>
      </Card>

      {/* Search Results */}
      {hasSearched && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold">Search Results</h2>
            <Badge variant="secondary">
              {totalCount} result{totalCount !== 1 ? "s" : ""}
            </Badge>
          </div>

          {results.length === 0 ? (
            <Card>
              <CardContent className="py-8">
                <div className="text-center space-y-4">
                  <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto" />
                  <div>
                    <h3 className="text-lg font-semibold">No memories found</h3>
                    <p className="text-muted-foreground">
                      Try adjusting your search query or check if memories have
                      been ingested for this agent.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-4">
              {results.map((result, index) => (
                <Card
                  key={result.memory_id}
                  className="hover:shadow-md transition-shadow"
                >
                  <CardHeader className="pb-3">
                    <div className="flex items-start justify-between">
                      <div className="space-y-1">
                        <CardTitle className="text-lg flex items-center gap-2">
                          <FileText className="h-4 w-4" />
                          Memory #{index + 1}
                        </CardTitle>
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                          <Calendar className="h-3 w-3" />
                          {formatDate(result.created_at)}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge
                          className={getRelevanceColor(result.relevance_score)}
                        >
                          {Math.round(result.relevance_score * 100)}% match
                        </Badge>
                        <Badge variant="outline">
                          <Tag className="h-3 w-3 mr-1" />
                          {result.category}
                        </Badge>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <h4 className="font-medium mb-2">Content</h4>
                      <p className="text-sm leading-relaxed bg-muted p-3 rounded-md">
                        {result.content}
                      </p>
                    </div>

                    {result.metadata &&
                      Object.keys(result.metadata).length > 0 && (
                        <>
                          <Separator />
                          <div>
                            <h4 className="font-medium mb-2">Metadata</h4>
                            <div className="grid gap-2 text-sm">
                              {Object.entries(result.metadata).map(
                                ([key, value]) => (
                                  <div
                                    key={key}
                                    className="flex justify-between"
                                  >
                                    <span className="text-muted-foreground capitalize">
                                      {key.replace(/_/g, " ")}:
                                    </span>
                                    <span className="font-medium">
                                      {typeof value === "object"
                                        ? JSON.stringify(value)
                                        : String(value)}
                                    </span>
                                  </div>
                                ),
                              )}
                            </div>
                          </div>
                        </>
                      )}

                    <div className="flex items-center justify-between pt-2 text-xs text-muted-foreground">
                      <span>Memory ID: {result.memory_id}</span>
                      <span>
                        Relevance: {result.relevance_score.toFixed(3)}
                      </span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
