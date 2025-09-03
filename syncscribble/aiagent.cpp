#include "aiagent.h"
#include "scribbleconfig.h"
#include "ulib/stringutil.h"
#include "pugixml.hpp"
#include <algorithm>
#include <sstream>
#include <regex>

// ContentFilterEngine Implementation
ContentFilterEngine::ContentFilterEngine(const ContentFilter& config) : config_(config) {}

bool ContentFilterEngine::isContentAllowed(const std::string& content, const AIRequest& request) const {
    lastFilterReason_.clear();
    
    // Check safety filter first
    if (!checkSafetyFilter(content)) {
        lastFilterReason_ = "Content blocked by safety filter";
        return false;
    }
    
    // Check topics if configured
    if (!config_.allowedTopics.empty() || !config_.blockedTopics.empty()) {
        if (!checkTopics(content)) {
            lastFilterReason_ = "Content blocked by topic filter";
            return false;
        }
    }
    
    // Check use cases
    if (!checkUseCases(request)) {
        lastFilterReason_ = "Request blocked by use case filter";
        return false;
    }
    
    return true;
}

bool ContentFilterEngine::checkTopics(const std::string& content) const {
    std::string lowerContent = content;
    std::transform(lowerContent.begin(), lowerContent.end(), lowerContent.begin(), ::tolower);
    
    // Check blocked topics first
    for (const auto& blocked : config_.blockedTopics) {
        std::string lowerBlocked = blocked;
        std::transform(lowerBlocked.begin(), lowerBlocked.end(), lowerBlocked.begin(), ::tolower);
        if (lowerContent.find(lowerBlocked) != std::string::npos) {
            return false;
        }
    }
    
    // If allowed topics are specified, content must contain at least one
    if (!config_.allowedTopics.empty()) {
        for (const auto& allowed : config_.allowedTopics) {
            std::string lowerAllowed = allowed;
            std::transform(lowerAllowed.begin(), lowerAllowed.end(), lowerAllowed.begin(), ::tolower);
            if (lowerContent.find(lowerAllowed) != std::string::npos) {
                return true;
            }
        }
        return false; // No allowed topics found
    }
    
    return true;
}

bool ContentFilterEngine::checkUseCases(const AIRequest& request) const {
    if (config_.allowedUseCases.empty()) {
        return true; // No restrictions
    }
    
    // Check if request metadata contains allowed use case
    auto it = request.metadata.find("useCase");
    if (it != request.metadata.end()) {
        const std::string& useCase = it->second;
        return std::find(config_.allowedUseCases.begin(), config_.allowedUseCases.end(), useCase) 
               != config_.allowedUseCases.end();
    }
    
    return false; // No use case specified but restrictions exist
}

bool ContentFilterEngine::checkSafetyFilter(const std::string& content) const {
    // Basic safety filter - can be enhanced with more sophisticated detection
    std::vector<std::string> unsafePatterns;
    
    if (config_.filterLevel == "strict") {
        unsafePatterns = {
            "violence", "hate", "harassment", "illegal", "harmful",
            "dangerous", "explicit", "nsfw", "toxic"
        };
    } else if (config_.filterLevel == "moderate") {
        unsafePatterns = {
            "violence", "hate", "harassment", "illegal", "dangerous"
        };
    }
    // "permissive" level has minimal filtering
    
    std::string lowerContent = content;
    std::transform(lowerContent.begin(), lowerContent.end(), lowerContent.begin(), ::tolower);
    
    for (const auto& pattern : unsafePatterns) {
        if (lowerContent.find(pattern) != std::string::npos) {
            return false;
        }
    }
    
    return true;
}

void ContentFilterEngine::updateFilterConfig(const ContentFilter& config) {
    config_ = config;
}

std::string ContentFilterEngine::getFilterReason() const {
    return lastFilterReason_;
}

// AIAgent Implementation
AIAgent::AIAgent(ScribbleConfig* config) 
    : config_(config), currentProvider_(AIProvider::OPENAI) {
    
    // Set default content filter
    currentFilter_.filterLevel = "moderate";
    currentFilter_.enableRagFiltering = true;
    
    filterEngine_ = std::make_unique<ContentFilterEngine>(currentFilter_);
    
    loadConfiguration();
    initializeProviders();
}

AIAgent::~AIAgent() = default;

