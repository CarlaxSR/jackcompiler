import java.io.File;
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

    private static final Set<String> OP_SYMBOLS = new HashSet<>(Arrays.asList(
            "+", "-", "*", "/", "&", "|", "<", ">", "="));

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

    private void consume(String expected) throws IOException 
    {
        if (!tokenizer.getCurrentToken().equals(expected)) 
        {
            throw new RuntimeException(
                    "Erro Sintático: Esperava '" + expected + "', mas recebeu '" + tokenizer.getCurrentToken() + "'");
        }
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
    }

    public void compileClass() throws IOException 
    {
        consume("class");
        currentClassName = tokenizer.identifier();
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

        while (tokenizer.getCurrentToken().equals(",")) 
        {
            tokenizer.advance(); // consome ','
            name = tokenizer.getCurrentToken();
            symbolTable.define(name, type, kind);
            tokenizer.advance();
        }
        consume(";");
    }

    public void compileSubroutine() throws IOException 
    {
        symbolTable.startSubroutine();
        String subType = tokenizer.keyWord(); // constructor, function, method
        tokenizer.advance();
        tokenizer.advance(); // tipo de retorno
        String subName = tokenizer.identifier();
        tokenizer.advance();

        if (subType.equals("method")) 
        {
            symbolTable.define("this", currentClassName, SymbolTable.Kind.ARG);
        }

        tokenizer.advance(); // '('
        compileParameterList();
        tokenizer.advance(); // ')'

        tokenizer.advance(); // '{'
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord().equals("var")) 
        {
            compileVarDec();
        }

        int nLocals = symbolTable.varCount(SymbolTable.Kind.VAR);
        vmWriter.writeFunction(currentClassName + "." + subName, nLocals);

        if (subType.equals("constructor")) 
        {
            int nFields = symbolTable.varCount(SymbolTable.Kind.FIELD);
            vmWriter.writePush(VMWriter.Segment.CONST, nFields);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        } 
        else if (subType.equals("method")) 
        {
            vmWriter.writePush(VMWriter.Segment.ARG, 0);
            vmWriter.writePop(VMWriter.Segment.POINTER, 0);
        }

        compileStatements();
        tokenizer.advance(); // '}'
    }

    public void compileParameterList() throws IOException 
    {
        if (!tokenizer.getCurrentToken().equals(")")) 
        {
            String type = tokenizer.getCurrentToken();
            tokenizer.advance();
            String name = tokenizer.getCurrentToken();
            symbolTable.define(name, type, SymbolTable.Kind.ARG);
            tokenizer.advance();

            while (tokenizer.getCurrentToken().equals(",")) 
            {
                tokenizer.advance(); // consome ','
                type = tokenizer.getCurrentToken();
                tokenizer.advance();
                name = tokenizer.getCurrentToken();
                symbolTable.define(name, type, SymbolTable.Kind.ARG);
                tokenizer.advance();
            }
        }
    }

    public void compileVarDec() throws IOException 
    {
        tokenizer.advance(); // consome 'var'
        String type = tokenizer.getCurrentToken();
        tokenizer.advance();
        String name = tokenizer.getCurrentToken();
        symbolTable.define(name, type, SymbolTable.Kind.VAR);
        tokenizer.advance();

        while (tokenizer.getCurrentToken().equals(",")) 
        {
            tokenizer.advance();
            name = tokenizer.getCurrentToken();
            symbolTable.define(name, type, SymbolTable.Kind.VAR);
            tokenizer.advance();
        }
        consume(";");
    }

    public void compileStatements() throws IOException 
    {
        while (tokenizer.tokenType() == TokenType.KEYWORD) 
        {
            String kw = tokenizer.keyWord();
            switch (kw) 
            {
                case "let" -> compileLet();
                case "do" -> compileDo();
                case "return" -> compileReturn();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                default -> {
                    return;
                }
            }
        }
    }

    public void compileLet() throws IOException 
    {
        tokenizer.advance(); // consome 'let'
        String varName = tokenizer.identifier();
        tokenizer.advance();

        boolean isArray = false;
        if (tokenizer.getCurrentToken().equals("[")) 
        {
            isArray = true;
            tokenizer.advance(); 
            compileExpression();
            tokenizer.advance(); 
            vmWriter.writePush(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
            vmWriter.writeArithmetic(VMWriter.Command.ADD);
        }

        tokenizer.advance(); // consome '='
        compileExpression();
        consume(";");

        if (isArray) 
        {
            vmWriter.writePop(VMWriter.Segment.TEMP, 0);
            vmWriter.writePop(VMWriter.Segment.POINTER, 1);
            vmWriter.writePush(VMWriter.Segment.TEMP, 0);
            vmWriter.writePop(VMWriter.Segment.THAT, 0);
        } 
        else 
        {
            vmWriter.writePop(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
        }
    }

    public void compileDo() throws IOException 
    {
        tokenizer.advance(); // consome 'do'
        compileSubroutineCall();
        consume(";");
        vmWriter.writePop(VMWriter.Segment.TEMP, 0);
    }

    public void compileReturn() throws IOException 
    {
        tokenizer.advance(); 
        if (!tokenizer.getCurrentToken().equals(";")) 
        {
            compileExpression();
        } else {
            vmWriter.writePush(VMWriter.Segment.CONST, 0);
        }
        consume(";");
        vmWriter.writeReturn();
    }

    public void compileIf() throws IOException 
    {
        String labelTrue = newLabel("IF_TRUE");
        String labelFalse = newLabel("IF_FALSE");
        String labelEnd = newLabel("IF_END");

        tokenizer.advance(); // consome 'if'
        tokenizer.advance(); // consome '('
        compileExpression();
        tokenizer.advance(); // consome ')'

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);

        tokenizer.advance(); // consome '{'
        compileStatements();
        tokenizer.advance(); // consome '}'

        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord().equals("else")) 
        {
            vmWriter.writeGoto(labelEnd);
            vmWriter.writeLabel(labelFalse);
            tokenizer.advance(); // consome 'else'
            tokenizer.advance(); // consome '{'
            compileStatements();
            tokenizer.advance(); // consome '}'
            vmWriter.writeLabel(labelEnd);
        } 
        else 
        {
            vmWriter.writeLabel(labelFalse);
        }
    }

    public void compileWhile() throws IOException 
    {
        String labelExp = newLabel("WHILE_EXP");
        String labelEnd = newLabel("WHILE_END");

        vmWriter.writeLabel(labelExp);
        tokenizer.advance(); // consome 'while'
        tokenizer.advance(); // consome '('
        compileExpression();
        tokenizer.advance(); // consome ')'

        vmWriter.writeArithmetic(VMWriter.Command.NOT);
        vmWriter.writeIf(labelEnd);

        tokenizer.advance(); // consome '{'
        compileStatements();
        tokenizer.advance(); // consome '}'

        vmWriter.writeGoto(labelExp);
        vmWriter.writeLabel(labelEnd);
    }

    public void compileExpression() throws IOException 
    {
        compileTerm();
        while (tokenizer.tokenType() == TokenType.SYMBOL && OP_SYMBOLS.contains(tokenizer.getCurrentToken())) 
        {
            String op = tokenizer.getCurrentToken();
            tokenizer.advance();
            compileTerm();

            switch (op) 
            {
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
        TokenType type = tokenizer.tokenType();
        String token = tokenizer.getCurrentToken();

        if (type == TokenType.INT_CONST) 
        {
            vmWriter.writePush(VMWriter.Segment.CONST, tokenizer.intVal());
            tokenizer.advance();
        } 
        else if (type == TokenType.STRING_CONST) 
        {
            String str = tokenizer.stringVal();
            vmWriter.writePush(VMWriter.Segment.CONST, str.length());
            vmWriter.writeCall("String.new", 1);
            for (char c : str.toCharArray()) {
                vmWriter.writePush(VMWriter.Segment.CONST, (int) c);
                vmWriter.writeCall("String.appendChar", 2);
            }
            tokenizer.advance();
        } 
        else if (type == TokenType.KEYWORD) 
        {
            switch (token) 
            {
                case "true" -> 
                {
                    vmWriter.writePush(VMWriter.Segment.CONST, 0);
                    vmWriter.writeArithmetic(VMWriter.Command.NOT);
                }
                case "false", "null" -> vmWriter.writePush(VMWriter.Segment.CONST, 0);
                case "this" -> vmWriter.writePush(VMWriter.Segment.POINTER, 0);
            }
            tokenizer.advance();
        } 
        else if (token.equals("(")) 
        {
            tokenizer.advance(); 
            compileExpression();
            tokenizer.advance(); 
        } 
        else if (type == TokenType.SYMBOL && (token.equals("-") || token.equals("~"))) 
        {
            char op = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();
            if (op == '-') vmWriter.writeArithmetic(VMWriter.Command.NEG);
            else vmWriter.writeArithmetic(VMWriter.Command.NOT);
        } 
        else if (type == TokenType.IDENTIFIER) 
        {
            String next = tokenizer.peekToken();
            if ("[".equals(next)) 
            { 
                String arrName = tokenizer.identifier();
                tokenizer.advance();
                tokenizer.advance();
                compileExpression();
                tokenizer.advance(); 
                vmWriter.writePush(getSegment(symbolTable.kindOf(arrName)), symbolTable.indexOf(arrName));
                vmWriter.writeArithmetic(VMWriter.Command.ADD);
                vmWriter.writePop(VMWriter.Segment.POINTER, 1);
                vmWriter.writePush(VMWriter.Segment.THAT, 0);
            } 
            else if ("(".equals(next) || ".".equals(next)) 
            {
                compileSubroutineCall();
            } 
            else 
            {
                String varName = tokenizer.identifier(); 
                vmWriter.writePush(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName)); 
                tokenizer.advance();
            }
        }
    }

    private void compileSubroutineCall() throws IOException 
    {
        String first = tokenizer.identifier();
        tokenizer.advance();
        String funcName;
        int nArgs = 0;

        if (tokenizer.getCurrentToken().equals(".")) {
            tokenizer.advance(); // '.'
            String subName = tokenizer.identifier();
            tokenizer.advance();

            SymbolTable.Kind kind = symbolTable.kindOf(first);
            if (kind != SymbolTable.Kind.NONE) {
                // Chamada de método em objeto: empurra a referência em this
                vmWriter.writePush(getSegment(kind), symbolTable.indexOf(first));
                funcName = symbolTable.typeOf(first) + "." + subName;
                nArgs = 1;
            } else {
                // Chamada de função estática de outra classe
                funcName = first + "." + subName;
            }
        } else {
            // Método chamado internamente na própria classe
            vmWriter.writePush(VMWriter.Segment.POINTER, 0);
            funcName = currentClassName + "." + first;
            nArgs = 1;
        }

        tokenizer.advance(); // '('
        nArgs += compileExpressionList();
        tokenizer.advance(); // ')'
        vmWriter.writeCall(funcName, nArgs);
    }

    public int compileExpressionList() throws IOException 
    {
        int count = 0;
        if (!tokenizer.getCurrentToken().equals(")")) {
            compileExpression();
            count++;
            while (tokenizer.getCurrentToken().equals(",")) {
                tokenizer.advance();
                compileExpression();
                count++;
            }
        }
        return count;
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