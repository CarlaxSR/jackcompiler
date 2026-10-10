// Arquivo: src/JackTokenizer.java
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JackTokenizer {
    private List<String> tokens;
    private int currentTokenIndex;
    private String currentToken;
    private TokenType currentTokenType;

    public JackTokenizer(File inputFile) throws IOException {
        this.tokens = new ArrayList<>();
        this.currentTokenIndex = -1;
        this.currentToken = null;
        this.currentTokenType = null;

        String content = Files.readString(inputFile.toPath());
        tokenize(content);
    }

    private void tokenize(String source) {
        // Implementação nas próximas etapas
    }

    public boolean hasMoreTokens() {
        return currentTokenIndex + 1 < tokens.size();
    }

    public void advance() {
        if (hasMoreTokens()) {
            currentTokenIndex++;
            currentToken = tokens.get(currentTokenIndex);
        }
    }
}