void AIAgent::configure(AIProvider provider, const std::string& apiKey, const std::string& baseUrl) {
    currentProvider_ = provider;
    
    // Store configuration
    std::string providerName;
    switch (provider) {
        case AIProvider::OPENAI: providerName = "openai"; break;
        case AIProvider::ANTHROPIC: providerName = "anthropic"; break;
        case AIProvider::GOOGLE_GEMINI: providerName = "google"; break;
        case AIProvider::OLLAMA: providerName = "ollama"; break;
        case AIProvider::CUSTOM: providerName = "custom"; break;
    }
    
    config_->setString(fstring("ai_%s_apikey", providerName.c_str()), apiKey);
    if (!baseUrl.empty()) {
        config_->setString(fstring("ai_%s_baseurl", providerName.c_str()), baseUrl);
    }
    
    // Recreate provider with new configuration
    providers_[provider] = createProvider(provider);
    
    saveConfiguration();
}

void AIAgent::setContentFilter(const ContentFilter& filter) {
    currentFilter_ = filter;
    filterEngine_->updateFilterConfig(filter);
    saveConfiguration();
}

void AIAgent::setRAGService(std::unique_ptr<RAGService> ragService) {
    ragService_ = std::move(ragService);
}

AIResponse AIAgent::processRequest(const AIRequest& request) {
    AIResponse response;
    lastError_.clear();
    
    // Check if provider is configured
    auto it = providers_.find(currentProvider_);
    if (it == providers_.end() || !it->second || !it->second->isConfigured()) {
        response.success = false;
        response.error = "AI provider not configured";
        lastError_ = response.error;
        return response;
    }
    
    // Apply content filtering
    if (!filterEngine_->isContentAllowed(request.prompt, request)) {
        response.success = false;
        response.error = "Content blocked by filter";
        response.filteredReason = filterEngine_->getFilterReason();
        lastError_ = response.error;
        return response;
    }
    
    // Enhance request with RAG if enabled
    AIRequest enhancedRequest = request;
    if (ragService_ && !request.documents.empty()) {
        enhancedRequest.context = enhancePromptWithRAG(request);
    }
    
    // Process with AI provider
    try {
        response = it->second->generateResponse(enhancedRequest);
        
        // Post-process response filtering
        if (response.success && !filterEngine_->isContentAllowed(response.content, request)) {
            response.success = false;
            response.error = "Response blocked by filter";
            response.filteredReason = filterEngine_->getFilterReason();
            response.content.clear();
        }
    } catch (const std::exception& e) {
        response.success = false;
        response.error = fstring("AI processing error: %s", e.what());
        lastError_ = response.error;
    }
    
    return response;
}

AIResponse AIAgent::generateText(const std::string& prompt, const std::string& context) {
    AIRequest request;
    request.prompt = prompt;
    request.context = context;
    request.provider = currentProvider_;
    request.filter = currentFilter_;
    request.metadata["useCase"] = "text_generation";
    
    return processRequest(request);
}

AIResponse AIAgent::summarizeContent(const std::string& content) {
    AIRequest request;
    request.prompt = "Please provide a concise summary of the following content:\n\n" + content;
    request.provider = currentProvider_;
    request.filter = currentFilter_;
    request.metadata["useCase"] = "summarization";
    request.maxTokens = 500;
    
    return processRequest(request);
}

AIResponse AIAgent::extractKeyPoints(const std::string& content) {
    AIRequest request;
    request.prompt = "Extract the key points from the following content as a bulleted list:\n\n" + content;
    request.provider = currentProvider_;
    request.filter = currentFilter_;
    request.metadata["useCase"] = "key_extraction";
    request.maxTokens = 300;
    
    return processRequest(request);
}

AIResponse AIAgent::answerQuestion(const std::string& question, const std::string& context) {
    AIRequest request;
    if (context.empty()) {
        request.prompt = question;
    } else {
        request.prompt = fstring("Based on the following context, answer the question:\n\nContext: %s\n\nQuestion: %s", 
                                context.c_str(), question.c_str());
    }
    request.provider = currentProvider_;
    request.filter = currentFilter_;
    request.metadata["useCase"] = "question_answering";
    
    return processRequest(request);
}

bool AIAgent::indexCurrentDocument() {
    // This would need integration with the current document system
    // For now, return false to indicate not implemented
    return false;
}

bool AIAgent::indexDocument(const std::string& content, const std::string& title, const std::string& id) {
    if (!ragService_) {
        lastError_ = "RAG service not configured";
        return false;
    }
    
    RAGDocument doc;
    doc.id = id.empty() ? fstring("doc_%llu", mSecSinceEpoch()) : id;
    doc.content = content;
    doc.title = title;
    doc.source = "user_document";
    
    return ragService_->indexDocument(doc);
}

std::vector<RAGDocument> AIAgent::searchRelevantContent(const std::string& query) {
    if (!ragService_) {
        return {};
    }
    
    return ragService_->searchDocuments(query);
}

bool AIAgent::isConfigured() const {
    auto it = providers_.find(currentProvider_);
    return it != providers_.end() && it->second && it->second->isConfigured();
}

