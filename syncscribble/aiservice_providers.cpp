#include "aiagent.h"
#include "ulib/stringutil.h"
#include "pugixml.hpp"
#include <sstream>
#include <regex>

// Include the existing networking functions
extern "C" {
    int unet_socket(int domain, int type, int flags, const char* node, const char* service);
    int unet_select(int readfd, int writefd, int timeout);
    int unet_send(int socket, const void* buffer, size_t length);
    int unet_recv(int socket, void* buffer, size_t length);
    int unet_close(int socket);
    int unet_shutdown(int socket, int how);
    
    #define UNET_TCP 1
    #define UNET_CONNECT 2
    #define UNET_NOBLOCK 4
    #define UNET_RDY_RD 1
    #define UNET_RDY_WR 2
    #define UNET_SHUT_RDWR 2
}

// HTTP helper for making API calls using the existing unet API
class HTTPClient {
public:
    struct Response {
        int statusCode = 0;
        std::string body;
        bool success = false;
    };
    
    static Response post(const std::string& url, const std::string& data, 
                        const std::map<std::string, std::string>& headers = {});
    static Response get(const std::string& url, 
                       const std::map<std::string, std::string>& headers = {});

private:
    static std::string escapeJson(const std::string& str);
    static std::pair<std::string, std::string> parseUrl(const std::string& url);
    static Response makeRequest(const std::string& method, const std::string& url, 
                               const std::string& data, const std::map<std::string, std::string>& headers);
};

// OpenAI Provider Implementation
class OpenAIProvider : public AIServiceProvider {
public:
    OpenAIProvider(const std::string& apiKey, const std::string& baseUrl = "")
        : apiKey_(apiKey), baseUrl_(baseUrl.empty() ? "https://api.openai.com/v1" : baseUrl) {}

    AIResponse generateResponse(const AIRequest& request) override {
        AIResponse response;
        
        if (apiKey_.empty()) {
            response.error = "OpenAI API key not configured";
            return response;
        }
        
        // Prepare the request
        std::ostringstream jsonPayload;
        jsonPayload << "{\n";
        jsonPayload << "  \"model\": \"gpt-3.5-turbo\",\n";
        jsonPayload << "  \"messages\": [\n";
        
        if (!request.context.empty()) {
            jsonPayload << "    {\"role\": \"system\", \"content\": \"" 
                       << HTTPClient::escapeJson(request.context) << "\"},\n";
        }
        
        jsonPayload << "    {\"role\": \"user\", \"content\": \"" 
                   << HTTPClient::escapeJson(request.prompt) << "\"}\n";
        jsonPayload << "  ],\n";
        jsonPayload << "  \"max_tokens\": " << request.maxTokens << ",\n";
        jsonPayload << "  \"temperature\": " << request.temperature << "\n";
        jsonPayload << "}";
        
        // Set up headers
        std::map<std::string, std::string> headers;
        headers["Authorization"] = "Bearer " + apiKey_;
        headers["Content-Type"] = "application/json";
        
        // Make the API call
        auto httpResponse = HTTPClient::post(baseUrl_ + "/chat/completions", 
                                           jsonPayload.str(), headers);
        
        if (!httpResponse.success) {
            response.error = "Failed to connect to OpenAI API";
            return response;
        }
        
        if (httpResponse.statusCode != 200) {
            response.error = fstring("OpenAI API error: HTTP %d", httpResponse.statusCode);
            return response;
        }
        
        // Parse the response
        return parseOpenAIResponse(httpResponse.body);
    }
    
    bool isConfigured() const override {
        return !apiKey_.empty();
    }
    
    std::string getProviderName() const override {
        return "OpenAI";
    }
    
    bool testConnection() override {
        if (apiKey_.empty()) return false;
        
        // Test with a minimal request
        AIRequest testRequest;
        testRequest.prompt = "Test";
        testRequest.maxTokens = 5;
        
        auto response = generateResponse(testRequest);
        return response.success;
    }

private:
    std::string apiKey_;
    std::string baseUrl_;
    
