"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Progress } from "@/components/ui/progress";
import { toast } from "sonner";
import { memoryApi, handleApiError } from "@/lib/api";
import { 
  Brain, 
  Database, 
  Upload, 
  CheckCircle, 
  AlertCircle, 
  Eye,
  Settings
} from "lucide-react";

interface MemoryRecord {
  id?: string;
  agent_id: string;
  content: string;
  source: string;
  metadata: {
    importance?: number;
    tags?: string[];
    category?: string;
    [key: string]: any;
  };
}

interface ValidationResult {
  valid: boolean;
  errors?: string[];
  warnings?: string[];
}

interface IngestionResult {
  success: boolean;
  memory_id?: string;
  message?: string;
  category?: {
    name: string;
    confidence: number;
    tags: string[];
  };
  relevance_score?: number;
  agent_id?: string;
  encoded?: boolean;
  dry_run?: boolean;
  updated_beliefs?: any[];
  processing_time_ms?: number;
  timestamp?: string;
  belief_update_result?: {
    reinforcedBeliefs: any[];
    conflicts: any[];
    newBeliefs: any[];
    weakenedBeliefs: any[];
    agentId: string;
    summary: string;
    totalBeliefChanges: number;
  };
}

export default function IngestPage() {
  const [memoryRecord, setMemoryRecord] = useState<MemoryRecord>({
    agent_id: "",
    content: "",
    source: "manual",
    metadata: {
      importance: 0.5,
      tags: [],
    }
  });

  const [isLoading, setIsLoading] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [ingestionResult, setIngestionResult] = useState<IngestionResult | null>(null);
  const [lastOperationType, setLastOperationType] = useState<'validate' | 'dry-run' | 'ingest' | null>(null);
  const [activeTab, setActiveTab] = useState("ingest");

  const handleInputChange = (field: keyof MemoryRecord, value: any) => {
    setMemoryRecord(prev => ({
      ...prev,
      [field]: value
    }));
    // Clear previous validation when input changes
    setValidationResult(null);
    setIngestionResult(null);
    setLastOperationType(null);
  };

  const handleMetadataChange = (key: string, value: any) => {
    setMemoryRecord(prev => ({
      ...prev,
      metadata: {
        ...prev.metadata,
        [key]: value
      }
    }));
  };

  const handleTagsChange = (tagsString: string) => {
    const tags = tagsString.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
    handleMetadataChange('tags', tags);
  };

  const validateInput = async () => {
    setIsLoading(true);
    setIngestionResult(null);
    setLastOperationType('validate');
    try {
      const result = await memoryApi.validate({
        agent_id: memoryRecord.agent_id,
        content: memoryRecord.content
      });
      
      setValidationResult(result);
      
      if (result.valid) {
        toast.success("Input validation passed!");
      } else {
        toast.error("Input validation failed. Please check the errors.");
      }
    } catch (error) {
      console.error('Validation error:', error);
      const apiError = handleApiError(error);
      toast.error(`Validation failed: ${apiError.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const performDryRun = async () => {
    setIsLoading(true);
    setValidationResult(null);
    setLastOperationType('dry-run');
    try {
      const result = await memoryApi.dryRun(memoryRecord);
      setIngestionResult(result);
      toast.success("Dry run completed successfully!");
    } catch (error) {
      console.error('Dry run error:', error);
      const apiError = handleApiError(error);
      toast.error(`Dry run failed: ${apiError.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const ingestMemory = async () => {
    setIsLoading(true);
    setValidationResult(null);
    setLastOperationType('ingest');
    try {
      const result = await memoryApi.ingest(memoryRecord);
      setIngestionResult(result);
      
      if (result.success) {
        toast.success("Memory ingested successfully!");
        // Reset form
        setMemoryRecord({
          agent_id: memoryRecord.agent_id, // Keep agent_id for convenience
          content: "",
          source: "manual",
          metadata: {
            importance: 0.5,
            tags: [],
          }
        });
      } else {
        toast.error(result.message || "Memory ingestion failed. Please try again.");
      }
    } catch (error) {
      console.error('Ingestion error:', error);
      const apiError = handleApiError(error);
      toast.error(`Ingestion failed: ${apiError.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const isFormValid = memoryRecord.agent_id.trim() !== '' && memoryRecord.content.trim() !== '';

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
      <div className="max-w-4xl mx-auto panel-soft rounded-soft shadow-medium">
        {/* Header */}
        <div className="text-center mb-8 p-6">
          <div className="flex items-center justify-center mb-4">
            <Brain className="h-12 w-12 text-blue-600 mr-3" />
            <h1 className="text-4xl font-bold gradient-text">
              HeadKey Memory Ingestion
            </h1>
          </div>
          <p className="text-gray-600 text-lg">
            Ingest information into the AI memory system with intelligent categorization and processing
          </p>
        </div>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full p-6">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="ingest" className="flex items-center gap-2">
              <Upload className="h-4 w-4" />
              Ingest Memory
            </TabsTrigger>
            <TabsTrigger value="validate" className="flex items-center gap-2">
              <Eye className="h-4 w-4" />
              Validate & Preview
            </TabsTrigger>
          </TabsList>

          <TabsContent value="ingest" className="space-y-6">
            <Card className="card-soft shadow-soft border-soft">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Database className="h-5 w-5" />
                  Memory Information
                </CardTitle>
                <CardDescription>
                  Enter the information you want to ingest into the memory system
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                {/* Agent ID */}
                <div className="space-y-2">
                  <Label htmlFor="agent-id">Agent ID</Label>
                  <Input
                    id="agent_id"
                    value={memoryRecord.agent_id}
                    onChange={(e) => handleInputChange('agent_id', e.target.value)}
                    placeholder="Enter a unique identifier for the agent"
                    className="w-full input-soft"
                  />
                  <p className="text-sm text-gray-500">
                    Unique identifier for the agent this memory belongs to
                  </p>
                </div>

                {/* Content */}
                <div className="space-y-2">
                  <Label htmlFor="content">Memory Content</Label>
                  <Textarea
                    id="content"
                    placeholder="Enter the information to be stored in memory..."
                    value={memoryRecord.content}
                    onChange={(e) => handleInputChange('content', e.target.value)}
                    rows={6}
                    className="w-full"
                  />
                  <p className="text-sm text-gray-500">
                    The actual content to be processed and stored
                  </p>
                </div>

                {/* Source */}
                <div className="space-y-2">
                  <Label htmlFor="source">Source</Label>
                  <Input
                    id="source"
                    placeholder="e.g., conversation, document, email"
                    value={memoryRecord.source}
                    onChange={(e) => handleInputChange('source', e.target.value)}
                    className="w-full"
                  />
                  <p className="text-sm text-gray-500">
                    Origin or type of this information
                  </p>
                </div>

                <Separator />

                {/* Metadata */}
                <div className="space-y-4">
                  <h3 className="text-lg font-semibold flex items-center gap-2">
                    <Settings className="h-5 w-5" />
                    Metadata
                  </h3>

                  {/* Importance */}
                  <div className="space-y-2">
                    <Label htmlFor="importance">Importance (0.0 - 1.0)</Label>
                    <div className="flex items-center gap-4">
                      <Input
                        id="importance"
                        type="number"
                        min="0"
                        max="1"
                        step="0.1"
                        value={memoryRecord.metadata.importance}
                        onChange={(e) => handleMetadataChange('importance', parseFloat(e.target.value))}
                        className="w-32"
                      />
                      <Progress 
                        value={(memoryRecord.metadata.importance || 0) * 100} 
                        className="flex-1"
                      />
                    </div>
                    <p className="text-sm text-gray-500">
                      How important is this memory? (0 = low, 1 = high)
                    </p>
                  </div>

                  {/* Tags */}
                  <div className="space-y-2">
                    <Label htmlFor="tags">Tags</Label>
                    <Input
                      id="tags"
                      placeholder="e.g., work, personal, urgent (comma-separated)"
                      value={memoryRecord.metadata.tags?.join(', ') || ''}
                      onChange={(e) => handleTagsChange(e.target.value)}
                      className="w-full"
                    />
                    {memoryRecord.metadata.tags && memoryRecord.metadata.tags.length > 0 && (
                      <div className="flex flex-wrap gap-2 mt-2">
                        {memoryRecord.metadata.tags.map((tag, index) => (
                          <Badge key={index} variant="secondary">
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    )}
                    <p className="text-sm text-gray-500">
                      Add tags to categorize this memory
                    </p>
                  </div>

                  {/* Category */}
                  <div className="space-y-2">
                    <Label htmlFor="category">Category (Optional)</Label>
                    <Input
                      id="category"
                      placeholder="e.g., technical, personal, work"
                      value={memoryRecord.metadata.category || ''}
                      onChange={(e) => handleMetadataChange('category', e.target.value)}
                      className="w-full"
                    />
                    <p className="text-sm text-gray-500">
                      Manual category override (system will auto-categorize if empty)
                    </p>
                  </div>
                </div>

                <Separator />

                {/* Action Buttons */}
                <div className="flex flex-col sm:flex-row gap-3">
                  <Button
                    onClick={validateInput}
                    disabled={!isFormValid || isLoading}
                    className="flex-1 shadow-soft button-soft rounded-soft"
                  >
                    Validate Input
                  </Button>
                  <Button
                    onClick={performDryRun}
                    disabled={!isFormValid || isLoading}
                    variant="outline"
                    className="flex-1 shadow-soft button-soft rounded-soft"
                  >
                    Dry Run
                  </Button>
                  <Button
                    onClick={ingestMemory}
                    disabled={!isFormValid || isLoading}
                    variant="default"
                    className="flex-1 shadow-soft button-soft rounded-soft"
                  >
                    Ingest Memory
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Results */}
            {(validationResult || ingestionResult) && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {lastOperationType === 'validate' && validationResult ? (
                      validationResult.valid ? (
                        <CheckCircle className="h-5 w-5 text-green-600" />
                      ) : (
                        <AlertCircle className="h-5 w-5 text-red-600" />
                      )
                    ) : ingestionResult?.success ? (
                      <CheckCircle className="h-5 w-5 text-green-600" />
                    ) : (
                      <AlertCircle className="h-5 w-5 text-yellow-600" />
                    )}
                    {lastOperationType === 'validate' ? 'Validation Results' : 
                     lastOperationType === 'dry-run' ? 'Dry Run Results' : 
                     lastOperationType === 'ingest' ? 'Ingestion Results' : 'Results'}
                  </CardTitle>
                  <CardDescription>
                    {lastOperationType === 'validate' ? 'Input validation and error checking' :
                     lastOperationType === 'dry-run' ? 'Preview of memory processing without storing' :
                     lastOperationType === 'ingest' ? 'Memory successfully stored in the system' : ''}
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  {validationResult && lastOperationType === 'validate' && (
                    <div className="space-y-3">
                      <div className={`p-3 rounded-lg ${validationResult.valid ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                        <div className="flex items-center gap-2 mb-2">
                          {validationResult.valid ? (
                            <CheckCircle className="h-4 w-4 text-green-600" />
                          ) : (
                            <AlertCircle className="h-4 w-4 text-red-600" />
                          )}
                          <span className={`font-medium ${validationResult.valid ? 'text-green-700' : 'text-red-700'}`}>
                            {validationResult.valid ? 'Input is valid and ready for processing' : 'Input validation failed'}
                          </span>
                        </div>
                        
                        {validationResult.errors && validationResult.errors.length > 0 && (
                          <div className="mt-3">
                            <p className="font-medium text-red-700 mb-1">Errors that must be fixed:</p>
                            <ul className="list-disc list-inside text-red-700 text-sm space-y-1">
                              {validationResult.errors.map((error, index) => (
                                <li key={index}>{error}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                        
                        {validationResult.warnings && validationResult.warnings.length > 0 && (
                          <div className="mt-3">
                            <p className="font-medium text-yellow-700 mb-1">Warnings (optional to address):</p>
                            <ul className="list-disc list-inside text-yellow-700 text-sm space-y-1">
                              {validationResult.warnings.map((warning, index) => (
                                <li key={index}>{warning}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {ingestionResult && (lastOperationType === 'dry-run' || lastOperationType === 'ingest') && (
                    <div className="space-y-4">
                      <div className={`p-3 rounded-lg ${ingestionResult.success ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
                        {ingestionResult.message && (
                          <p className="text-sm font-medium mb-2">{ingestionResult.message}</p>
                        )}
                        {!ingestionResult.message && ingestionResult.success && (
                          <p className="text-sm font-medium text-green-700 mb-2">
                            {lastOperationType === 'ingest' ? '🎉 Memory successfully ingested and processed!' : 
                             '✅ Dry run completed - processing preview successful!'}
                          </p>
                        )}
                        
                        {lastOperationType === 'dry-run' && (
                          <div className="bg-gray-50 p-4 rounded-soft shadow-soft border-soft mt-4">
                            <p className="text-xs text-blue-700">
                              💡 This was a preview only. Click "Ingest Memory" to actually store this information.
                            </p>
                          </div>
                        )}
                      </div>
                      
                      {ingestionResult.memory_id && (
                        <div className="bg-gray-50 p-4 rounded-soft shadow-soft border-soft mb-4">
                          <h5 className="font-medium text-gray-900">Processing Details</h5>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 bg-gray-50 rounded-soft shadow-soft border-soft">
                            <div>
                              <span className="font-medium">Memory ID:</span>
                              <span className="ml-2 font-mono text-xs bg-gray-200 px-2 py-1 rounded">
                                {ingestionResult.memory_id}
                              </span>
                            </div>
                            {ingestionResult.processing_time_ms && (
                              <div>
                                <span className="font-medium">Processing Time:</span>
                                <span className="ml-2">{ingestionResult.processing_time_ms}ms</span>
                              </div>
                            )}
                          </div>
                          
                          {ingestionResult.category && (
                            <div className="space-y-2">
                              <h6 className="font-medium text-gray-900">Categorization</h6>
                              <div className="bg-white p-3 rounded border">
                                <div className="flex items-center justify-between mb-2">
                                  <span className="font-medium">{ingestionResult.category.name}</span>
                                  <Badge variant="outline" className="badge-soft">
                                    {(ingestionResult.category.confidence * 100).toFixed(1)}% confidence
                                  </Badge>
                                </div>
                                {ingestionResult.category.tags && ingestionResult.category.tags.length > 0 && (
                                  <div className="flex flex-wrap gap-1">
                                    {ingestionResult.category.tags.map((tag, index) => (
                                      <Badge key={index} variant="secondary" className="text-xs">
                                        {tag}
                                      </Badge>
                                    ))}
                                  </div>
                                )}
                              </div>
                            </div>
                          )}
                        </div>
                      )}

                      {/* Belief Update Results */}
                      {ingestionResult.belief_update_result && lastOperationType === 'ingest' && (
                        <div className="bg-gradient-to-r from-blue-50 to-purple-50 p-4 rounded-lg border border-blue-200">
                          <div className="flex items-center gap-2 mb-3">
                            <Brain className="h-5 w-5 text-blue-600" />
                            <h5 className="font-medium text-blue-900">Belief System Updates</h5>
                          </div>
                          <p className="text-sm text-blue-800 mb-4">{ingestionResult.belief_update_result.summary}</p>
                          
                          {ingestionResult.belief_update_result.newBeliefs && ingestionResult.belief_update_result.newBeliefs.length > 0 && (
                            <div className="space-y-3">
                              <div className="flex items-center gap-2">
                                <CheckCircle className="h-4 w-4 text-green-600" />
                                <p className="text-sm font-medium text-green-800">
                                  {ingestionResult.belief_update_result.newBeliefs.length} New Beliefs Formed
                                </p>
                              </div>
                              <div className="space-y-2 max-h-64 overflow-y-auto">
                                {ingestionResult.belief_update_result.newBeliefs.map((belief, index) => (
                                  <div key={index} className="bg-white p-3 rounded-lg border-l-4 border-blue-400 shadow-sm">
                                    <p className="text-sm font-medium text-gray-900 mb-2">"{belief.statement}"</p>
                                    <div className="flex flex-wrap items-center gap-3 text-xs text-gray-600">
                                      <span className="flex items-center gap-1">
                                        <span className="w-2 h-2 bg-blue-400 rounded-full"></span>
                                        {(belief.confidence * 100).toFixed(1)}% confidence
                                      </span>
                                      <span className="flex items-center gap-1">
                                        <span className="w-2 h-2 bg-purple-400 rounded-full"></span>
                                        {belief.category}
                                      </span>
                                      {belief.highConfidence && (
                                        <Badge variant="outline" className="text-xs py-0">
                                          High Confidence
                                        </Badge>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {ingestionResult.belief_update_result.conflicts && ingestionResult.belief_update_result.conflicts.length > 0 && (
                            <div className="mt-4 space-y-2">
                              <div className="flex items-center gap-2">
                                <AlertCircle className="h-4 w-4 text-red-600" />
                                <p className="text-sm font-medium text-red-800">Conflicts Detected</p>
                              </div>
                              <div className="space-y-1">
                                {ingestionResult.belief_update_result.conflicts.map((conflict, index) => (
                                  <div key={index} className="bg-red-50 border border-red-200 p-2 rounded text-sm text-red-800">
                                    {conflict.description || 'Conflict detected - beliefs may contradict existing knowledge'}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
          </TabsContent>

          <TabsContent value="validate">
            <Card className="card-soft shadow-soft border-soft">
              <CardHeader>
                <CardTitle>Input Validation & Preview</CardTitle>
                <CardDescription>
                  Validate your input and preview how it will be processed
                </CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-gray-500 p-3 bg-gray-50 shadow-soft border-soft rounded-soft">
                  Use the "Validate Input" and "Dry Run" buttons in the Ingest tab to see validation results and processing preview.
                </p>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}