bool AIAgent::testConnection() {
    auto it = providers_.find(currentProvider_);
    if (it == providers_.end() || !it->second) {
        lastError_ = "Provider not available";
        return false;
    }
    
    return it->second->testConnection();
}

void AIAgent::initializeProviders() {
    // Initialize all configured providers
    for (int i = 0; i < 5; ++i) {
        AIProvider provider = static_cast<AIProvider>(i);
        auto created = createProvider(provider);
        if (created && created->isConfigured()) {
            providers_[provider] = std::move(created);
        }
    }
}

std::unique_ptr<AIServiceProvider> AIAgent::createProvider(AIProvider provider) {
    std::string providerName;
    switch (provider) {
        case AIProvider::OPENAI: 
            providerName = "openai";
            break;
        case AIProvider::ANTHROPIC: 
            providerName = "anthropic";
            break;
        case AIProvider::GOOGLE_GEMINI: 
            providerName = "google";
            break;
        case AIProvider::OLLAMA: 
            providerName = "ollama";
            break;
        default:
            return nullptr;
    }
    
    std::string apiKey = config_->String(fstring("ai_%s_apikey", providerName.c_str()));
    std::string baseUrl = config_->String(fstring("ai_%s_baseurl", providerName.c_str()));
    
    switch (provider) {
        case AIProvider::OPENAI:
            return createOpenAIProvider(apiKey, baseUrl);
        case AIProvider::ANTHROPIC:
            return createAnthropicProvider(apiKey);
        case AIProvider::GOOGLE_GEMINI:
            return createGoogleProvider(apiKey);
        case AIProvider::OLLAMA:
            return createOllamaProvider(baseUrl.empty() ? "http://localhost:11434" : baseUrl);
        default:
            return nullptr;
    }
}

void AIAgent::loadConfiguration() {
    // Load content filter configuration
    currentFilter_.filterLevel = config_->String("ai_filter_level", "moderate");
    currentFilter_.enableRagFiltering = config_->Bool("ai_enable_rag_filtering", true);
    
    // Load current provider
    std::string providerStr = config_->String("ai_current_provider", "openai");
    if (providerStr == "openai") currentProvider_ = AIProvider::OPENAI;
    else if (providerStr == "anthropic") currentProvider_ = AIProvider::ANTHROPIC;
    else if (providerStr == "google") currentProvider_ = AIProvider::GOOGLE_GEMINI;
    else if (providerStr == "ollama") currentProvider_ = AIProvider::OLLAMA;
    else currentProvider_ = AIProvider::OPENAI;
}

void AIAgent::saveConfiguration() {
    // Save content filter configuration
    config_->setString("ai_filter_level", currentFilter_.filterLevel);
    config_->setBool("ai_enable_rag_filtering", currentFilter_.enableRagFiltering);
    
    // Save current provider
    std::string providerStr;
    switch (currentProvider_) {
        case AIProvider::OPENAI: providerStr = "openai"; break;
        case AIProvider::ANTHROPIC: providerStr = "anthropic"; break;
        case AIProvider::GOOGLE_GEMINI: providerStr = "google"; break;
        case AIProvider::OLLAMA: providerStr = "ollama"; break;
        default: providerStr = "openai"; break;
    }
    config_->setString("ai_current_provider", providerStr);
}

std::string AIAgent::enhancePromptWithRAG(const AIRequest& request) {
    if (!ragService_ || request.documents.empty()) {
        return request.context;
    }
    
    std::ostringstream enhanced;
    enhanced << "Context from relevant documents:\n\n";
    
    // Search for relevant documents
    auto relevantDocs = ragService_->searchDocuments(request.prompt, 3);
    for (const auto& doc : relevantDocs) {
        enhanced << "- " << doc.title << ": " << doc.content.substr(0, 200);
        if (doc.content.length() > 200) enhanced << "...";
        enhanced << "\n\n";
    }
    
    if (!request.context.empty()) {
        enhanced << "Additional context:\n" << request.context << "\n\n";
    }
    
    return enhanced.str();
}

std::vector<std::string> AIAgent::getAvailableProviders() const {
    std::vector<std::string> available;
    for (const auto& pair : providers_) {
        if (pair.second && pair.second->isConfigured()) {
            available.push_back(pair.second->getProviderName());
        }
    }
    return available;
}

bool AIAgent::switchProvider(AIProvider provider) {
    auto it = providers_.find(provider);
    if (it != providers_.end() && it->second && it->second->isConfigured()) {
        currentProvider_ = provider;
        saveConfiguration();
        return true;
    }
    return false;
}

void AIAgent::addCustomProvider(std::unique_ptr<AIServiceProvider> provider) {
    if (provider) {
        providers_[AIProvider::CUSTOM] = std::move(provider);
    }
}