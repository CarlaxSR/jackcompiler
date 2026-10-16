
// Arquivo: src/JackTokenizer.java
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JackTokenizer {
    private List<String> tokens;
    private int currentTokenIndex;
    private String currentToken;
    private TokenType currentTokenType;

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "class", "constructor", "function", "method", "field", "static", "var",
            "int", "char", "boolean", "void", "true", "false", "null", "this",
            "let", "do", "if", "else", "while", "return"));

    private static final Set<Character> SYMBOLS = new HashSet<>(Arrays.asList(
            '{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'));

    public JackTokenizer(File inputFile) throws IOException {
        this.tokens = new ArrayList<>();
        this.currentTokenIndex = -1;
        this.currentToken = null;
        this.currentTokenType = null;

        String content = Files.readString(inputFile.toPath());
        tokenize(content);
    }

    private void tokenize(String source) {
        String cleanSource = removeComments(source);
        int n = cleanSource.length();
        int i = 0;

        while (i < n) {
            char ch = cleanSource.charAt(i);

            // Ignora caracteres em branco
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            // Trata Símbolos
            if (SYMBOLS.contains(ch)) {
                tokens.add(String.valueOf(ch));
                i++;
                continue;
            }

            // Trata String Constants ("...")
            if (ch == '"') {
                StringBuilder sb = new StringBuilder();
                i++; // pula a aspa inicial
                while (i < n && cleanSource.charAt(i) != '"') {
                    sb.append(cleanSource.charAt(i));
                    i++;
                }
                i++; // pula a aspa final
                tokens.add("\"" + sb.toString() + "\"");
                continue;
            }

            // Trata Inteiros
            if (Character.isDigit(ch)) {
                StringBuilder sb = new StringBuilder();
                while (i < n && Character.isDigit(cleanSource.charAt(i))) {
                    sb.append(cleanSource.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                continue;
            }

            // Trata Identificadores e Palavras-chave
            if (Character.isLetter(ch) || ch == '_') {
                StringBuilder sb = new StringBuilder();
                while (i < n && (Character.isLetterOrDigit(cleanSource.charAt(i)) || cleanSource.charAt(i) == '_')) {
                    sb.append(cleanSource.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                continue;
            }

            i++;
        }
    }

    public boolean hasMoreTokens() {
        return currentTokenIndex + 1 < tokens.size();
    }

    public void advance() {
        if (hasMoreTokens()) {
            currentTokenIndex++;
            currentToken = tokens.get(currentTokenIndex);
            if (currentToken.startsWith("\"") && currentToken.endsWith("\"")) {
                currentTokenType = TokenType.STRING_CONST;
            } else if (SYMBOLS.contains(currentToken.charAt(0)) && currentToken.length() == 1) {
                currentTokenType = TokenType.SYMBOL;
            } else if (currentToken.matches("\\d+")) {
                currentTokenType = TokenType.INT_CONST;
            } else if (KEYWORDS.contains(currentToken)) {
                currentTokenType = TokenType.KEYWORD;
            } else {
                currentTokenType = TokenType.IDENTIFIER;
            }
        }
    }

    public TokenType tokenType() {
        return currentTokenType;
    }

    public String keyWord() {
        return currentToken;
    }

    public char symbol() {
        return currentToken.charAt(0);
    }

    public String identifier() {
        return currentToken;
    }

    public int intVal() {
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        return currentToken.substring(1, currentToken.length() - 1);
    }

    private String removeComments(String source) {
        StringBuilder clean = new StringBuilder();
        int n = source.length();
        int i = 0;

        while (i < n) {
            if (i + 1 < n && source.charAt(i) == '/' && source.charAt(i + 1) == '/') {
                i += 2;

                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            }

            else if (i + 1 < n && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                i += 2;

                while (i + 1 < n && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                    i++;
                }

                i += 2;
            }

            else if (source.charAt(i) == '"') {
                clean.append(source.charAt(i));
                i++;

                while (i < n && source.charAt(i) != '"') {
                    clean.append(source.charAt(i));
                    i++;
                }

                if (i < n) {
                    clean.append(source.charAt(i));
                    i++;
                }
            }

            else {
                clean.append(source.charAt(i));
                i++;
            }
        }
        return clean.toString();
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public String peekToken() {
        if (currentTokenIndex + 1 < tokens.size()) {
            return tokens.get(currentTokenIndex + 1);
        }
        return null;
    }

    public TokenType peekTokenType() {
        if (currentTokenIndex + 1 < tokens.size()) {
            String next = tokens.get(currentTokenIndex + 1);
            if (next.startsWith("\"") && next.endsWith("\""))
                return TokenType.STRING_CONST;
            if (SYMBOLS.contains(next.charAt(0)) && next.length() == 1)
                return TokenType.SYMBOL;
            if (next.matches("\\d+"))
                return TokenType.INT_CONST;
            if (KEYWORDS.contains(next))
                return TokenType.KEYWORD;
            return TokenType.IDENTIFIER;
        }
        return null;
    }

}