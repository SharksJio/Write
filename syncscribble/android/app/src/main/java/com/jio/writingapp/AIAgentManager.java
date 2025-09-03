package com.jio.writingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

/**
 * Android AI Agent Manager - handles AI service integration and configuration
 */
public class AIAgentManager {
    private static final String TAG = "AIAgentManager";
    private static final String PREFS_NAME = "ai_agent_prefs";
    
    // Native AI Agent instance
    private long nativeAgentPtr = 0;
    
    // Configuration
    private SharedPreferences preferences;
    private ExecutorService executorService;
    private Context context;
    
    // AI Provider Types
    public enum AIProvider {
        OPENAI("openai"),
        ANTHROPIC("anthropic"), 
        GOOGLE_GEMINI("google"),
        OLLAMA("ollama"),
        CUSTOM("custom");
        
        private final String name;
        AIProvider(String name) { this.name = name; }
        public String getName() { return name; }
    }
    
    // Content Filter Configuration
    public static class ContentFilter {
        public List<String> allowedTopics = new ArrayList<>();
        public List<String> blockedTopics = new ArrayList<>();
        public List<String> allowedUseCases = new ArrayList<>();
        public String filterLevel = "moderate"; // "strict", "moderate", "permissive"
        public boolean enableRagFiltering = true;
    }
    
    // AI Request/Response classes
    public static class AIRequest {
        public String prompt;
        public String context = "";
        public List<String> documents = new ArrayList<>();
        public AIProvider provider = AIProvider.OPENAI;
        public int maxTokens = 1000;
        public float temperature = 0.7f;
        public String useCase = "general";
    }
    
    public static class AIResponse {
        public String content = "";
        public String filteredReason = "";
        public boolean success = false;
        public String error = "";
        public float confidence = 0.0f;
    }
    
    // Callback interfaces
    public interface AIResponseCallback {
        void onSuccess(AIResponse response);
        void onError(String error);
    }
    
    public interface ConfigurationCallback {
        void onConfigured(boolean success, String message);
    }
    
    // Singleton instance
    private static AIAgentManager instance;
    
    public static synchronized AIAgentManager getInstance(Context context) {
        if (instance == null) {
            instance = new AIAgentManager(context);
        }
        return instance;
    }
    
    private AIAgentManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newCachedThreadPool();
        