    AIResponse parseOpenAIResponse(const std::string& jsonResponse) {
        AIResponse response;
        
        try {
            // Simple JSON parsing - in a real implementation, use a proper JSON library
            // This is a basic parser for demonstration
            size_t contentStart = jsonResponse.find("\"content\":");
            if (contentStart == std::string::npos) {
                response.error = "Invalid response format";
                return response;
            }
            
            contentStart = jsonResponse.find("\"", contentStart + 10);
            if (contentStart == std::string::npos) {
                response.error = "Invalid response format";
                return response;
            }
            contentStart++;
            
            size_t contentEnd = contentStart;
            int escapeCount = 0;
            while (contentEnd < jsonResponse.length()) {
                if (jsonResponse[contentEnd] == '\\') {
                    escapeCount++;
                } else if (jsonResponse[contentEnd] == '"' && escapeCount % 2 == 0) {
                    break;
                } else {
                    escapeCount = 0;
                }
                contentEnd++;
            }
            
            if (contentEnd < jsonResponse.length()) {
                response.content = jsonResponse.substr(contentStart, contentEnd - contentStart);
                response.success = true;
            } else {
                response.error = "Failed to parse response content";
            }
            
        } catch (const std::exception& e) {
            response.error = fstring("JSON parsing error: %s", e.what());
        }
        
        return response;
    }
};

// Anthropic Provider Implementation
class AnthropicProvider : public AIServiceProvider {
public:
    AnthropicProvider(const std::string& apiKey) : apiKey_(apiKey) {}

    AIResponse generateResponse(const AIRequest& request) override {
        AIResponse response;
        
        if (apiKey_.empty()) {
            response.error = "Anthropic API key not configured";
            return response;
        }
        
        // Prepare the request for Claude
        std::ostringstream jsonPayload;
        jsonPayload << "{\n";
        jsonPayload << "  \"model\": \"claude-3-sonnet-20240229\",\n";
        jsonPayload << "  \"max_tokens\": " << request.maxTokens << ",\n";
        jsonPayload << "  \"messages\": [\n";
        
        std::string fullPrompt = request.prompt;
        if (!request.context.empty()) {
            fullPrompt = request.context + "\n\n" + request.prompt;
        }
        
        jsonPayload << "    {\"role\": \"user\", \"content\": \"" 
                   << HTTPClient::escapeJson(fullPrompt) << "\"}\n";
        jsonPayload << "  ]\n";
        jsonPayload << "}";
        
        // Set up headers
        std::map<std::string, std::string> headers;
        headers["x-api-key"] = apiKey_;
        headers["Content-Type"] = "application/json";
        headers["anthropic-version"] = "2023-06-01";
        
        // Make the API call
        auto httpResponse = HTTPClient::post("https://api.anthropic.com/v1/messages", 
                                           jsonPayload.str(), headers);
        
        if (!httpResponse.success) {
            response.error = "Failed to connect to Anthropic API";
            return response;
        }
        
        if (httpResponse.statusCode != 200) {
            response.error = fstring("Anthropic API error: HTTP %d", httpResponse.statusCode);
            return response;
        }
        
        // Parse the response (similar to OpenAI but different structure)
        return parseAnthropicResponse(httpResponse.body);
    }
    
    bool isConfigured() const override {
        return !apiKey_.empty();
    }
    
    std::string getProviderName() const override {
        return "Anthropic";
    }
    
    bool testConnection() override {
        if (apiKey_.empty()) return false;
        
        AIRequest testRequest;
        testRequest.prompt = "Test";
        testRequest.maxTokens = 5;
        
        auto response = generateResponse(testRequest);
        return response.success;
    }

private:
    std::string apiKey_;
    
    AIResponse parseAnthropicResponse(const std::string& jsonResponse) {
        AIResponse response;
        
        try {
            // Basic JSON parsing for Anthropic response format
            size_t contentStart = jsonResponse.find("\"text\":");
            if (contentStart == std::string::npos) {
                response.error = "Invalid Anthropic response format";
                return response;
            }
            
            contentStart = jsonResponse.find("\"", contentStart + 7);
            if (contentStart == std::string::npos) {
                response.error = "Invalid Anthropic response format";
                return response;
            }
            contentStart++;
            
            size_t contentEnd = contentStart;
            while (contentEnd < jsonResponse.length() && jsonResponse[contentEnd] != '"') {
                if (jsonResponse[contentEnd] == '\\') contentEnd++; // Skip escaped chars
                contentEnd++;
            }
            
            if (contentEnd < jsonResponse.length()) {
                response.content = jsonResponse.substr(contentStart, contentEnd - contentStart);
                response.success = true;
            } else {
                response.error = "Failed to parse Anthropic response content";
            }
            
        } catch (const std::exception& e) {
            response.error = fstring("Anthropic JSON parsing error: %s", e.what());
        }
        
        return response;
    }
};

