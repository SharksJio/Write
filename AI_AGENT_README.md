# Android AI Agent Integration Guide

## Overview

The Android AI Agent provides a comprehensive solution for integrating AI capabilities into the Write note-taking application. It supports multiple AI LLM providers, advanced content filtering, and RAG (Retrieval-Augmented Generation) for context-aware responses.

## Features

### Multi-Provider AI Support
- **OpenAI**: GPT-3.5-turbo and GPT-4 models
- **Anthropic**: Claude 3 models  
- **Google Gemini**: Gemini Pro and Ultra models
- **Ollama**: Local models (Llama 2, Code Llama, etc.)
- **Custom**: Support for custom API endpoints

### Content Filtering
- **Topic-based filtering**: Allow/block specific topics
- **Use-case restrictions**: Control AI usage scenarios
- **Safety levels**: Configurable content safety (strict/moderate/permissive)
- **RAG filtering**: Filter responses based on document context

### RAG Capabilities
- **Local document indexing**: Index notes for context-aware responses
- **Semantic search**: Find relevant content based on user queries
- **Context injection**: Automatically include relevant document snippets
- **Vector similarity**: Basic text similarity for document retrieval

## Architecture

### Core Components

```
AIAgent (C++)
├── AIServiceProvider (Interface)
│   ├── OpenAIProvider
│   ├── AnthropicProvider
│   ├── GoogleProvider
│   └── OllamaProvider
├── ContentFilterEngine
├── RAGService
│   └── LocalRAGService
└── HTTPClient (using unet)

AIAgentManager (Java)
├── AIChatActivity
├── AIConfigActivity
└── JNI Bridge
```

### Key Classes

#### AIAgent (C++)
```cpp
class AIAgent {
public:
    void configure(AIProvider provider, const std::string& apiKey, const std::string& baseUrl = "");
    AIResponse processRequest(const AIRequest& request);
    AIResponse generateText(const std::string& prompt, const std::string& context = "");
    AIResponse summarizeContent(const std::string& content);
    AIResponse extractKeyPoints(const std::string& content);
    bool indexDocument(const std::string& content, const std::string& title, const std::string& id = "");
};
```

#### AIAgentManager (Java)
```java
public class AIAgentManager {
    public void configureProvider(AIProvider provider, String apiKey, String baseUrl, ConfigurationCallback callback);
    public void generateResponse(AIRequest request, AIResponseCallback callback);
    public void generateText(String prompt, String context, AIResponseCallback callback);
    public void summarizeContent(String content, AIResponseCallback callback);
    public CompletableFuture<Boolean> indexDocument(String content, String title, String id);
}
```

## Usage Guide

### 1. Configuration

#### Setting up an AI Provider
```java
AIAgentManager aiAgent = AIAgentManager.getInstance(context);

// Configure OpenAI
aiAgent.configureProvider(
    AIProvider.OPENAI, 
    "your-api-key", 
    null, // Default base URL
    new ConfigurationCallback() {
        @Override
        public void onConfigured(boolean success, String message) {
            if (success) {
                Log.i("AI", "OpenAI configured successfully");
            }
        }
    }
);
```

#### Content Filter Setup
```java
ContentFilter filter = new ContentFilter();
filter.filterLevel = "moderate";
filter.allowedTopics.add("education");
filter.allowedTopics.add("technology");
filter.blockedTopics.add("harmful_content");
filter.allowedUseCases.add("summarization");
filter.allowedUseCases.add("question_answering");

aiAgent.setContentFilter(filter);
```

### 2. Basic AI Operations

