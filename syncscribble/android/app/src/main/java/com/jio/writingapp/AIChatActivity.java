package com.jio.writingapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Chat Activity - provides an interface for interacting with AI agents
 */
public class AIChatActivity extends AppCompatActivity {
    
    private AIAgentManager aiAgent;
    private AIChatAdapter chatAdapter;
    private RecyclerView chatRecyclerView;
    private TextInputEditText messageInput;
    private TextInputLayout messageInputLayout;
    private FloatingActionButton sendButton;
    private CircularProgressIndicator loadingIndicator;
    private ChipGroup actionChipsGroup;
    private MaterialToolbar toolbar;
    
    private List<AIChatMessage> chatMessages = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        
        initializeViews();
        setupChat();
        setupActionChips();
        
        aiAgent = AIAgentManager.getInstance(this);
        
        // Check if AI agent is configured
        if (!aiAgent.isConfigured()) {
            showConfigurationNeeded();
        }
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("AI Assistant");
        
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        messageInputLayout = findViewById(R.id.message_input_layout);
        sendButton = findViewById(R.id.send_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        actionChipsGroup = findViewById(R.id.action_chips_group);
    }
    
    private void setupChat() {
        chatAdapter = new AIChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        
        // Setup message input
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                sendButton.setEnabled(!s.toString().trim().isEmpty());
            }
        });
        
        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());
        sendButton.setEnabled(false);
        
        // Add welcome message
        addMessage("Hi! I'm your AI writing assistant. I can help you with:", false, true);
        addMessage("• Summarize your notes\n• Extract key points\n• Answer questions about your content\n• Generate new text\n• And much more!", false, false);
    }
    
    private void setupActionChips() {
        // Quick action chips for common tasks
        addActionChip("Summarize Selection", () -> {
            if (hasSelectedText()) {
                String selectedText = getSelectedText();
                summarizeText(selectedText);
            } else {
                Toast.makeText(this, "Please select some text first", Toast.LENGTH_SHORT).show();
            }
        });
        
        addActionChip("Extract Key Points", () -> {
            if (hasSelectedText()) {
                String selectedText = getSelectedText();
                extractKeyPoints(selectedText);
            } else {
                Toast.makeText(this, "Please select some text first", Toast.LENGTH_SHORT).show();
            }
        });
        
        addActionChip("Ask Question", () -> {
            messageInput.setText("Question: ");
            messageInput.setSelection(messageInput.getText().length());
            messageInput.requestFocus();
        });
        
        addActionChip("Generate Ideas", () -> {
            messageInput.setText("Generate ideas about: ");
            messageInput.setSelection(messageInput.getText().length());
            messageInput.requestFocus();
        });
    }
    
    private void addActionChip(String text, Runnable action) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setOnClickListener(v -> action.run());
        actionChipsGroup.addView(chip);
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;
        
        // Add user message to chat
        addMessage(message, true, false);
        
        // Clear input
        messageInput.setText("");
        
        // Show loading
        setLoading(true);
        
        // Send to AI
        aiAgent.generateText(message, "", new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addMessage(response.content, false, false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addMessage("Sorry, I encountered an error: " + error, false, true);
                });
            }
        });
    }
    
    private void summarizeText(String text) {
        setLoading(true);
        addMessage("Summarize: " + text.substring(0, Math.min(100, text.length())) + 
                  (text.length() > 100 ? "..." : ""), true, false);
        
        aiAgent.summarizeContent(text, new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addMessage("Summary:\n" + response.content, false, false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addMessage("Error creating summary: " + error, false, true);
                });
            }
        });
    }
    
    private void extractKeyPoints(String text) {
        setLoading(true);
        addMessage("Extract key points from: " + text.substring(0, Math.min(100, text.length())) + 
                  (text.length() > 100 ? "..." : ""), true, false);
        
        aiAgent.extractKeyPoints(text, new AIAgentManager.AIResponseCallback() {
            @Override
            public void onSuccess(AIAgentManager.AIResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addMessage("Key Points:\n" + response.content, false, false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    addMessage("Error extracting key points: " + error, false, true);
                });
            }
        });
    }
    
    private void addMessage(String content, boolean isUser, boolean isSystem) {
        AIChatMessage message = new AIChatMessage(content, isUser, isSystem, System.currentTimeMillis());
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading && !messageInput.getText().toString().trim().isEmpty());
        messageInput.setEnabled(!loading);
    }
    
    private void showConfigurationNeeded() {
        addMessage("⚠️ AI Assistant needs to be configured first.", false, true);
        addMessage("Please go to Settings > AI Configuration to set up your AI provider.", false, true);
        
        // Disable input
        messageInput.setEnabled(false);
        sendButton.setEnabled(false);
        messageInputLayout.setHelperText("Configure AI provider in settings first");
    }
    
    // These methods would integrate with the main Write app's text selection
    private boolean hasSelectedText() {
        // TODO: Integrate with main app's text selection
        return false;
    }
    
    private String getSelectedText() {
        // TODO: Get selected text from main app
        return "";
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    // Data class for chat messages
    public static class AIChatMessage {
        public String content;
        public boolean isUser;
        public boolean isSystem;
        public long timestamp;
        
        public AIChatMessage(String content, boolean isUser, boolean isSystem, long timestamp) {
            this.content = content;
            this.isUser = isUser;
            this.isSystem = isSystem;
            this.timestamp = timestamp;
        }
    }
}