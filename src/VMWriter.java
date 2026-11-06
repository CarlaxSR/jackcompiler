import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {
    public enum Segment {
        CONST("constant"), ARG("argument"), LOCAL("local"), STATIC("static"),
        THIS("this"), THAT("that"), POINTER("pointer"), TEMP("temp");

        private final String text;
        Segment(String text) { this.text = text; }
        public String getText() { return text; }
    }

    public enum Command {
        ADD("add"), SUB("sub"), NEG("neg"), EQ("eq"), GT("gt"), LT("lt"),
        AND("and"), OR("or"), NOT("not");

        private final String text;
        Command(String text) { this.text = text; }
        public String getText() { return text; }
    }

    private FileWriter writer;

    public VMWriter(File outputFile) throws IOException {
        this.writer = new FileWriter(outputFile);
    }

    public void writePush(Segment segment, int index) throws IOException {
        writer.write("push " + segment.getText() + " " + index + "\n");
    }

    public void writePop(Segment segment, int index) throws IOException {
        writer.write("pop " + segment.getText() + " " + index + "\n");
    }

    public void writeArithmetic(Command command) throws IOException {
        writer.write(command.getText() + "\n");
    }

    public void writeLabel(String label) throws IOException {
        writer.write("label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        writer.write("goto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        writer.write("if-goto " + label + "\n");
    }

    public void writeCall(String name, int nArgs) throws IOException {
        writer.write("call " + name + " " + nArgs + "\n");
    }

    public void writeFunction(String name, int nLocals) throws IOException {
        writer.write("function " + name + " " + nLocals + "\n");
    }

    public void writeReturn() throws IOException {
        writer.write("return\n");
    }

    public void close() throws IOException {
        if (writer != null) writer.close();
    }
}