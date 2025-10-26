# AI Agent Configuration Template

## Provider Setup Instructions

### 1. OpenAI Configuration

#### Getting an API Key
1. Visit https://platform.openai.com/api-keys
2. Sign in to your OpenAI account or create one
3. Click "Create new secret key"
4. Copy the key (starts with "sk-")
5. Keep the key secure and never share it publicly

#### Configuration in App
```java
aiAgent.configureProvider(
    AIProvider.OPENAI, 
    "sk-your-actual-api-key-here", 
    null // Use default OpenAI endpoint
);
```

#### Supported Models
- gpt-3.5-turbo (default)
- gpt-4
- gpt-4-turbo

#### Rate Limits
- Free tier: 3 requests per minute
- Paid tier: Based on usage tier

### 2. Anthropic (Claude) Configuration

#### Getting an API Key
1. Visit https://console.anthropic.com/
2. Create an account and verify email
3. Go to API Keys section
4. Generate a new key (starts with "sk-ant-")
5. Copy and store securely

#### Configuration in App
```java
aiAgent.configureProvider(
    AIProvider.ANTHROPIC, 
    "sk-ant-your-actual-api-key-here", 
    null // Use default Anthropic endpoint
);
```

#### Supported Models
- claude-3-sonnet-20240229 (default)
- claude-3-haiku-20240307
- claude-3-opus-20240229

#### Rate Limits
- Varies by plan and model
- Check console for current limits

### 3. Google Gemini Configuration

#### Getting an API Key
1. Visit https://makersuite.google.com/app/apikey
2. Sign in with Google account
3. Click "Create API Key"
4. Copy the key (starts with "AIza")
5. Enable the Generative AI API if needed

#### Configuration in App
```java
aiAgent.configureProvider(
    AIProvider.GOOGLE_GEMINI, 
    "AIza-your-actual-api-key-here", 
    null // Use default Google endpoint
);
```

#### Supported Models
- gemini-pro (default)
- gemini-pro-vision

#### Rate Limits
- Free tier: 60 requests per minute
- Paid tier: Higher limits available

### 4. Ollama (Local Models) Configuration

#### Installation
1. Install Ollama: https://ollama.ai/
2. Download a model: `ollama pull llama2`
3. Start Ollama server: `ollama serve`
4. Server runs on http://localhost:11434

#### Configuration in App
```java
aiAgent.configureProvider(
    AIProvider.OLLAMA, 
    "", // No API key needed for local
    "http://localhost:11434" // Local Ollama server
);
```

#### Popular Models
- llama2 (7B, 13B, 70B)
- codellama (code generation)
- mistral (7B, 8x7B)
- neural-chat (conversational)

#### Rate Limits
- No limits (local processing)
- Limited by hardware performance

### 5. Custom Provider Configuration

#### For Custom Endpoints
```java
aiAgent.configureProvider(
    AIProvider.CUSTOM, 
    "your-custom-api-key", 
    "https://your-custom-endpoint.com/v1"
);
```

## Content Filter Configuration

### Filter Levels

#### Strict
- Blocks most potentially harmful content
- Conservative topic filtering
- Suitable for educational environments

```java
filter.filterLevel = "strict";
```

#### Moderate (Recommended)
- Balanced content filtering
- Blocks clearly harmful content
- Allows most educational/business content

```java
filter.filterLevel = "moderate";
```

#### Permissive
- Minimal content filtering
- Only blocks extreme content
- Maximum flexibility

```java
filter.filterLevel = "permissive";
```

### Topic Configuration

#### Allowed Topics Example
```java
filter.allowedTopics.add("education");
filter.allowedTopics.add("technology");
filter.allowedTopics.add("business");
filter.allowedTopics.add("science");
filter.allowedTopics.add("health");
filter.allowedTopics.add("creative writing");
```

#### Blocked Topics Example
```java
filter.blockedTopics.add("violence");
filter.blockedTopics.add("hate speech");
filter.blockedTopics.add("illegal activities");
filter.blockedTopics.add("harmful instructions");
```

### Use Case Configuration

