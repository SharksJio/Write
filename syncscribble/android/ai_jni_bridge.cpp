#include "aiagent.h"
#include "android/androidhelper.h"
#include <jni.h>
#include <memory>
#include <map>

// Global AI agent instances (managed by Java)
static std::map<jlong, std::unique_ptr<AIAgent>> g_aiAgents;
static jlong g_nextAgentId = 1;

// JNI Helper functions
jstring createJString(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

std::string getJString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

// Convert Java ContentFilter to C++ ContentFilter
ContentFilter convertContentFilter(JNIEnv* env, jobject jfilter) {
    ContentFilter filter;
    
    jclass filterClass = env->GetObjectClass(jfilter);
    
    // Get filter level
    jfieldID filterLevelField = env->GetFieldID(filterClass, "filterLevel", "Ljava/lang/String;");
    jstring jFilterLevel = (jstring) env->GetObjectField(jfilter, filterLevelField);
    filter.filterLevel = getJString(env, jFilterLevel);
    
    // Get enable RAG filtering
    jfieldID enableRagField = env->GetFieldID(filterClass, "enableRagFiltering", "Z");
    filter.enableRagFiltering = env->GetBooleanField(jfilter, enableRagField);
    
    // Get allowed topics list
    jfieldID allowedTopicsField = env->GetFieldID(filterClass, "allowedTopics", "Ljava/util/List;");
    jobject jAllowedTopics = env->GetObjectField(jfilter, allowedTopicsField);
    if (jAllowedTopics) {
        jclass listClass = env->GetObjectClass(jAllowedTopics);
        jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        
        int size = env->CallIntMethod(jAllowedTopics, sizeMethod);
        for (int i = 0; i < size; i++) {
            jstring jTopic = (jstring) env->CallObjectMethod(jAllowedTopics, getMethod, i);
            filter.allowedTopics.push_back(getJString(env, jTopic));
        }
    }
    
    // Get blocked topics list
    jfieldID blockedTopicsField = env->GetFieldID(filterClass, "blockedTopics", "Ljava/util/List;");
    jobject jBlockedTopics = env->GetObjectField(jfilter, blockedTopicsField);
    if (jBlockedTopics) {
        jclass listClass = env->GetObjectClass(jBlockedTopics);
        jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        
        int size = env->CallIntMethod(jBlockedTopics, sizeMethod);
        for (int i = 0; i < size; i++) {
            jstring jTopic = (jstring) env->CallObjectMethod(jBlockedTopics, getMethod, i);
            filter.blockedTopics.push_back(getJString(env, jTopic));
        }
    }
    
    // Get allowed use cases list
    jfieldID allowedUseCasesField = env->GetFieldID(filterClass, "allowedUseCases", "Ljava/util/List;");
    jobject jAllowedUseCases = env->GetObjectField(jfilter, allowedUseCasesField);
    if (jAllowedUseCases) {
        jclass listClass = env->GetObjectClass(jAllowedUseCases);
        jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
        jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        
        int size = env->CallIntMethod(jAllowedUseCases, sizeMethod);
        for (int i = 0; i < size; i++) {
            jstring jUseCase = (jstring) env->CallObjectMethod(jAllowedUseCases, getMethod, i);
            filter.allowedUseCases.push_back(getJString(env, jUseCase));
        }
    }
    
    return filter;
}

// Convert Java AIRequest to C++ AIRequest
AIRequest convertAIRequest(JNIEnv* env, jobject jrequest) {
    AIRequest request;
    
    jclass requestClass = env->GetObjectClass(jrequest);
    
    // Get prompt
    jfieldID promptField = env->GetFieldID(requestClass, "prompt", "Ljava/lang/String;");
    jstring jPrompt = (jstring) env->GetObjectField(jrequest, promptField);
    request.prompt = getJString(env, jPrompt);
    
    // Get context
    jfieldID contextField = env->GetFieldID(requestClass, "context", "Ljava/lang/String;");
    jstring jContext = (jstring) env->GetObjectField(jrequest, contextField);
    request.context = getJString(env, jContext);
    
    // Get max tokens
    jfieldID maxTokensField = env->GetFieldID(requestClass, "maxTokens", "I");
    request.maxTokens = env->GetIntField(jrequest, maxTokensField);
    
    // Get temperature
    jfieldID temperatureField = env->GetFieldID(requestClass, "temperature", "F");
    request.temperature = env->GetFloatField(jrequest, temperatureField);
    
    // Get use case
    jfieldID useCaseField = env->GetFieldID(requestClass, "useCase", "Ljava/lang/String;");
    jstring jUseCase = (jstring) env->GetObjectField(jrequest, useCaseField);
    request.metadata["useCase"] = getJString(env, jUseCase);
    
    return request;
}

// Convert C++ AIResponse to Java AIResponse
jobject convertAIResponse(JNIEnv* env, const AIResponse& response) {
    // Find AIResponse class
    jclass responseClass = env->FindClass("com/jio/writingapp/AIAgentManager$AIResponse");
    if (!responseClass) {
        return nullptr;
    }
    
    // Create new instance
    jmethodID constructor = env->GetMethodID(responseClass, "<init>", "()V");
    jobject jresponse = env->NewObject(responseClass, constructor);
    
    // Set fields
    jfieldID contentField = env->GetFieldID(responseClass, "content", "Ljava/lang/String;");
    env->SetObjectField(jresponse, contentField, createJString(env, response.content));
    
    jfieldID filteredReasonField = env->GetFieldID(responseClass, "filteredReason", "Ljava/lang/String;");
    env->SetObjectField(jresponse, filteredReasonField, createJString(env, response.filteredReason));
    
    jfieldID successField = env->GetFieldID(responseClass, "success", "Z");
    env->SetBooleanField(jresponse, successField, response.success);
    
    jfieldID errorField = env->GetFieldID(responseClass, "error", "Ljava/lang/String;");
    env->SetObjectField(jresponse, errorField, createJString(env, response.error));
    
    jfieldID confidenceField = env->GetFieldID(responseClass, "confidence", "F");
    env->SetFloatField(jresponse, confidenceField, response.confidence);
    
    return jresponse;
}

// Convert provider string to enum
AIProvider getProviderFromString(const std::string& providerStr) {
    if (providerStr == "openai") return AIProvider::OPENAI;
    if (providerStr == "anthropic") return AIProvider::ANTHROPIC;
    if (providerStr == "google") return AIProvider::GOOGLE_GEMINI;
    if (providerStr == "ollama") return AIProvider::OLLAMA;
    if (providerStr == "custom") return AIProvider::CUSTOM;
    return AIProvider::OPENAI;
}

extern "C" {

// Create AI Agent
JNIEXPORT jlong JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeCreateAgent(JNIEnv* env, jobject thiz) {
    try {
        // Get ScribbleApp config (assuming it's available)
        ScribbleConfig* config = ScribbleApp::cfg;
        if (!config) {
            return 0; // Failed to get config
        }
        
        auto agent = std::make_unique<AIAgent>(config);
        jlong agentId = g_nextAgentId++;
        g_aiAgents[agentId] = std::move(agent);
        return agentId;
    } catch (const std::exception& e) {
        return 0; // Failed to create agent
    }
}

// Destroy AI Agent
JNIEXPORT void JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeDestroyAgent(JNIEnv* env, jobject thiz, jlong agentPtr) {
    auto it = g_aiAgents.find(agentPtr);
    if (it != g_aiAgents.end()) {
        g_aiAgents.erase(it);
    }
}

// Configure Provider
JNIEXPORT jboolean JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeConfigureProvider(JNIEnv* env, jobject thiz, 
                                                              jlong agentPtr, jstring jprovider, 
                                                              jstring japiKey, jstring jbaseUrl) {
    auto it = g_aiAgents.find(agentPtr);
    if (it == g_aiAgents.end()) {
        return JNI_FALSE;
    }
    
    try {
        std::string provider = getJString(env, jprovider);
        std::string apiKey = getJString(env, japiKey);
        std::string baseUrl = getJString(env, jbaseUrl);
        
        AIProvider providerEnum = getProviderFromString(provider);
        it->second->configure(providerEnum, apiKey, baseUrl);
        
        return JNI_TRUE;
    } catch (const std::exception& e) {
        return JNI_FALSE;
    }
}

// Set Content Filter
JNIEXPORT void JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeSetContentFilter(JNIEnv* env, jobject thiz, 
                                                             jlong agentPtr, jobject jfilter) {
    auto it = g_aiAgents.find(agentPtr);
    if (it == g_aiAgents.end()) {
        return;
    }
    
    try {
        ContentFilter filter = convertContentFilter(env, jfilter);
        it->second->setContentFilter(filter);
    } catch (const std::exception& e) {
        // Error setting content filter
    }
}

// Process AI Request
JNIEXPORT jobject JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeProcessRequest(JNIEnv* env, jobject thiz, 
                                                           jlong agentPtr, jobject jrequest) {
    auto it = g_aiAgents.find(agentPtr);
    if (it == g_aiAgents.end()) {
        // Return error response
        AIResponse errorResponse;
        errorResponse.success = false;
        errorResponse.error = "AI agent not found";
        return convertAIResponse(env, errorResponse);
    }
    
    try {
        AIRequest request = convertAIRequest(env, jrequest);
        AIResponse response = it->second->processRequest(request);
        return convertAIResponse(env, response);
    } catch (const std::exception& e) {
        AIResponse errorResponse;
        errorResponse.success = false;
        errorResponse.error = fstring("Processing error: %s", e.what());
        return convertAIResponse(env, errorResponse);
    }
}

// Test Connection
JNIEXPORT jboolean JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeTestConnection(JNIEnv* env, jobject thiz, jlong agentPtr) {
    auto it = g_aiAgents.find(agentPtr);
    if (it == g_aiAgents.end()) {
        return JNI_FALSE;
    }
    
    try {
        return it->second->testConnection() ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        return JNI_FALSE;
    }
}

// Get Available Providers
JNIEXPORT jobjectArray JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeGetAvailableProviders(JNIEnv* env, jobject thiz, jlong agentPtr) {
    auto it = g_aiAgents.find(agentPtr);
    if (it == g_aiAgents.end()) {
        return nullptr;
    }
    
    try {
        auto providers = it->second->getAvailableProviders();
        
        jclass stringClass = env->FindClass("java/lang/String");
        jobjectArray result = env->NewObjectArray(providers.size(), stringClass, nullptr);
        
        for (size_t i = 0; i < providers.size(); i++) {
            env->SetObjectArrayElement(result, i, createJString(env, providers[i]));
        }
        
        return result;
    } catch (const std::exception& e) {
        return nullptr;
    }
}

// Index Document
JNIEXPORT jboolean JNICALL
Java_com_jio_writingapp_AIAgentManager_nativeIndexDocument(JNIEnv* env, jobject thiz, 
                                                          jlong agentPtr, jstring jcontent, 
                                                          jstring jtitle, jstring jid) {
    auto it = g_aiAgents.find(agentPtr);
    if (it == g_aiAgents.end()) {
        return JNI_FALSE;
    }
    
    try {
        std::string content = getJString(env, jcontent);
        std::string title = getJString(env, jtitle);
        std::string id = getJString(env, jid);
        
        return it->second->indexDocument(content, title, id) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        return JNI_FALSE;
    }
}

} // extern "C"