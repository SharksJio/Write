#include "aiagent.h"
#include "ulib/stringutil.h"
#include <algorithm>
#include <fstream>
#include <sstream>
#include <cmath>

// Simple text similarity functions
class TextAnalyzer {
public:
    static std::vector<std::string> tokenize(const std::string& text);
    static float calculateSimilarity(const std::string& query, const std::string& document);
    static std::vector<std::string> extractKeywords(const std::string& text);
    static std::string normalizeText(const std::string& text);
};

// Local RAG Service Implementation
class LocalRAGService : public RAGService {
public:
    LocalRAGService(const std::string& indexPath) : indexPath_(indexPath) {
        loadIndex();
    }
    
    ~LocalRAGService() {
        saveIndex();
    }

    std::vector<RAGDocument> searchDocuments(const std::string& query, int maxResults = 5) override {
        std::vector<std::pair<float, RAGDocument*>> scored;
        
        std::string normalizedQuery = TextAnalyzer::normalizeText(query);
        
        // Score all documents
        for (auto& doc : documents_) {
            float score = TextAnalyzer::calculateSimilarity(normalizedQuery, doc.content);
            if (score > 0.1f) { // Minimum threshold
                scored.push_back({score, &doc});
            }
        }
        
        // Sort by score (descending)
        std::sort(scored.begin(), scored.end(), 
                 [](const auto& a, const auto& b) { return a.first > b.first; });
        
        // Return top results
        std::vector<RAGDocument> results;
        int count = std::min(maxResults, static_cast<int>(scored.size()));
        for (int i = 0; i < count; ++i) {
            RAGDocument result = *scored[i].second;
            result.relevanceScore = scored[i].first;
            results.push_back(result);
        }
        
        return results;
    }
    
    bool indexDocument(const RAGDocument& document) override {
        // Check if document already exists
        auto it = std::find_if(documents_.begin(), documents_.end(),
                              [&](const RAGDocument& doc) { return doc.id == document.id; });
        
        if (it != documents_.end()) {
            // Update existing document
            *it = document;
        } else {
            // Add new document
            documents_.push_back(document);
        }
        
        return true;
    }
    
    bool removeDocument(const std::string& documentId) override {
        auto it = std::find_if(documents_.begin(), documents_.end(),
                              [&](const RAGDocument& doc) { return doc.id == documentId; });
        
        if (it != documents_.end()) {
            documents_.erase(it);
            return true;
        }
        
        return false;
    }
    
    void clearIndex() override {
        documents_.clear();
    }

private:
    std::string indexPath_;
    std::vector<RAGDocument> documents_;
    
    void loadIndex() {
        std::ifstream file(indexPath_);
        if (!file.is_open()) {
            return; // No existing index
        }
        
        std::string line;
        RAGDocument currentDoc;
        bool inDocument = false;
        
        while (std::getline(file, line)) {
            if (line == "---DOC_START---") {
                inDocument = true;
                currentDoc = RAGDocument();
            } else if (line == "---DOC_END---") {
                if (inDocument) {
                    documents_.push_back(currentDoc);
                    inDocument = false;
                }
            } else if (inDocument) {
                if (line.substr(0, 3) == "ID:") {
                    currentDoc.id = line.substr(3);
                } else if (line.substr(0, 6) == "TITLE:") {
                    currentDoc.title = line.substr(6);
                } else if (line.substr(0, 7) == "SOURCE:") {
                    currentDoc.source = line.substr(7);
                } else if (line.substr(0, 8) == "CONTENT:") {
                    currentDoc.content = line.substr(8);
                    // Handle multi-line content
                    std::string contentLine;
                    while (std::getline(file, contentLine) && contentLine != "---DOC_END---") {
                        currentDoc.content += "\n" + contentLine;
                    }
                    // Put back the end marker
                    if (contentLine == "---DOC_END---") {
                        if (inDocument) {
                            documents_.push_back(currentDoc);
                            inDocument = false;
                        }
                    }
                }
            }
        }
        
        file.close();
    }
    
    void saveIndex() {
        std::ofstream file(indexPath_);
        if (!file.is_open()) {
            return;
        }
        
        for (const auto& doc : documents_) {
            file << "---DOC_START---\n";
            file << "ID:" << doc.id << "\n";
            file << "TITLE:" << doc.title << "\n";
            file << "SOURCE:" << doc.source << "\n";
            file << "CONTENT:" << doc.content << "\n";
            file << "---DOC_END---\n";
        }
        
        file.close();
    }
};

// TextAnalyzer implementation
std::vector<std::string> TextAnalyzer::tokenize(const std::string& text) {
    std::vector<std::string> tokens;
    std::string normalized = normalizeText(text);
    
    std::stringstream ss(normalized);
    std::string token;
    
    while (ss >> token) {
        if (token.length() > 2) { // Filter out very short words
            tokens.push_back(token);
        }
    }
    
    return tokens;
}

float TextAnalyzer::calculateSimilarity(const std::string& query, const std::string& document) {
    auto queryTokens = tokenize(query);
    auto docTokens = tokenize(document);
    
    if (queryTokens.empty() || docTokens.empty()) {
        return 0.0f;
    }
    
    // Simple term frequency similarity
    int matches = 0;
    for (const auto& queryToken : queryTokens) {
        for (const auto& docToken : docTokens) {
            if (queryToken == docToken) {
                matches++;
                break;
            }
        }
    }
    
    // Normalize by query length
    return static_cast<float>(matches) / static_cast<float>(queryTokens.size());
}

std::vector<std::string> TextAnalyzer::extractKeywords(const std::string& text) {
    auto tokens = tokenize(text);
    
    // Simple keyword extraction - remove common stop words
    std::vector<std::string> stopWords = {
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "can", "this", "that", "these", "those"
    };
    
    std::vector<std::string> keywords;
    for (const auto& token : tokens) {
        if (std::find(stopWords.begin(), stopWords.end(), token) == stopWords.end()) {
            keywords.push_back(token);
        }
    }
    
    return keywords;
}

std::string TextAnalyzer::normalizeText(const std::string& text) {
    std::string normalized;
    normalized.reserve(text.length());
    
    for (char c : text) {
        if (std::isalnum(c)) {
            normalized += std::tolower(c);
        } else if (std::isspace(c)) {
            if (!normalized.empty() && normalized.back() != ' ') {
                normalized += ' ';
            }
        }
    }
    
    return normalized;
}

// Factory function implementations
std::unique_ptr<RAGService> createLocalRAGService(const std::string& indexPath) {
    return std::make_unique<LocalRAGService>(indexPath);
}

std::unique_ptr<RAGService> createVectorRAGService(const std::string& connectionString) {
    // TODO: Implement vector database RAG service (e.g., using ChromaDB, Pinecone, etc.)
    return nullptr;
}