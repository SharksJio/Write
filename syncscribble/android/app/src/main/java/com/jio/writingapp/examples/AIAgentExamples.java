package com.jio.writingapp.examples;

import android.content.Context;
import android.util.Log;
import com.jio.writingapp.AIAgentManager;

/**
 * Example usage of the AI Agent for common writing tasks
 */
public class AIAgentExamples {
    private static final String TAG = "AIAgentExamples";
    private AIAgentManager aiAgent;
    
    public AIAgentExamples(Context context) {
        this.aiAgent = AIAgentManager.getInstance(context);
    }
    
    /**
     * Example 1: Basic AI Configuration
     */
    public void configureAIProvider() {
        // Configure OpenAI (replace with your actual API key)
        aiAgent.configureProvider(
            AIAgentManager.AIProvider.OPENAI, 
            "sk-your-openai-api-key-here", 
            null, // Use default base URL
            new AIAgentManager.ConfigurationCallback() {
                @Override
                public void onConfigured(boolean success, String message) {
                    if (success) {
                        Log.i(TAG, "AI Provider configured successfully");
                        setupContentFilter();
                    } else {
                        Log.e(TAG, "Failed to configure AI: " + message);
                    }
                }
            }
        );
    }
    
    /**
     * Example 2: Content Filter Setup
     */
    private void setupContentFilter() {
        AIAgentManager.ContentFilter filter = new AIAgentManager.ContentFilter();
        
        // Set filter level
        filter.filterLevel = "moderate"; // strict, moderate, or permissive
        
        // Allow specific topics
        filter.allowedTopics.add("education");
        filter.allowedTopics.add("technology");
        filter.allowedTopics.add("business");
        filter.allowedTopics.add("science");
        
        // Block harmful topics
        filter.blockedTopics.add("violence");
        filter.blockedTopics.add("hate");
        
        // Allow specific use cases
        filter.allowedUseCases.add("summarization");
        filter.allowedUseCases.add("question_answering");
        filter.allowedUseCases.add("text_generation");
        filter.allowedUseCases.add("key_extraction");
        
        // Enable RAG filtering
        filter.enableRagFiltering = true;
        
        aiAgent.setContentFilter(filter);
        Log.i(TAG, "Content filter configured");
    }
    
