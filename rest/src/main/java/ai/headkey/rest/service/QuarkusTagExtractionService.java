package ai.headkey.rest.service;

import ai.headkey.memory.langchain4j.services.LangChain4jTagExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class QuarkusTagExtractionService extends LangChain4jTagExtractionService {

    @Inject
    public QuarkusTagExtractionService(ChatModel chatModel, QuarkusTagAiService aiService) {
        super(chatModel, aiService, "QuarkusTagExtractionService");
    }
    
}
