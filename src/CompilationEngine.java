
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CompilationEngine 
{
    private JackTokenizer tokenizer;
    private VMWriter vmWriter;
    private SymbolTable symbolTable;
    private String currentClassName;
    private int labelIndex = 0;
    private FileWriter writer;
    private int indentLevel = 0;

    private static final Set<String> OP_SYMBOLS = new HashSet<>(Arrays.asList(
            "+", "-", "*", "/", "&", "|", "<", ">", "="));

    private static final Set<Character> UNARY_OPS = new HashSet<>(Arrays.asList('-', '~'));

    public CompilationEngine(JackTokenizer tokenizer, File outputFile) throws IOException 
    {
        this.tokenizer = tokenizer;
        this.vmWriter = new VMWriter(outputFile);
        this.symbolTable = new SymbolTable();

        if (tokenizer.hasMoreTokens()) 
        {
            tokenizer.advance();
        }
    }

    private void writeIndent() throws IOException 
    {
        if (writer == null) return;
        for (int i = 0; i < indentLevel; i++) 
        {
            writer.write("  ");
        }
    }

    private void openTag(String tag) throws IOException 
    {
        if (writer == null) return;
        writeIndent();
        writer.write("<" + tag + ">\n");
        indentLevel++;
    }

    private void closeTag(String tag) throws IOException 
    {
        if (writer == null) return;
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
        if (writer == null) return;
        TokenType type = tokenizer.tokenType();
        writeIndent();
        switch (type) 
        {
            case KEYWORD -> writer.write("<keyword> " + tokenizer.keyWord() + " </keyword>\n");
            case SYMBOL -> writer.write("<symbol> " + escapeXml(String.valueOf(tokenizer.symbol())) + " </symbol>\n");
            case IDENTIFIER -> writer.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");
            case INT_CONST -> writer.write("<integerConstant> " + tokenizer.intVal() + " </integerConstant>\n");
            case STRING_CONST -> writer.write("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>\n");
        }
    }

    private void consume(String expected) throws IOException 
    {
        if (!tokenizer.getCurrentToken().equals(expected)) 
        {
            throw new RuntimeException(
                    "Erro Sintático: Esperava '" + expected + "', mas recebeu '" + tokenizer.getCurrentToken() + "'");
        }
        writeCurrentToken();
        if (tokenizer.hasMoreTokens()) 
        {
            tokenizer.advance();
        }
    }

    public void close() throws IOException 
    {
        if (vmWriter != null) 
        {
            vmWriter.close();
        }
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
        SymbolTable.Kind kind = tokenizer.keyWord().equals("static") ? SymbolTable.Kind.STATIC : SymbolTable.Kind.FIELD;
        tokenizer.advance();

        String type = tokenizer.getCurrentToken();
        tokenizer.advance();

        String name = tokenizer.getCurrentToken();
        symbolTable.define(name, type, kind);
        tokenizer.advance();

        while (tokenizer.getCurrentToken().equals(",")) {
            tokenizer.advance(); // consome ','
            name = tokenizer.getCurrentToken();
            symbolTable.define(name, type, kind);
            tokenizer.advance();
        }
        tokenizer.advance(); // consome ';'
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
        while (tokenizer.tokenType() == TokenType.KEYWORD) 
        {
            String kw = tokenizer.keyWord();
            switch (kw) 
            {
                case "let" -> compileLet();
                case "do" -> compileDo();
                case "return" -> compileReturn();
                case "if" -> compileIf();       // implementar no próximo commit
                case "while" -> compileWhile(); // implementar no próximo commit
                default -> {
                    closeTag("statements");
                    return;
                }
            }
        }
        closeTag("statements");
    }

    public void compileLet() throws IOException 
    {
        tokenizer.advance(); 
        String varName = tokenizer.identifier();
        tokenizer.advance();

        boolean isArray = false;
        if (tokenizer.getCurrentToken().equals("[")) {
            isArray = true;
            tokenizer.advance(); 
            compileExpression();
            tokenizer.advance(); 
            vmWriter.writePush(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
            vmWriter.writeArithmetic(VMWriter.Command.ADD);
        }

        tokenizer.advance(); 
        compileExpression();
        tokenizer.advance(); 

        if (isArray) {
            vmWriter.writePop(VMWriter.Segment.TEMP, 0);
            vmWriter.writePop(VMWriter.Segment.POINTER, 1);
            vmWriter.writePush(VMWriter.Segment.TEMP, 0);
            vmWriter.writePop(VMWriter.Segment.THAT, 0);
        } else {
            vmWriter.writePop(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
        }
    }

    public void compileDo() throws IOException 
    {
        openTag("doStatement");
        consume("do");
        compileSubroutineCall();
        consume(";");
        closeTag("doStatement");
    }

    public void compileReturn() throws IOException 
    {
        tokenizer.advance(); 
        if (!tokenizer.getCurrentToken().equals(";")) {
            compileExpression();
        } else {
            vmWriter.writePush(VMWriter.Segment.CONST, 0);
        }
        tokenizer.advance(); 
        vmWriter.writeReturn();
    }

    public void compileIf() throws IOException 
    {
        String labelTrue = newLabel("IF_TRUE");
        String labelFalse = newLabel("IF_FALSE");
        String labelEnd = newLabel("IF_END");

        tokenizer.advance(); 
        tokenizer.advance(); 
        compileExpression();
        tokenizer.advance(); 

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);

        tokenizer.advance(); 
        compileStatements();
        tokenizer.advance(); 

        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord().equals("else")) {
            vmWriter.writeGoto(labelEnd);
            vmWriter.writeLabel(labelFalse);
            tokenizer.advance(); 
            tokenizer.advance(); 
            compileStatements();
            tokenizer.advance(); 
            vmWriter.writeLabel(labelEnd);
        } else {
            vmWriter.writeLabel(labelFalse);
        }
    }

    public void compileWhile() throws IOException 
    {
        String labelExp = newLabel("WHILE_EXP");
        String labelEnd = newLabel("WHILE_END");

        vmWriter.writeLabel(labelExp);
        tokenizer.advance(); 
        tokenizer.advance(); 
        compileExpression();
        tokenizer.advance(); 

        vmWriter.writeArithmetic(VMWriter.Command.NOT);
        vmWriter.writeIf(labelEnd);

        tokenizer.advance(); 
        compileStatements();
        tokenizer.advance(); 

        vmWriter.writeGoto(labelExp);
        vmWriter.writeLabel(labelEnd);
    }

    public void compileExpression() throws IOException 
    {
        compileTerm();
        while (tokenizer.tokenType() == TokenType.SYMBOL && "+-*/&|<>=".contains(tokenizer.getCurrentToken())) {
            String op = tokenizer.getCurrentToken();
            tokenizer.advance();
            compileTerm();

            switch (op) {
                case "+" -> vmWriter.writeArithmetic(VMWriter.Command.ADD);
                case "-" -> vmWriter.writeArithmetic(VMWriter.Command.SUB);
                case "*" -> vmWriter.writeCall("Math.multiply", 2);
                case "/" -> vmWriter.writeCall("Math.divide", 2);
                case "&" -> vmWriter.writeArithmetic(VMWriter.Command.AND);
                case "|" -> vmWriter.writeArithmetic(VMWriter.Command.OR);
                case "<" -> vmWriter.writeArithmetic(VMWriter.Command.LT);
                case ">" -> vmWriter.writeArithmetic(VMWriter.Command.GT);
                case "=" -> vmWriter.writeArithmetic(VMWriter.Command.EQ);
            }
        }
    }

    public void compileTerm() throws IOException 
    {
        openTag("term");

        TokenType type = tokenizer.tokenType();
        String token = tokenizer.getCurrentToken();

        if (type == TokenType.INT_CONST || type == TokenType.STRING_CONST) 
        {
            writeCurrentToken();
            tokenizer.advance();
        } else if (type == TokenType.KEYWORD && 
                (token.equals("true") || token.equals("false") || token.equals("null") || token.equals("this"))) 
                {
            writeCurrentToken();
            tokenizer.advance();
        } else if (token.equals("(")) 
        {
            consume("(");
            compileExpression();
            consume(")");
        } else if (type == TokenType.SYMBOL && UNARY_OPS.contains(tokenizer.symbol())) 
        {
            writeCurrentToken(); 
            tokenizer.advance();
            compileTerm();
        } else if (type == TokenType.IDENTIFIER) 
        {
            String next = tokenizer.peekToken();
            if ("[".equals(next)) 
            { 
                writeCurrentToken();
                tokenizer.advance();
                consume("[");
                compileExpression();
                consume("]");
            } else if ("(".equals(next) || ".".equals(next)) 
            { 
                compileSubroutineCall();
            } else 
            { 
                writeCurrentToken();
                tokenizer.advance();
            }
        }

        closeTag("term");
    }

    private void compileSubroutineCall() throws IOException 
    {
        writeCurrentToken(); 
        tokenizer.advance();

        if (tokenizer.getCurrentToken().equals(".")) 
        {
            consume(".");
            writeCurrentToken(); 
            tokenizer.advance();
        }

        consume("(");
        compileExpressionList();
        consume(")");
    }

    public void compileExpressionList() throws IOException 
    {
        openTag("expressionList");
        if (!tokenizer.getCurrentToken().equals(")")) 
        {
            compileExpression();
            while (tokenizer.getCurrentToken().equals(",")) 
            {
                consume(",");
                compileExpression();
            }
        }
        closeTag("expressionList");
    }
    private VMWriter.Segment getSegment(SymbolTable.Kind kind) {
        return switch (kind) {
            case STATIC -> VMWriter.Segment.STATIC;
            case FIELD -> VMWriter.Segment.THIS;
            case ARG -> VMWriter.Segment.ARG;
            case VAR -> VMWriter.Segment.LOCAL;
            default -> throw new IllegalArgumentException("Kind desconhecido: " + kind);
        };
    }

    private String newLabel(String prefix) {
        return prefix + "_" + (labelIndex++);
    }
}