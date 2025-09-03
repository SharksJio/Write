package com.jio.writingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import android.widget.AutoCompleteTextView;

import java.util.Arrays;
import java.util.List;

/**
 * AI Configuration Activity - allows users to configure AI providers and content filtering
 */
public class AIConfigActivity extends AppCompatActivity {
    
    private AIAgentManager aiAgent;
    
    // UI Components
    private MaterialToolbar toolbar;
    private AutoCompleteTextView providerSpinner;
    private TextInputEditText apiKeyInput;
    private TextInputEditText baseUrlInput;
    private TextInputLayout baseUrlLayout;
    private MaterialButton testConnectionButton;
    private MaterialButton saveButton;
    private CircularProgressIndicator loadingIndicator;
    
    // Content Filter Components
    private AutoCompleteTextView filterLevelSpinner;
    private SwitchMaterial enableRagSwitch;
    private TextInputEditText allowedTopicsInput;
    private TextInputEditText blockedTopicsInput;
    private TextInputEditText allowedUseCasesInput;
    private ChipGroup presetTopicsGroup;
    
    // Provider options
    private final String[] providers = {"OpenAI", "Anthropic", "Google Gemini", "Ollama", "Custom"};
    private final String[] filterLevels = {"Permissive", "Moderate", "Strict"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_config);
        
        initializeViews();
        setupProviderSelection();
        setupContentFilter();
        
        aiAgent = AIAgentManager.getInstance(this);
        loadCurrentConfiguration();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("AI Configuration");
        
        // Provider configuration
        providerSpinner = findViewById(R.id.provider_spinner);
        apiKeyInput = findViewById(R.id.api_key_input);
        baseUrlInput = findViewById(R.id.base_url_input);
        baseUrlLayout = findViewById(R.id.base_url_layout);
        testConnectionButton = findViewById(R.id.test_connection_button);
        saveButton = findViewById(R.id.save_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        
        // Content filter configuration
        filterLevelSpinner = findViewById(R.id.filter_level_spinner);
        enableRagSwitch = findViewById(R.id.enable_rag_switch);
        allowedTopicsInput = findViewById(R.id.allowed_topics_input);
        blockedTopicsInput = findViewById(R.id.blocked_topics_input);
        allowedUseCasesInput = findViewById(R.id.allowed_use_cases_input);
        presetTopicsGroup = findViewById(R.id.preset_topics_group);
    }
    
    private void setupProviderSelection() {
        // Setup provider dropdown
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, providers);
        providerSpinner.setAdapter(providerAdapter);
        