// Ollama Provider Implementation (for local models)
class OllamaProvider : public AIServiceProvider {
public:
    OllamaProvider(const std::string& baseUrl) : baseUrl_(baseUrl) {}

    AIResponse generateResponse(const AIRequest& request) override {
        AIResponse response;
        
        // Prepare the request
        std::ostringstream jsonPayload;
        jsonPayload << "{\n";
        jsonPayload << "  \"model\": \"llama2\",\n";
        jsonPayload << "  \"prompt\": \"";
        
        if (!request.context.empty()) {
            jsonPayload << HTTPClient::escapeJson(request.context) << "\\n\\n";
        }
        
        jsonPayload << HTTPClient::escapeJson(request.prompt) << "\",\n";
        jsonPayload << "  \"stream\": false\n";
        jsonPayload << "}";
        
        // Set up headers
        std::map<std::string, std::string> headers;
        headers["Content-Type"] = "application/json";
        
        // Make the API call
        auto httpResponse = HTTPClient::post(baseUrl_ + "/api/generate", 
                                           jsonPayload.str(), headers);
        
        if (!httpResponse.success) {
            response.error = "Failed to connect to Ollama";
            return response;
        }
        
        if (httpResponse.statusCode != 200) {
            response.error = fstring("Ollama error: HTTP %d", httpResponse.statusCode);
            return response;
        }
        
        // Parse the response
        return parseOllamaResponse(httpResponse.body);
    }
    
    bool isConfigured() const override {
        return !baseUrl_.empty();
    }
    
    std::string getProviderName() const override {
        return "Ollama";
    }
    
    bool testConnection() override {
        // Test connection with models endpoint
        auto response = HTTPClient::get(baseUrl_ + "/api/tags");
        return response.success && response.statusCode == 200;
    }

private:
    std::string baseUrl_;
    
    AIResponse parseOllamaResponse(const std::string& jsonResponse) {
        AIResponse response;
        
        try {
            // Parse Ollama response format
            size_t responseStart = jsonResponse.find("\"response\":");
            if (responseStart == std::string::npos) {
                response.error = "Invalid Ollama response format";
                return response;
            }
            
            responseStart = jsonResponse.find("\"", responseStart + 11);
            if (responseStart == std::string::npos) {
                response.error = "Invalid Ollama response format";
                return response;
            }
            responseStart++;
            
            size_t responseEnd = responseStart;
            while (responseEnd < jsonResponse.length() && jsonResponse[responseEnd] != '"') {
                if (jsonResponse[responseEnd] == '\\') responseEnd++; // Skip escaped chars
                responseEnd++;
            }
            
            if (responseEnd < jsonResponse.length()) {
                response.content = jsonResponse.substr(responseStart, responseEnd - responseStart);
                response.success = true;
            } else {
                response.error = "Failed to parse Ollama response content";
            }
            
        } catch (const std::exception& e) {
            response.error = fstring("Ollama JSON parsing error: %s", e.what());
        }
        
        return response;
    }
};

// Factory function implementations
std::unique_ptr<AIServiceProvider> createOpenAIProvider(const std::string& apiKey, const std::string& baseUrl) {
    return std::make_unique<OpenAIProvider>(apiKey, baseUrl);
}

std::unique_ptr<AIServiceProvider> createAnthropicProvider(const std::string& apiKey) {
    return std::make_unique<AnthropicProvider>(apiKey);
}

std::unique_ptr<AIServiceProvider> createOllamaProvider(const std::string& baseUrl) {
    return std::make_unique<OllamaProvider>(baseUrl);
}

std::unique_ptr<AIServiceProvider> createGoogleProvider(const std::string& apiKey) {
    // TODO: Implement Google Gemini provider
    return nullptr;
}

// HTTPClient implementation using existing unet API
HTTPClient::Response HTTPClient::post(const std::string& url, const std::string& data, 
                                     const std::map<std::string, std::string>& headers) {
    return makeRequest("POST", url, data, headers);
}