        // Initialize native agent
        initializeNativeAgent();
    }
    
    // Native method declarations
    private native long nativeCreateAgent();
    private native void nativeDestroyAgent(long agentPtr);
    private native boolean nativeConfigureProvider(long agentPtr, String provider, String apiKey, String baseUrl);
    private native void nativeSetContentFilter(long agentPtr, ContentFilter filter);
    private native AIResponse nativeProcessRequest(long agentPtr, AIRequest request);
    private native boolean nativeTestConnection(long agentPtr);
    private native String[] nativeGetAvailableProviders(long agentPtr);
    private native boolean nativeIndexDocument(long agentPtr, String content, String title, String id);
    
    private void initializeNativeAgent() {
        try {
            nativeAgentPtr = nativeCreateAgent();
            if (nativeAgentPtr == 0) {
                Log.e(TAG, "Failed to create native AI agent");
            } else {
                Log.i(TAG, "Native AI agent initialized successfully");
                loadConfiguration();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing native AI agent", e);
        }
    }
    
    /**
     * Configure an AI provider
     */
    public void configureProvider(AIProvider provider, String apiKey, String baseUrl, 
                                 ConfigurationCallback callback) {
        executorService.execute(() -> {
            try {
                if (nativeAgentPtr == 0) {
                    callback.onConfigured(false, "AI agent not initialized");
                    return;
                }
                
                boolean success = nativeConfigureProvider(nativeAgentPtr, provider.getName(), 
                                                        apiKey, baseUrl != null ? baseUrl : "");
                
                if (success) {
                    // Save configuration
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("current_provider", provider.getName());
                    editor.putString(provider.getName() + "_api_key", apiKey);
                    if (baseUrl != null && !baseUrl.isEmpty()) {
                        editor.putString(provider.getName() + "_base_url", baseUrl);
                    }
                    editor.apply();
                    
                    callback.onConfigured(true, "Provider configured successfully");
                } else {
                    callback.onConfigured(false, "Failed to configure provider");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error configuring provider", e);
                callback.onConfigured(false, "Configuration error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Set content filtering configuration
     */
    public void setContentFilter(ContentFilter filter) {
        if (nativeAgentPtr != 0) {
            nativeSetContentFilter(nativeAgentPtr, filter);
            saveFilterConfiguration(filter);
        }
    }
    
    /**
     * Generate AI response asynchronously
     */
    public void generateResponse(AIRequest request, AIResponseCallback callback) {
        executorService.execute(() -> {
            try {
                if (nativeAgentPtr == 0) {
                    callback.onError("AI agent not initialized");
                    return;
                }
                
                AIResponse response = nativeProcessRequest(nativeAgentPtr, request);
                
                if (response.success) {
                    callback.onSuccess(response);
                } else {
                    callback.onError(response.error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating AI response", e);
                callback.onError("AI processing error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Generate text with simple prompt
     */
    public void generateText(String prompt, String context, AIResponseCallback callback) {
        AIRequest request = new AIRequest();
        request.prompt = prompt;
        request.context = context != null ? context : "";
        request.useCase = "text_generation";
        
        generateResponse(request, callback);
    }
    
    /**
     * Summarize content
     */
    public void summarizeContent(String content, AIResponseCallback callback) {
        AIRequest request = new AIRequest();
        request.prompt = "Please provide a concise summary of the following content:\n\n" + content;
        request.useCase = "summarization";
        request.maxTokens = 500;
        
        generateResponse(request, callback);
    }
    
    /**
     * Extract key points from content
     */
    public void extractKeyPoints(String content, AIResponseCallback callback) {
        AIRequest request = new AIRequest();
        request.prompt = "Extract the key points from the following content as a bulleted list:\n\n" + content;
        request.useCase = "key_extraction";
        request.maxTokens = 300;
        
        generateResponse(request, callback);
    }
    
    /**
     * Answer a question with optional context
     */
    public void answerQuestion(String question, String context, AIResponseCallback callback) {
        AIRequest request = new AIRequest();
        if (context != null && !context.isEmpty()) {
            request.prompt = String.format("Based on the following context, answer the question:\n\nContext: %s\n\nQuestion: %s", 
                                          context, question);
        } else {
            request.prompt = question;
        }
        request.useCase = "question_answering";
        
        generateResponse(request, callback);
    }
    
    /**
     * Index a document for RAG
     */
    public CompletableFuture<Boolean> indexDocument(String content, String title, String id) {
        return CompletableFuture.supplyAsync(() -> {
            if (nativeAgentPtr == 0) {
                return false;
            }
            
            try {
                return nativeIndexDocument(nativeAgentPtr, content, title, 
                                         id != null ? id : "doc_" + System.currentTimeMillis());
            } catch (Exception e) {
                Log.e(TAG, "Error indexing document", e);
                return false;
            }
        }, executorService);
    }
    
    /**
     * Test connection to current AI provider
     */
    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            if (nativeAgentPtr == 0) {
                return false;
            }
            
            try {
                return nativeTestConnection(nativeAgentPtr);
            } catch (Exception e) {
                Log.e(TAG, "Error testing connection", e);
                return false;
            }
        }, executorService);
    }
    
    /**
     * Get list of available AI providers
     */
    public List<String> getAvailableProviders() {
        if (nativeAgentPtr == 0) {
            return new ArrayList<>();
        }
        
        try {
            String[] providers = nativeGetAvailableProviders(nativeAgentPtr);
            List<String> result = new ArrayList<>();
            if (providers != null) {
                for (String provider : providers) {
                    result.add(provider);
                }
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error getting available providers", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if AI agent is properly configured
     */
    public boolean isConfigured() {
        String currentProvider = preferences.getString("current_provider", "");
        if (currentProvider.isEmpty()) {
            return false;
        }
        
        String apiKey = preferences.getString(currentProvider + "_api_key", "");
        return !apiKey.isEmpty();
    }
    
    /**
     * Get current provider name
     */
    public String getCurrentProvider() {
        return preferences.getString("current_provider", "");
    }
    
    private void loadConfiguration() {
        // Load current provider configuration
        String currentProvider = preferences.getString("current_provider", "");
        if (!currentProvider.isEmpty()) {
            String apiKey = preferences.getString(currentProvider + "_api_key", "");
            String baseUrl = preferences.getString(currentProvider + "_base_url", "");
            
            if (!apiKey.isEmpty()) {
                try {
                    AIProvider provider = AIProvider.valueOf(currentProvider.toUpperCase());
                    nativeConfigureProvider(nativeAgentPtr, provider.getName(), apiKey, baseUrl);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown provider in preferences: " + currentProvider);
                }
            }
        }
        
        // Load content filter configuration
        ContentFilter filter = loadFilterConfiguration();
        if (filter != null) {
            nativeSetContentFilter(nativeAgentPtr, filter);
        }
    }
    
    private ContentFilter loadFilterConfiguration() {
        ContentFilter filter = new ContentFilter();
        filter.filterLevel = preferences.getString("filter_level", "moderate");
        filter.enableRagFiltering = preferences.getBoolean("enable_rag_filtering", true);
        
        // Load topics and use cases (stored as comma-separated strings)
        String allowedTopics = preferences.getString("allowed_topics", "");
        if (!allowedTopics.isEmpty()) {
            String[] topics = allowedTopics.split(",");
            for (String topic : topics) {
                filter.allowedTopics.add(topic.trim());
            }
        }
        
        String blockedTopics = preferences.getString("blocked_topics", "");
        if (!blockedTopics.isEmpty()) {
            String[] topics = blockedTopics.split(",");
            for (String topic : topics) {
                filter.blockedTopics.add(topic.trim());
            }
        }
        
        String allowedUseCases = preferences.getString("allowed_use_cases", "");
        if (!allowedUseCases.isEmpty()) {
            String[] useCases = allowedUseCases.split(",");
            for (String useCase : useCases) {
                filter.allowedUseCases.add(useCase.trim());
            }
        }
        
        return filter;
    }
    
    private void saveFilterConfiguration(ContentFilter filter) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("filter_level", filter.filterLevel);
        editor.putBoolean("enable_rag_filtering", filter.enableRagFiltering);
        
        // Save topics and use cases as comma-separated strings
        editor.putString("allowed_topics", String.join(",", filter.allowedTopics));
        editor.putString("blocked_topics", String.join(",", filter.blockedTopics));
        editor.putString("allowed_use_cases", String.join(",", filter.allowedUseCases));
        
        editor.apply();
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        if (nativeAgentPtr != 0) {
            nativeDestroyAgent(nativeAgentPtr);
            nativeAgentPtr = 0;
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    // Load native library
    static {
        System.loadLibrary("main");
    }
}