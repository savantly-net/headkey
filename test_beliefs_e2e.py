#!/usr/bin/env python3
"""
End-to-End Test Script for HeadKey Belief Analysis
This script tests the complete memory ingestion pipeline with a focus on belief creation
Uses the Dune Chapter 1 content as test data
"""

import json
import sys
import time
import urllib.request
import urllib.parse
import urllib.error
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional, Any
import argparse


class Colors:
    """ANSI color codes for terminal output"""
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    END = '\033[0m'


class HeadKeyTester:
    def __init__(self, base_url: str = "http://localhost:8080", agent_id: str = "test-agent-dune-ch1"):
        self.base_url = base_url
        self.memory_api_url = f"{base_url}/api/v1/memory"
        self.agent_id = agent_id
        self.input_file = Path("data/input/dune-ch1.md")
        self.headers = {'Content-Type': 'application/json'}
        
    def print_header(self, text: str, color: str = Colors.BLUE):
        """Print a colored header"""
        print(f"\n{color}{Colors.BOLD}{text}{Colors.END}")
        print(f"{color}{'=' * len(text)}{Colors.END}")
        
    def print_section(self, text: str, color: str = Colors.CYAN):
        """Print a colored section header"""
        print(f"\n{color}{Colors.BOLD}{text}{Colors.END}")
        
    def print_success(self, text: str):
        """Print success message"""
        print(f"{Colors.GREEN}✅ {text}{Colors.END}")
        
    def print_warning(self, text: str):
        """Print warning message"""
        print(f"{Colors.YELLOW}⚠️  {text}{Colors.END}")
        
    def print_error(self, text: str):
        """Print error message"""
        print(f"{Colors.RED}❌ {text}{Colors.END}")
        
    def print_info(self, text: str):
        """Print info message"""
        print(f"{Colors.BLUE}💡 {text}{Colors.END}")
        
    def make_request(self, method: str, endpoint: str, data: Optional[Dict] = None) -> Dict:
        """Make HTTP request with proper error handling"""
        url = f"{self.memory_api_url}{endpoint}"
        self.print_info(f"{method} {endpoint}")
        
        try:
            if method.upper() == "GET":
                req = urllib.request.Request(url, headers=self.headers)
                with urllib.request.urlopen(req) as response:
                    response_data = response.read().decode('utf-8')
                    status_code = response.status
            elif method.upper() == "POST":
                json_data = json.dumps(data).encode('utf-8') if data else b''
                req = urllib.request.Request(url, data=json_data, headers=self.headers, method='POST')
                with urllib.request.urlopen(req) as response:
                    response_data = response.read().decode('utf-8')
                    status_code = response.status
            else:
                raise ValueError(f"Unsupported HTTP method: {method}")
                
            # Parse and pretty print response
            try:
                response_json = json.loads(response_data)
                print(json.dumps(response_json, indent=2))
                return {"json": response_json, "status_code": status_code, "text": response_data}
            except json.JSONDecodeError:
                print(response_data)
                return {"json": None, "status_code": status_code, "text": response_data}
                
        except urllib.error.HTTPError as e:
            # Handle HTTP errors (4xx, 5xx)
            try:
                error_data = e.read().decode('utf-8')
                try:
                    error_json = json.loads(error_data)
                    print(json.dumps(error_json, indent=2))
                except json.JSONDecodeError:
                    print(error_data)
                return {"json": error_json if 'error_json' in locals() else None, 
                       "status_code": e.code, "text": error_data}
            except Exception:
                self.print_error(f"HTTP Error {e.code}: {e.reason}")
                return {"json": None, "status_code": e.code, "text": str(e.reason)}
        except urllib.error.URLError as e:
            self.print_error(f"Failed to connect to {url}: {e}")
            raise
        except Exception as e:
            self.print_error(f"Request failed: {e}")
            raise
            
    def display_beliefs(self, belief_list: List[Dict], belief_type: str):
        """Display belief details in a formatted way"""
        if not belief_list:
            print(f"   📋 {belief_type}: 0 beliefs")
            return
            
        count = len(belief_list)
        print(f"   📋 {belief_type}: {count} beliefs")
        
        for belief in belief_list:
            belief_id = belief.get('id', 'N/A')
            statement = belief.get('statement', 'N/A')
            confidence = belief.get('confidence', 'N/A')
            print(f"      • ID: {belief_id} | Statement: {statement} | Confidence: {confidence}")
            
    def display_knowledge_graph(self, knowledge_graph: Optional[Dict]):
        """Display beliefs and relationships from a knowledge graph structure"""
        if not knowledge_graph:
            print("   📋 Knowledge Graph: No data available")
            return
            
        beliefs = knowledge_graph.get('beliefs', {})
        relationships = knowledge_graph.get('relationships', {})
        
        if not beliefs:
            print("   📋 Knowledge Graph: 0 beliefs")
            return
            
        count = len(beliefs)
        print(f"   📋 Knowledge Graph: {count} beliefs")
        
        for belief_id, belief in beliefs.items():
            statement = belief.get('statement', 'N/A')
            confidence = belief.get('confidence', 'N/A')
            active = belief.get('active', True)
            status = "Active" if active else "Inactive"
            print(f"      • ID: {belief_id} | Statement: {statement} | Confidence: {confidence} | Status: {status}")
            
        if relationships:
            rel_count = len(relationships)
            print(f"   🔗 Relationships: {rel_count}")
            
    def display_conflicts(self, conflicts: List[Dict]):
        """Display conflict details"""
        if not conflicts:
            print("   ⚡ Conflicts Detected: 0")
            return
            
        count = len(conflicts)
        print(f"   ⚡ Conflicts Detected: {count}")
        
        for conflict in conflicts:
            conflict_type = conflict.get('type', 'N/A')
            severity = conflict.get('severity', 'N/A')
            resolved = conflict.get('resolved', False)
            print(f"      • Type: {conflict_type} | Severity: {severity} | Resolved: {resolved}")
            
    def check_server_health(self) -> bool:
        """Check if the HeadKey server is running"""
        self.print_section("🔍 Checking Server Health")
        
        try:
            response = self.make_request("GET", "/health")
            if response["status_code"] == 200:
                self.print_success("Server is running!")
                return True
            else:
                self.print_error(f"Server health check failed with status: {response['status_code']}")
                return False
        except Exception:
            self.print_error(f"Server is not running at {self.memory_api_url}")
            self.print_info("To start the server:")
            self.print_info("   1. Start PostgreSQL: docker compose up -d")
            self.print_info("   2. Start HeadKey: ./gradlew rest:quarkusDev")
            self.print_info("   3. Wait for 'started in X.XXXs' message")
            self.print_info("   4. Run this test: python test_beliefs_e2e.py")
            return False
            
    def load_test_content(self) -> str:
        """Load content from the test file"""
        self.print_section("📖 Loading Test Content")
        
        if not self.input_file.exists():
            self.print_error(f"Input file not found: {self.input_file}")
            raise FileNotFoundError(f"Test content file not found: {self.input_file}")
            
        content = self.input_file.read_text(encoding='utf-8')
        original_length = len(content)
        
        # Truncate content if it exceeds the maximum allowed length (10,000 characters)
        max_length = 9800  # Leave some buffer for safety
        if original_length > max_length:
            content = content[:max_length]
            content_length = len(content)
            self.print_warning(f"Content truncated from {original_length:,} to {content_length:,} characters due to validation limits")
        else:
            content_length = original_length
        
        self.print_success(f"Content loaded from {self.input_file}")
        print(f"   📏 Content length: {content_length:,} characters")
        
        return content
        
    def validate_input(self, content: str) -> bool:
        """Validate the input content"""
        self.print_section("✅ Input Validation")
        
        validate_data = {
            "agent_id": self.agent_id,
            "content": content,
            "source": "file",
            "metadata": {
                "file_name": "dune-ch1.md",
                "content_type": "literary_text",
                "chapter": "1",
                "book": "Dune",
                "author": "Frank Herbert"
            }
        }
        
        try:
            response = self.make_request("POST", "/validate", validate_data)
            if response["status_code"] == 200:
                self.print_success("Input validation passed")
                return True
            elif response["status_code"] == 400:
                self.print_error("Input validation failed - check content length and format")
                return False
            else:
                self.print_error(f"Input validation failed with status {response['status_code']}")
                return False
        except Exception as e:
            self.print_error(f"Validation request failed: {e}")
            return False
            
    def dry_run_analysis(self, content: str) -> Optional[Dict]:
        """Perform dry run analysis"""
        self.print_section("🧪 Dry Run - Preview Belief Analysis")
        print("This will show what beliefs would be created without actually storing them.")
        
        dry_run_data = {
            "agent_id": self.agent_id,
            "content": content,
            "source": "file",
            "metadata": {
                "file_name": "dune-ch1.md",
                "content_type": "literary_text",
                "chapter": "1",
                "book": "Dune",
                "author": "Frank Herbert",
                "test_run": True
            },
            "dry_run": True
        }
        
        try:
            response = self.make_request("POST", "/ingest", dry_run_data)
            if response["status_code"] == 200:
                self.print_success("Dry run completed successfully")
                return response["json"]
            else:
                self.print_error("Dry run failed")
                return None
        except Exception as e:
            self.print_error(f"Dry run request failed: {e}")
            return None
            
    def analyze_belief_results(self, response_data: Dict, result_type: str = "Dry Run"):
        """Analyze and display belief analysis results"""
        print(f"\n🔍 {result_type} Belief Analysis Results:")
        
        belief_update_result = response_data.get('belief_update_result', {})
        
        if not belief_update_result:
            self.print_warning("No belief analysis results found")
            return
            
        # Display beliefs
        new_beliefs = belief_update_result.get('new_beliefs', [])
        reinforced_beliefs = belief_update_result.get('reinforced_beliefs', [])
        weakened_beliefs = belief_update_result.get('weakened_beliefs', [])
        conflicts = belief_update_result.get('conflicts', [])
        
        self.display_beliefs(new_beliefs, "New Beliefs")
        self.display_beliefs(reinforced_beliefs, "Reinforced Beliefs")
        self.display_beliefs(weakened_beliefs, "Weakened Beliefs")
        self.display_conflicts(conflicts)
        
        # Display statistics
        processing_time = belief_update_result.get('processing_time_ms', 'N/A')
        total_examined = belief_update_result.get('total_beliefs_examined', 'N/A')
        memories_analyzed = belief_update_result.get('memories_analyzed', 'N/A')
        overall_confidence = belief_update_result.get('overall_confidence', 'N/A')
        
        print(f"\n   📊 Analysis Statistics:")
        print(f"      • Processing Time: {processing_time}ms")
        print(f"      • Total Beliefs Examined: {total_examined}")
        print(f"      • Memories Analyzed: {memories_analyzed}")
        print(f"      • Overall Confidence: {overall_confidence}")
        
        # Display summary
        summary = belief_update_result.get('summary', 'No summary available')
        print(f"\n   📝 Summary: {summary}")
        
    def actual_ingestion(self, content: str) -> Optional[Dict]:
        """Perform actual memory ingestion"""
        self.print_section("💾 Actual Memory Ingestion with Belief Analysis")
        print("This will ingest the content and create/update beliefs in the system.")
        
        ingest_data = {
            "agent_id": self.agent_id,
            "content": content,
            "source": "file",
            "metadata": {
                "file_name": "dune-ch1.md",
                "content_type": "literary_text",
                "chapter": "1",
                "book": "Dune",
                "author": "Frank Herbert",
                "importance": "high",
                "tags": ["dune", "science_fiction", "literature", "chapter_1"],
                "test_run": True
            }
        }
        
        try:
            response = self.make_request("POST", "/ingest", ingest_data)
            if response["status_code"] == 201:
                self.print_success("Memory ingestion completed successfully")
                return response["json"]
            else:
                self.print_error("Memory ingestion failed")
                return None
        except Exception as e:
            self.print_error(f"Ingestion request failed: {e}")
            return None
            
    def get_system_statistics(self) -> Optional[Dict]:
        """Get system statistics"""
        self.print_section("📈 System Statistics")
        
        try:
            response = self.make_request("GET", "/statistics")
            if response["status_code"] == 200:
                return response["json"]
            else:
                self.print_error("Failed to retrieve statistics")
                return None
        except Exception as e:
            self.print_error(f"Statistics request failed: {e}")
            return None
            
    def query_agent_beliefs(self, include_inactive: bool = False) -> Optional[Dict]:
        """Query beliefs for the agent using the knowledge graph snapshot"""
        self.print_section("🔍 Querying Agent's Beliefs")
        print(f"Checking beliefs for agent: {self.agent_id}")
        
        # Use the snapshot-graph endpoint to get beliefs and relationships
        belief_url = f"{self.base_url}/api/v1/agents/{self.agent_id}/belief-relationships/snapshot-graph"
        if include_inactive:
            belief_url += "?includeInactive=true"
            print("Including inactive beliefs and relationships")
        
        try:
            req = urllib.request.Request(belief_url, headers=self.headers)
            with urllib.request.urlopen(req) as response:
                if response.status == 200:
                    response_data = response.read().decode('utf-8')
                    response_json = json.loads(response_data)
                    self.print_success("Retrieved agent's knowledge graph snapshot")
                    print(json.dumps(response_json, indent=2))
                    return response_json
                else:
                    self.print_warning("Knowledge graph snapshot endpoint not available or accessible")
                    return None
        except Exception as e:
            self.print_warning(f"Knowledge graph snapshot endpoint not available or accessible: {str(e)}")
            return None
            
    def print_test_summary(self, content_length: int, dry_run_result: Optional[Dict], 
                          ingest_result: Optional[Dict], knowledge_graph: Optional[Dict] = None):
        """Print comprehensive test summary"""
        self.print_header("🎉 End-to-End Belief Analysis Test Complete!", Colors.GREEN)
        
        print(f"\n📋 Test Summary:")
        print(f"   • Agent ID: {self.agent_id}")
        print(f"   • Content Source: {self.input_file}")
        print(f"   • Content Length: {content_length:,} characters")
        print(f"   • Test Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        
        # Extract belief counts from ingestion result
        if ingest_result:
            belief_result = ingest_result.get('belief_update_result', {})
            new_count = len(belief_result.get('new_beliefs', []))
            reinforced_count = len(belief_result.get('reinforced_beliefs', []))
            weakened_count = len(belief_result.get('weakened_beliefs', []))
            conflict_count = len(belief_result.get('conflicts', []))
            
            print(f"\n🧠 Belief Analysis Results:")
            print(f"   • New Beliefs Created: {new_count}")
            print(f"   • Beliefs Reinforced: {reinforced_count}")
            print(f"   • Beliefs Weakened: {weakened_count}")
            print(f"   • Conflicts Detected: {conflict_count}")
            
            # Show memory ID if available
            memory_id = ingest_result.get('memory_id', 'N/A')
            print(f"   • Memory ID: {memory_id}")
            
        # Display knowledge graph stats if available
        if knowledge_graph:
            beliefs = knowledge_graph.get('beliefs', {})
            relationships = knowledge_graph.get('relationships', {})
            print(f"\n🕸️ Knowledge Graph Status:")
            print(f"   • Total Beliefs in Graph: {len(beliefs)}")
            print(f"   • Total Relationships: {len(relationships)}")
            
        print(f"\n💡 Next Steps:")
        print(f"   • Review the beliefs created from the Dune chapter content")
        print(f"   • Check the system statistics for performance metrics")
        print(f"   • Explore the belief relationships using the relationship endpoints")
        print(f"   • Test with different content: cp your_file.md data/input/")
        
        print(f"\n🔧 API Endpoints Available:")
        print(f"   • Memory Ingestion: {self.memory_api_url}/ingest")
        print(f"   • Belief Relationships: {self.base_url}/api/v1/agents/{{agentId}}/belief-relationships")
        print(f"   • System Statistics: {self.memory_api_url}/statistics")
        print(f"   • API Documentation: {self.base_url}/swagger-ui")
        
        print(f"\n📚 To run this test again:")
        print(f"   python test_beliefs_e2e.py")
        
        print(f"\n🔄 To test with different content:")
        print(f"   python test_beliefs_e2e.py --input-file path/to/your/file.md")
        
    def run_full_test(self):
        """Run the complete end-to-end test"""
        self.print_header("🧠 HeadKey Belief Analysis End-to-End Test")
        print(f"Agent ID: {self.agent_id}")
        print(f"Input File: {self.input_file}")
        print(f"Base URL: {self.base_url}")
        
        try:
            # Step 1: Check server health
            if not self.check_server_health():
                return False
                
            # Step 2: Load test content
            content = self.load_test_content()
            
            # Step 3: Validate input
            if not self.validate_input(content):
                self.print_error("Input validation failed, stopping test")
                return False
                
            # Step 4: Dry run analysis
            dry_run_result = self.dry_run_analysis(content)
            if dry_run_result:
                self.analyze_belief_results(dry_run_result, "Dry Run")
                
            # Step 5: Actual ingestion
            ingest_result = self.actual_ingestion(content)
            if ingest_result:
                self.analyze_belief_results(ingest_result, "Actual Ingestion")
                
            # Step 6: Query agent beliefs
            knowledge_graph = self.query_agent_beliefs()
            if knowledge_graph:
                self.display_knowledge_graph(knowledge_graph)
            
            # Step 7: Get final statistics
            self.get_system_statistics()
            
            # Step 8: Print summary
            self.print_test_summary(len(content), dry_run_result, ingest_result, knowledge_graph)
            
            return True
            
        except KeyboardInterrupt:
            self.print_warning("Test interrupted by user")
            return False
        except Exception as e:
            self.print_error(f"Test failed with error: {e}")
            return False


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='HeadKey Belief Analysis End-to-End Test')
    parser.add_argument('--base-url', default='http://localhost:8080',
                       help='Base URL for HeadKey API (default: http://localhost:8080)')
    parser.add_argument('--agent-id', default='test-agent-dune-ch1',
                       help='Agent ID for testing (default: test-agent-dune-ch1)')
    parser.add_argument('--input-file', default='data/input/dune-ch1.md',
                       help='Input file path (default: data/input/dune-ch1.md)')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    # Create tester instance
    tester = HeadKeyTester(base_url=args.base_url, agent_id=args.agent_id)
    tester.input_file = Path(args.input_file)
    
    # Run the test
    success = tester.run_full_test()
    
    # Exit with appropriate code
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()