#pragma once

#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <map>
#include "basics.h"

// Forward declarations
class ScribbleConfig;

// AI Service Provider Types
enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GOOGLE_GEMINI,
    OLLAMA,
    CUSTOM
};

// Content filtering configuration
struct ContentFilter {
    std::vector<std::string> allowedTopics;
    std::vector<std::string> blockedTopics;
    std::vector<std::string> allowedUseCases;
    std::string filterLevel; // "strict", "moderate", "permissive"
    bool enableRagFiltering = true;
};

// AI Request/Response structures
struct AIRequest {
    std::string prompt;
    std::string context;
    std::vector<std::string> documents; // For RAG
    std::map<std::string, std::string> metadata;
    ContentFilter filter;
    AIProvider provider = AIProvider::OPENAI;
    int maxTokens = 1000;
    float temperature = 0.7f;
};

struct AIResponse {
    std::string content;
    std::string filteredReason;
    bool success = false;
    std::string error;
    std::map<std::string, std::string> metadata;
    float confidence = 0.0f;
};

// RAG Document processing
struct RAGDocument {
    std::string id;
    std::string content;
    std::string title;
    std::string source;
    std::vector<std::string> tags;
    float relevanceScore = 0.0f;
};

// AI Service Provider Interface
class AIServiceProvider {
public:
    virtual ~AIServiceProvider() = default;
    virtual AIResponse generateResponse(const AIRequest& request) = 0;
    virtual bool isConfigured() const = 0;
    virtual std::string getProviderName() const = 0;
    virtual bool testConnection() = 0;
};

// RAG Service Interface
class RAGService {
public:
    virtual ~RAGService() = default;
    virtual std::vector<RAGDocument> searchDocuments(const std::string& query, int maxResults = 5) = 0;
    virtual bool indexDocument(const RAGDocument& document) = 0;
    virtual bool removeDocument(const std::string& documentId) = 0;
    virtual void clearIndex() = 0;
};

// Content Filter Engine
class ContentFilterEngine {
public:
    ContentFilterEngine(const ContentFilter& config);
    bool isContentAllowed(const std::string& content, const AIRequest& request) const;
    std::string getFilterReason() const;
    void updateFilterConfig(const ContentFilter& config);

private:
    ContentFilter config_;
    mutable std::string lastFilterReason_;
    
    bool checkTopics(const std::string& content) const;
    bool checkUseCases(const AIRequest& request) const;
    bool checkSafetyFilter(const std::string& content) const;
};

// Main AI Agent class
class AIAgent {
public:
    AIAgent(ScribbleConfig* config);
    ~AIAgent();

    // Configuration
    void configure(AIProvider provider, const std::string& apiKey, const std::string& baseUrl = "");
    void setContentFilter(const ContentFilter& filter);
    void setRAGService(std::unique_ptr<RAGService> ragService);

    // Core AI operations
    AIResponse processRequest(const AIRequest& request);
    AIResponse generateText(const std::string& prompt, const std::string& context = "");
    AIResponse summarizeContent(const std::string& content);
    AIResponse extractKeyPoints(const std::string& content);
    AIResponse answerQuestion(const std::string& question, const std::string& context = "");

    // RAG operations
    bool indexCurrentDocument();
    bool indexDocument(const std::string& content, const std::string& title, const std::string& id = "");
    std::vector<RAGDocument> searchRelevantContent(const std::string& query);

    // Provider management
    void addCustomProvider(std::unique_ptr<AIServiceProvider> provider);
    std::vector<std::string> getAvailableProviders() const;
    bool switchProvider(AIProvider provider);
    AIProvider getCurrentProvider() const { return currentProvider_; }

    // Status and diagnostics
    bool isConfigured() const;
    bool testConnection();
    std::string getLastError() const { return lastError_; }

private:
    ScribbleConfig* config_;
    AIProvider currentProvider_;
    std::map<AIProvider, std::unique_ptr<AIServiceProvider>> providers_;
    std::unique_ptr<RAGService> ragService_;
    std::unique_ptr<ContentFilterEngine> filterEngine_;
    ContentFilter currentFilter_;
    std::string lastError_;

    // Helper methods
    std::unique_ptr<AIServiceProvider> createProvider(AIProvider provider);
    void initializeProviders();
    void loadConfiguration();
    void saveConfiguration();
    std::string enhancePromptWithRAG(const AIRequest& request);
};

// Factory functions for providers
std::unique_ptr<AIServiceProvider> createOpenAIProvider(const std::string& apiKey, const std::string& baseUrl = "");
std::unique_ptr<AIServiceProvider> createAnthropicProvider(const std::string& apiKey);
std::unique_ptr<AIServiceProvider> createGoogleProvider(const std::string& apiKey);
std::unique_ptr<AIServiceProvider> createOllamaProvider(const std::string& baseUrl = "http://localhost:11434");

// Factory functions for RAG services
std::unique_ptr<RAGService> createLocalRAGService(const std::string& indexPath);
std::unique_ptr<RAGService> createVectorRAGService(const std::string& connectionString);