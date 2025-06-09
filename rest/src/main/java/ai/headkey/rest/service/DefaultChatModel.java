package ai.headkey.rest.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class DefaultChatModel {

    private final ChatModel chatModel;

    public DefaultChatModel() {
        this.chatModel = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o-mini")
            .temperature(0.3)
            .build();
    }

    public ChatModel getChatModel() {
        return chatModel;
    }
    
}