#### Generate Text
```java
aiAgent.generateText("Explain quantum computing", "", new AIResponseCallback() {
    @Override
    public void onSuccess(AIResponse response) {
        // Handle successful response
        String generatedText = response.content;
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

#### Summarize Content
```java
String content = "Your long document content here...";
aiAgent.summarizeContent(content, new AIResponseCallback() {
    @Override
    public void onSuccess(AIResponse response) {
        String summary = response.content;
        // Display summary to user
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

#### Extract Key Points
```java
aiAgent.extractKeyPoints(content, new AIResponseCallback() {
    @Override
    public void onSuccess(AIResponse response) {
        String keyPoints = response.content;
        // Display key points as bulleted list
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

### 3. RAG Operations

#### Index Documents
```java
// Index current document for RAG
aiAgent.indexDocument(documentContent, documentTitle, documentId)
    .thenAccept(success -> {
        if (success) {
            Log.i("RAG", "Document indexed successfully");
        }
    });
```

#### Context-Aware Queries
When RAG is enabled, the AI will automatically search for relevant document content and include it in responses.

## UI Components

### AIChatActivity
A Material 3 chat interface with:
- Message history display
- Quick action chips (Summarize, Extract Key Points, etc.)
- Real-time typing and response indicators
- Error handling and status messages

### AIConfigActivity  
Configuration interface featuring:
- Provider selection dropdown
- API key management
- Content filter settings
- Connection testing
- Preset topic chips

## Integration Points

### Menu Integration
Add AI options to the main menu:
```xml
<item android:id="@+id/action_ai_chat"
      android:title="AI Assistant"
      android:icon="@drawable/ic_smart_toy"
      app:showAsAction="ifRoom" />
```

### Native Activity Integration
```java
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_ai_chat) {
        Intent intent = new Intent(this, AIChatActivity.class);
        startActivity(intent);
        return true;
    }
    return super.onOptionsItemSelected(item);
}
```

## Security Considerations

### API Key Management
- API keys are stored securely using Android SharedPreferences
- Keys are not logged or exposed in debug builds
- Support for environment-based configuration

### Content Filtering
- Three-tier safety filtering (strict/moderate/permissive)
- Topic-based allow/block lists
- Use-case restrictions to prevent misuse
- Response filtering to catch inappropriate AI outputs

### Network Security
- HTTPS-only connections for all API calls
- Certificate validation
- Request/response sanitization

## Provider-Specific Setup

### OpenAI
```java
aiAgent.configureProvider(AIProvider.OPENAI, "sk-...", null);
```
- Requires valid OpenAI API key
- Supports GPT-3.5-turbo and GPT-4 models
- Rate limiting handled automatically

### Anthropic (Claude)
```java
aiAgent.configureProvider(AIProvider.ANTHROPIC, "sk-ant-...", null);
```
- Requires Anthropic API key
- Supports Claude 3 models
- Different request format than OpenAI

### Google Gemini
```java
aiAgent.configureProvider(AIProvider.GOOGLE_GEMINI, "AIza...", null);
```
- Requires Google Cloud API key
- Access to Gemini Pro models
- Google-specific authentication

### Ollama (Local Models)
```java
aiAgent.configureProvider(AIProvider.OLLAMA, "", "http://localhost:11434");
```
- No API key required for local installation
- Supports various open-source models
- Requires Ollama server running locally

## Error Handling

### Common Error Scenarios
1. **Network connectivity issues**
2. **Invalid API keys**
3. **Rate limiting**
4. **Content filtering blocks**
5. **Model unavailability**

### Error Response Structure
```java
public static class AIResponse {
    public String content = "";
    public String filteredReason = "";
    public boolean success = false;
    public String error = "";
    public float confidence = 0.0f;
}
```

## Performance Considerations

### Async Operations
All AI operations are performed asynchronously using:
- ExecutorService for background processing
- CompletableFuture for async operations
- Callback interfaces for UI updates

### Caching
- RAG document index cached locally
- Provider configurations persisted
- Response caching for identical queries

### Rate Limiting
- Built-in request throttling
- Provider-specific rate limits respected
- Graceful handling of rate limit errors

## Testing

### Unit Tests
```java
@Test
public void testAIProviderConfiguration() {
    AIAgentManager agent = AIAgentManager.getInstance(context);
    // Test configuration logic
}

@Test
public void testContentFiltering() {
    ContentFilter filter = new ContentFilter();
    // Test filtering logic
}
```

### Integration Tests
- Test actual API calls with test keys
- Validate response parsing
- Test error handling scenarios

## Troubleshooting

### Common Issues

1. **"AI agent not configured"**
   - Ensure AI provider is configured with valid API key
   - Check network connectivity

2. **"Content blocked by filter"**
   - Review content filter settings
   - Adjust allowed topics or use cases

3. **"Connection failed"**
   - Verify API endpoint accessibility
   - Check API key validity
   - Confirm network permissions

### Debug Logging
Enable debug logging to troubleshoot issues:
```java
private static final String TAG = "AIAgent";
Log.d(TAG, "AI request: " + request.prompt);
```

## Future Enhancements

### Planned Features
- **Streaming responses** for real-time output
- **Voice input/output** integration
- **Advanced RAG** with vector embeddings
- **Custom model fine-tuning**
- **Collaborative AI** for team workflows

### Extension Points
- Plugin architecture for custom providers
- Webhook support for external integrations
- Advanced analytics and usage tracking
- Multi-language support

## Contributing

### Adding New Providers
1. Implement `AIServiceProvider` interface
2. Add provider to enum and factory
3. Update configuration UI
4. Add tests and documentation

### Extending Content Filtering
1. Modify `ContentFilterEngine` class
2. Add new filter criteria
3. Update configuration interface
4. Test with various content types

For questions or contributions, please refer to the project's main README or open an issue on GitHub.