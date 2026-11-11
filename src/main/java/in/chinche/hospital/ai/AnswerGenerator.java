package in.chinche.hospital.ai;

import java.util.Optional;

public interface AnswerGenerator {

    Optional<String> generate(String question, String language, String retrievedContext);
}