        // Setup filter level dropdown
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, filterLevels);
        filterLevelSpinner.setAdapter(filterAdapter);
        
        // Provider selection change listener
        providerSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvider = providers[position];
            updateUIForProvider(selectedProvider);
        });
        
        // Setup button listeners
        testConnectionButton.setOnClickListener(v -> testConnection());
        saveButton.setOnClickListener(v -> saveConfiguration());
    }
    
    private void setupContentFilter() {
        // Setup preset topic chips
        String[] presetTopics = {
            "Education", "Business", "Technology", "Science", "Health",
            "Creative Writing", "Research", "Personal Notes", "Meeting Notes"
        };
        
        for (String topic : presetTopics) {
            Chip chip = new Chip(this);
            chip.setText(topic);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateTopicsFromChips();
            });
            presetTopicsGroup.addView(chip);
        }
    }
    
    private void updateUIForProvider(String provider) {
        switch (provider) {
            case "OpenAI":
                baseUrlLayout.setVisibility(View.GONE);
                apiKeyInput.setHint("Enter your OpenAI API key");
                break;
            case "Anthropic":
                baseUrlLayout.setVisibility(View.GONE);
                apiKeyInput.setHint("Enter your Anthropic API key");
                break;
            case "Google Gemini":
                baseUrlLayout.setVisibility(View.GONE);
                apiKeyInput.setHint("Enter your Google API key");
                break;
            case "Ollama":
                baseUrlLayout.setVisibility(View.VISIBLE);
                baseUrlInput.setHint("http://localhost:11434");
                apiKeyInput.setHint("API key (leave empty for local)");
                break;
            case "Custom":
                baseUrlLayout.setVisibility(View.VISIBLE);
                baseUrlInput.setHint("Enter base URL");
                apiKeyInput.setHint("Enter API key");
                break;
        }
    }
    
    private void testConnection() {
        if (!validateInputs()) {
            return;
        }
        
        setLoading(true);
        
        // First save the configuration
        AIAgentManager.AIProvider provider = getSelectedProvider();
        String apiKey = apiKeyInput.getText().toString().trim();
        String baseUrl = baseUrlInput.getText().toString().trim();
        
        aiAgent.configureProvider(provider, apiKey, baseUrl, (success, message) -> {
            if (success) {
                // Test the connection
                aiAgent.testConnection().thenAccept(connectionSuccess -> {
                    runOnUiThread(() -> {
                        setLoading(false);
                        if (connectionSuccess) {
                            Toast.makeText(this, "Connection successful!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Connection failed. Please check your configuration.", 
                                         Toast.LENGTH_LONG).show();
                        }
                    });
                });
            } else {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Configuration error: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void saveConfiguration() {
        if (!validateInputs()) {
            return;
        }
        
        setLoading(true);
        
        // Save provider configuration
        AIAgentManager.AIProvider provider = getSelectedProvider();
        String apiKey = apiKeyInput.getText().toString().trim();
        String baseUrl = baseUrlInput.getText().toString().trim();
        
        aiAgent.configureProvider(provider, apiKey, baseUrl, (success, message) -> {
            if (success) {
                // Save content filter configuration
                AIAgentManager.ContentFilter filter = buildContentFilter();
                aiAgent.setContentFilter(filter);
                
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Configuration saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save configuration: " + message, 
                                 Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private boolean validateInputs() {
        String provider = providerSpinner.getText().toString();
        String apiKey = apiKeyInput.getText().toString().trim();
        
        if (provider.isEmpty()) {
            Toast.makeText(this, "Please select a provider", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (!provider.equals("Ollama") && apiKey.isEmpty()) {
            Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private AIAgentManager.AIProvider getSelectedProvider() {
        String selected = providerSpinner.getText().toString();
        switch (selected) {
            case "OpenAI": return AIAgentManager.AIProvider.OPENAI;
            case "Anthropic": return AIAgentManager.AIProvider.ANTHROPIC;
            case "Google Gemini": return AIAgentManager.AIProvider.GOOGLE_GEMINI;
            case "Ollama": return AIAgentManager.AIProvider.OLLAMA;
            case "Custom": return AIAgentManager.AIProvider.CUSTOM;
            default: return AIAgentManager.AIProvider.OPENAI;
        }
    }
    
    private AIAgentManager.ContentFilter buildContentFilter() {
        AIAgentManager.ContentFilter filter = new AIAgentManager.ContentFilter();
        
        String filterLevel = filterLevelSpinner.getText().toString().toLowerCase();
        filter.filterLevel = filterLevel;
        filter.enableRagFiltering = enableRagSwitch.isChecked();
        
        // Parse topics and use cases
        String allowedTopics = allowedTopicsInput.getText().toString().trim();
        if (!allowedTopics.isEmpty()) {
            filter.allowedTopics = Arrays.asList(allowedTopics.split(","));
        }
        
        String blockedTopics = blockedTopicsInput.getText().toString().trim();
        if (!blockedTopics.isEmpty()) {
            filter.blockedTopics = Arrays.asList(blockedTopics.split(","));
        }
        
        String allowedUseCases = allowedUseCasesInput.getText().toString().trim();
        if (!allowedUseCases.isEmpty()) {
            filter.allowedUseCases = Arrays.asList(allowedUseCases.split(","));
        }
        
        return filter;
    }
    
    private void updateTopicsFromChips() {
        StringBuilder allowedTopics = new StringBuilder();
        
        for (int i = 0; i < presetTopicsGroup.getChildCount(); i++) {
            View child = presetTopicsGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    if (allowedTopics.length() > 0) {
                        allowedTopics.append(", ");
                    }
                    allowedTopics.append(chip.getText().toString());
                }
            }
        }
        
        allowedTopicsInput.setText(allowedTopics.toString());
    }
    
    private void loadCurrentConfiguration() {
        if (aiAgent.isConfigured()) {
            String currentProvider = aiAgent.getCurrentProvider();
            providerSpinner.setText(currentProvider, false);
            updateUIForProvider(currentProvider);
        }
        
        // Set default values
        filterLevelSpinner.setText("Moderate", false);
        enableRagSwitch.setChecked(true);
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        testConnectionButton.setEnabled(!loading);
        saveButton.setEnabled(!loading);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}