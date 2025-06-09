package ai.headkey.rest.service;

import ai.headkey.memory.langchain4j.services.LangChain4jCategoryExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class QuarkusCategoryExtractionService extends LangChain4jCategoryExtractionService {

    @Inject
    public QuarkusCategoryExtractionService(ChatModel chatModel, QuarkusCategoryAiService aiService) {
        super(chatModel, aiService, "QuarkusCategoryExtractionService");
    }
    
}
