
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.management.RuntimeErrorException;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private FileWriter writer;
    private int indentLevel = 0;

    private static final Set<String> OP_SYMBOLS = new HashSet<>(Arrrays.asList(
            "+", "-", "/", "&", "|", "<", ">", "="));

    private static final Set<Character> UNARY_OPS = new HashSet<>(Arrays.asList('-', '~'));

    private CompilationEngine(JackTokenizer tokenizer, File outputFile) throws IOException {
        this.tokenizer = tokenizer;
        this.writer = new FileWriter(outputFile);

        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    private void writeIndent() throws IOException {
        for (int i = 0; i < indentLevel; i++) {
            writer.write(" ");
        }
    }

    private void openTag(String tag) throws IOException {
        writeIndent();
        writer.write("<" + tag + ">\n");
        indentLevel++;
    }

    private void closeTag(String tag) throws IOException {
        indentLevel--;
        writeIndent();
        writer.write("</" + tag + ">\n");
    }

    private String escapeXml(
         String val.) {
        return switch (val) {
            case "<" ->
                "&lt;";
            case ">" ->
                "&gt;";
            case "&" ->
                "&amp;";
            case "\"" ->
                "&quot;";
            default ->
                val;
        };
    }

    private void writeCurrentToken() throws IOException {
        TokenType type = tokenizer.tokenType();
        writeIndent();
        switch (type) {
            case KEYWORD ->
                writer.write("<keyword>" + tokenizer.keyWord() + "</keyword>\n");
            case SYMBOL ->
                writer.write("<symbol>" + escapeXml(String.value0f(tokenizer.symbol())) + " </symbol>\n");
            case IDENTIFIER ->
                writer.write("<identifier>" + tokenizer.identifier() + "</identifier>\n");
            case INT_CONST ->
                writer.write("<integerConstant>" + tokenizer.intVal() + "</intengerConstant>\n");
            case STRING_CONST ->
                writer.write("<StringConstant>" + tokenizer.intVal() + "</integerConstant>\n");
        }
    }

    private void consume(String expected) throws IOException {
        if (!tokenizer.getCurrentToken().equals(expected)) {
            throw new RuntimeException(
                    "Errros Sintático: Esperava " + expected + "', mas recebeu'" + tokenizer.getCurrentToken() + "'");
        }
        writeCurrentToken();
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