HTTPClient::Response HTTPClient::get(const std::string& url, 
                                    const std::map<std::string, std::string>& headers) {
    return makeRequest("GET", url, "", headers);
}

std::pair<std::string, std::string> HTTPClient::parseUrl(const std::string& url) {
    // Simple URL parsing - extract host and path
    std::string host, path;
    
    size_t protocolEnd = url.find("://");
    if (protocolEnd == std::string::npos) {
        return {"", ""};
    }
    
    size_t hostStart = protocolEnd + 3;
    size_t pathStart = url.find("/", hostStart);
    
    if (pathStart == std::string::npos) {
        host = url.substr(hostStart);
        path = "/";
    } else {
        host = url.substr(hostStart, pathStart - hostStart);
        path = url.substr(pathStart);
    }
    
    return {host, path};
}

HTTPClient::Response HTTPClient::makeRequest(const std::string& method, const std::string& url, 
                                           const std::string& data, const std::map<std::string, std::string>& headers) {
    Response response;
    
    auto [host, path] = parseUrl(url);
    if (host.empty()) {
        response.success = false;
        return response;
    }
    
    // Determine port (default to 443 for https, 80 for http)
    std::string port = "443";
    if (url.find("http://") == 0) {
        port = "80";
    }
    
    // Create socket
    int sock = unet_socket(UNET_TCP, UNET_CONNECT, UNET_NOBLOCK, host.c_str(), port.c_str());
    if (sock == -1) {
        response.success = false;
        return response;
    }
    
    // Wait for connection
    if (unet_select(-1, sock, 10) <= 0) {
        unet_close(sock);
        response.success = false;
        return response;
    }
    
    // Build HTTP request
    std::ostringstream request;
    request << method << " " << path << " HTTP/1.1\r\n";
    request << "Host: " << host << "\r\n";
    request << "Connection: close\r\n";
    
    // Add custom headers
    for (const auto& header : headers) {
        request << header.first << ": " << header.second << "\r\n";
    }
    
    // Add content length for POST requests
    if (method == "POST" && !data.empty()) {
        request << "Content-Length: " << data.length() << "\r\n";
    }
    
    request << "\r\n";
    
    // Add POST data
    if (method == "POST" && !data.empty()) {
        request << data;
    }
    
    std::string requestStr = request.str();
    
    // Send request
    if (unet_send(sock, requestStr.data(), requestStr.size()) <= 0) {
        unet_close(sock);
        response.success = false;
        return response;
    }
    
    // Read response
    std::string responseData;
    char buffer[4096];
    
    while (unet_select(sock, -1, 4) > 0) {
        int bytesRead = unet_recv(sock, buffer, sizeof(buffer) - 1);
        if (bytesRead <= 0) {
            break;
        }
        buffer[bytesRead] = '\0';
        responseData.append(buffer, bytesRead);
    }
    
    unet_close(sock);
    
    if (responseData.empty()) {
        response.success = false;
        return response;
    }
    
    // Parse HTTP response
    size_t headerEnd = responseData.find("\r\n\r\n");
    if (headerEnd == std::string::npos) {
        response.success = false;
        return response;
    }
    
    std::string headerSection = responseData.substr(0, headerEnd);
    response.body = responseData.substr(headerEnd + 4);
    
    // Extract status code
    size_t statusStart = headerSection.find(" ");
    if (statusStart != std::string::npos) {
        size_t statusEnd = headerSection.find(" ", statusStart + 1);
        if (statusEnd != std::string::npos) {
            std::string statusStr = headerSection.substr(statusStart + 1, statusEnd - statusStart - 1);
            response.statusCode = std::stoi(statusStr);
        }
    }
    
    response.success = true;
    return response;
}

std::string HTTPClient::escapeJson(const std::string& str) {
    std::string escaped;
    escaped.reserve(str.length() * 1.1); // Reserve some extra space
    
    for (char c : str) {
        switch (c) {
            case '"': escaped += "\\\""; break;
            case '\\': escaped += "\\\\"; break;
            case '\b': escaped += "\\b"; break;
            case '\f': escaped += "\\f"; break;
            case '\n': escaped += "\\n"; break;
            case '\r': escaped += "\\r"; break;
            case '\t': escaped += "\\t"; break;
            default: escaped += c; break;
        }
    }
    
    return escaped;
}