    /**
     * Example 3: Summarize Meeting Notes
     */
    public void summarizeMeetingNotes(String meetingNotes) {
        Log.i(TAG, "Summarizing meeting notes...");
        
        aiAgent.summarizeContent(meetingNotes, new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                Log.i(TAG, "Meeting summary generated:");
                Log.i(TAG, response.content);
                
                // You would typically update your UI here
                displaySummary(response.content);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to summarize meeting: " + error);
                showError("Could not generate summary: " + error);
            }
        });
    }
    
    /**
     * Example 4: Extract Action Items
     */
    public void extractActionItems(String meetingNotes) {
        String prompt = "Extract action items and tasks from the following meeting notes. " +
                       "Format as a bulleted list with responsible person and deadline if mentioned:\n\n" + 
                       meetingNotes;
        
        aiAgent.generateText(prompt, "", new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                Log.i(TAG, "Action items extracted:");
                Log.i(TAG, response.content);
                displayActionItems(response.content);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to extract action items: " + error);
                showError("Could not extract action items: " + error);
            }
        });
    }
    
    /**
     * Example 5: Generate Writing Ideas
     */
    public void generateWritingIdeas(String topic) {
        String prompt = "Generate 5 creative writing ideas about: " + topic + 
                       ". Provide brief descriptions for each idea.";
        
        AIAgentManager.AIRequest request = new AIAgentManager.AIRequest();
        request.prompt = prompt;
        request.useCase = "text_generation";
        request.maxTokens = 400;
        request.temperature = 0.8f; // Higher creativity
        
        aiAgent.generateResponse(request, new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                Log.i(TAG, "Writing ideas generated:");
                Log.i(TAG, response.content);
                displayWritingIdeas(response.content);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to generate ideas: " + error);
                showError("Could not generate writing ideas: " + error);
            }
        });
    }
    
    /**
     * Example 6: Answer Questions Based on Documents
     */
    public void answerQuestionWithContext(String question, String documentContent) {
        // First, index the document for RAG
        String documentId = "doc_" + System.currentTimeMillis();
        
        aiAgent.indexDocument(documentContent, "Context Document", documentId)
            .thenAccept(indexed -> {
                if (indexed) {
                    Log.i(TAG, "Document indexed for RAG");
                    
                    // Now ask the question
                    aiAgent.answerQuestion(question, documentContent, new AIAgentManager.AIResponseCallback() {
                        @Override
                        public void onSuccess(AIAgentManager.AIResponse response) {
                            Log.i(TAG, "Question answered:");
                            Log.i(TAG, response.content);
                            displayAnswer(question, response.content);
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to answer question: " + error);
                            showError("Could not answer question: " + error);
                        }
                    });
                } else {
                    Log.w(TAG, "Failed to index document, answering without RAG");
                    aiAgent.answerQuestion(question, documentContent, new AIAgentManager.AIResponseCallback() {
                        @Override
                        public void onSuccess(AIAgentManager.AIResponse response) {
                            displayAnswer(question, response.content);
                        }
                        
                        @Override
                        public void onError(String error) {
                            showError("Could not answer question: " + error);
                        }
                    });
                }
            });
    }
    
    /**
     * Example 7: Improve Writing Style
     */
    public void improveWritingStyle(String originalText) {
        String prompt = "Improve the writing style, clarity, and flow of the following text " +
                       "while maintaining its original meaning and tone:\n\n" + originalText;
        
        aiAgent.generateText(prompt, "", new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                Log.i(TAG, "Writing improved:");
                Log.i(TAG, "Original: " + originalText);
                Log.i(TAG, "Improved: " + response.content);
                displayImprovedText(originalText, response.content);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to improve writing: " + error);
                showError("Could not improve text: " + error);
            }
        });
    }
    
    /**
     * Example 8: Test AI Connection
     */
    public void testConnection() {
        Log.i(TAG, "Testing AI connection...");
        
        aiAgent.testConnection()
            .thenAccept(success -> {
                if (success) {
                    Log.i(TAG, "AI connection test successful");
                    showSuccess("AI connection is working properly");
                } else {
                    Log.w(TAG, "AI connection test failed");
                    showError("AI connection test failed. Check your configuration.");
                }
            });
    }
    
    /**
     * Example 9: Batch Document Processing
     */
    public void batchProcessDocuments(String[] documents, String[] titles) {
        Log.i(TAG, "Processing " + documents.length + " documents...");
        
        for (int i = 0; i < documents.length; i++) {
            final int index = i;
            final String content = documents[i];
            final String title = titles[i];
            
            // Index document for RAG
            aiAgent.indexDocument(content, title, "doc_" + i)
                .thenAccept(indexed -> {
                    if (indexed) {
                        Log.i(TAG, "Indexed document: " + title);
                    }
                });
            
            // Generate summary
            aiAgent.summarizeContent(content, new AIAgentManager.AIResponseCallback() {
                @Override
                public void onSuccess(AIAgentManager.AIResponse response) {
                    Log.i(TAG, "Summary for " + title + ":");
                    Log.i(TAG, response.content);
                    saveSummary(title, response.content);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to summarize " + title + ": " + error);
                }
            });
        }
    }
    
    // UI update methods (implement these based on your UI framework)
    private void displaySummary(String summary) {
        Log.i(TAG, "Display summary: " + summary);
        // Update UI with summary
    }
    
    private void displayActionItems(String actionItems) {
        Log.i(TAG, "Display action items: " + actionItems);
        // Update UI with action items
    }
    
    private void displayWritingIdeas(String ideas) {
        Log.i(TAG, "Display writing ideas: " + ideas);
        // Update UI with writing ideas
    }
    
    private void displayAnswer(String question, String answer) {
        Log.i(TAG, "Q: " + question);
        Log.i(TAG, "A: " + answer);
        // Update UI with Q&A
    }
    
    private void displayImprovedText(String original, String improved) {
        Log.i(TAG, "Display text comparison");
        // Show before/after text comparison
    }
    
    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        // Show error message to user
    }
    
    private void showSuccess(String message) {
        Log.i(TAG, "Success: " + message);
        // Show success message to user
    }
    
    private void saveSummary(String title, String summary) {
        Log.i(TAG, "Saving summary for: " + title);
        // Save summary to storage
    }
}