package at.donmatzos.opsdesk.ai.llm;

import java.util.Objects;

public record LlmAnswer(String text, String model, boolean mock) {

    public LlmAnswer {
        Objects.requireNonNull(text);
        Objects.requireNonNull(model);
    }
}