#### Common Use Cases
```java
filter.allowedUseCases.add("summarization");
filter.allowedUseCases.add("question_answering");
filter.allowedUseCases.add("text_generation");
filter.allowedUseCases.add("key_extraction");
filter.allowedUseCases.add("writing_improvement");
filter.allowedUseCases.add("idea_generation");
filter.allowedUseCases.add("translation");
filter.allowedUseCases.add("proofreading");
```

## Environment-Specific Configuration

### Development Environment
```java
// Use test API keys
String openaiKey = BuildConfig.DEBUG ? "sk-test-key" : "sk-prod-key";
aiAgent.configureProvider(AIProvider.OPENAI, openaiKey, null);

// Enable debug logging
if (BuildConfig.DEBUG) {
    Log.setLevel(Log.DEBUG);
}
```

### Production Environment
```java
// Use secure key storage
String apiKey = EncryptedPreferences.getString("openai_api_key");
aiAgent.configureProvider(AIProvider.OPENAI, apiKey, null);

// Enable error reporting
CrashReporting.enableAIErrors(true);
```

### Testing Environment
```java
// Use mock responses for testing
if (BuildConfig.FLAVOR.equals("test")) {
    aiAgent.addCustomProvider(new MockAIProvider());
}
```

## Security Best Practices

### API Key Management
1. **Never hardcode API keys** in source code
2. **Use environment variables** or secure storage
3. **Rotate keys regularly** (every 90 days)
4. **Monitor usage** for unexpected spikes
5. **Implement key validation** before use

### Content Security
1. **Enable content filtering** appropriate for your use case
2. **Log filtered requests** for monitoring
3. **Implement rate limiting** per user
4. **Sanitize inputs** before sending to AI
5. **Validate responses** before displaying

### Network Security
1. **Use HTTPS only** for all AI API calls
2. **Implement certificate pinning** where possible
3. **Add request/response logging** for debugging
4. **Set reasonable timeouts** (30 seconds max)
5. **Handle network failures** gracefully

## Monitoring and Analytics

### Usage Tracking
```java
// Track AI usage
Analytics.track("ai_request", Map.of(
    "provider", aiAgent.getCurrentProvider(),
    "use_case", request.useCase,
    "success", response.success
));
```

### Error Monitoring
```java
// Monitor AI errors
if (!response.success) {
    ErrorReporting.log("AI Request Failed", Map.of(
        "provider", aiAgent.getCurrentProvider(),
        "error", response.error,
        "prompt_length", request.prompt.length()
    ));
}
```

### Performance Metrics
```java
// Track response times
long startTime = System.currentTimeMillis();
aiAgent.generateText(prompt, context, new AIResponseCallback() {
    @Override
    public void onSuccess(AIResponse response) {
        long duration = System.currentTimeMillis() - startTime;
        Metrics.record("ai_response_time", duration);
    }
});
```

## Troubleshooting

### Common Issues

#### "API key not valid"
- Check key format (OpenAI: sk-..., Anthropic: sk-ant-...)
- Verify key hasn't expired
- Ensure sufficient credits/quota
- Test key with simple API call

#### "Rate limit exceeded"
- Check current usage limits
- Implement exponential backoff
- Consider upgrading plan
- Cache responses when possible

#### "Network timeout"
- Check internet connectivity
- Verify API endpoint URLs
- Increase timeout values
- Test with different network

#### "Content blocked by filter"
- Review content filter settings
- Check allowed/blocked topics
- Verify use case permissions
- Test with different prompt

### Debug Commands

#### Test Configuration
```java
// Test each provider
for (AIProvider provider : AIProvider.values()) {
    aiAgent.switchProvider(provider);
    aiAgent.testConnection().thenAccept(success -> {
        Log.i("Test", provider + ": " + (success ? "OK" : "FAIL"));
    });
}
```

#### Validate Content Filter
```java
// Test content filtering
ContentFilterEngine filter = new ContentFilterEngine(config);
boolean allowed = filter.isContentAllowed("test content", request);
Log.i("Filter", "Content allowed: " + allowed);
if (!allowed) {
    Log.i("Filter", "Reason: " + filter.getFilterReason());
}
```

#### Check RAG Index
```java
// Verify RAG documents
aiAgent.searchRelevantContent("test query")
    .forEach(doc -> Log.i("RAG", "Found: " + doc.title));
```

For additional support or questions, please refer to the main documentation or create an issue in the repository.