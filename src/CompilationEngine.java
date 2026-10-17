import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CompilationEngine 
{
    private JackTokenizer tokenizer;
    private FileWriter writer;
    private int indentLevel = 0;

    private static final Set<String> OP_SYMBOLS = new HashSet<>(Arrays.asList(
            "+", "-", "/", "&", "|", "<", ">", "="));

    private static final Set<Character> UNARY_OPS = new HashSet<>(Arrays.asList('-', '~'));

    private CompilationEngine(JackTokenizer tokenizer, File outputFile) throws IOException 
    {
        this.tokenizer = tokenizer;
        this.writer = new FileWriter(outputFile);

        if (tokenizer.hasMoreTokens()) 
        {
            tokenizer.advance();
        }
    }

    private void writeIndent() throws IOException 
    {
        for (int i = 0; i < indentLevel; i++) 
        {
            writer.write(" ");
        }
    }

    private void openTag(String tag) throws IOException 
    {
        writeIndent();
        writer.write("<" + tag + ">\n");
        indentLevel++;
    }

    private void closeTag(String tag) throws IOException 
    {
        indentLevel--;
        writeIndent();
        writer.write("</" + tag + ">\n");
    }

    private String escapeXml(String val) 
    {
        return switch (val) 
        {
            case "<" -> "&lt;";
            case ">" -> "&gt;";
            case "&" -> "&amp;";
            case "\"" -> "&quot;";
            default -> val;
        };
    }

    private void writeCurrentToken() throws IOException 
    {
        TokenType type = tokenizer.tokenType();
        writeIndent();
        switch (type) 
        {
            case KEYWORD -> writer.write("<keyword>" + tokenizer.keyWord() + "</keyword>\n");
            case SYMBOL -> writer.write("<symbol>" + escapeXml(String.valueOf(tokenizer.symbol())) + " </symbol>\n");
            case IDENTIFIER -> writer.write("<identifier>" + tokenizer.identifier() + "</identifier>\n");
            case INT_CONST -> writer.write("<integerConstant>" + tokenizer.intVal() + "</intengerConstant>\n");
            case STRING_CONST -> writer.write("<StringConstant>" + tokenizer.intVal() + "</integerConstant>\n");
        }
    }

    private void consume(String expected) throws IOException 
    {
        if (!tokenizer.getCurrentToken().equals(expected)) 
        {
            throw new RuntimeException(
                    "Errros Sintático: Esperava " + expected + "', mas recebeu'" + tokenizer.getCurrentToken() + "'");
        }
        writeCurrentToken();
        if (tokenizer.hasMoreTokens()) 
        {
            tokenizer.advance();
        }
    }

    public void close() throws IOException 
    {
        if (writer != null) 
        {
            writer.close();
        }
    }

    public void compileClass() throws IOException 
    {
        openTag("class");
        consume("class");

        writeCurrentToken(); 
        tokenizer.advance();

        consume("{");

        while (tokenizer.tokenType() == TokenType.KEYWORD && 
            (tokenizer.keyWord().equals("static") || tokenizer.keyWord().equals("field"))) 
            {
            compileClassVarDec();
            }

        while (tokenizer.tokenType() == TokenType.KEYWORD && 
            (tokenizer.keyWord().equals("constructor") || tokenizer.keyWord().equals("function") || tokenizer.keyWord().equals("method"))) 
            {
            compileSubroutine();
            }

        consume("}");
        closeTag("class");
        close();
    }

    public void compileClassVarDec() throws IOException 
    {
        openTag("classVarDec");
        writeCurrentToken(); 
        tokenizer.advance();

        writeCurrentToken(); 
        tokenizer.advance();

        writeCurrentToken(); 
        tokenizer.advance();

        while (tokenizer.getCurrentToken().equals(",")) 
        {
            consume(",");
            writeCurrentToken(); 
            tokenizer.advance();
        }

        consume(";");
        closeTag("classVarDec");
    }

    public void compileSubroutine() throws IOException 
    {
        openTag("subroutineDec");
        writeCurrentToken(); 
        tokenizer.advance();

        writeCurrentToken(); 
        tokenizer.advance();

        writeCurrentToken(); 
        tokenizer.advance();

        consume("(");
        compileParameterList();
        consume(")");

        openTag("subroutineBody");
        consume("{");

        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord().equals("var")) 
        {
            compileVarDec();
        }

        compileStatements(); 

        consume("}");
        closeTag("subroutineBody");
        closeTag("subroutineDec");
    }

    public void compileParameterList() throws IOException 
    {
        openTag("parameterList");
        if (!tokenizer.getCurrentToken().equals(")")) 
        {
            writeCurrentToken(); 
            tokenizer.advance();

            writeCurrentToken(); 
            tokenizer.advance();

            while (tokenizer.getCurrentToken().equals(",")) 
            {
                consume(",");
                writeCurrentToken(); 
                tokenizer.advance();
                writeCurrentToken(); 
                tokenizer.advance();
            }
        }
        closeTag("parameterList");
    }

    public void compileVarDec() throws IOException 
    {
        openTag("varDec");
        consume("var");

        writeCurrentToken(); 
        tokenizer.advance();

        writeCurrentToken(); 
        tokenizer.advance();

        while (tokenizer.getCurrentToken().equals(",")) 
        {
            consume(",");
            writeCurrentToken(); 
            tokenizer.advance();
        }

        consume(";");
        closeTag("varDec");
    }

    public void compileStatements() throws IOException 
    {
        openTag("statements");
        closeTag("statements");
    }
}