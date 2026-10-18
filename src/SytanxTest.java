import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SytanxTest 
{
        public static void main(String[]args) throws IOException
        {
            String[] testFiles = {"Square","SquareGame","Main"};
            boolean allPassed = true;

            for (String[] testFiles)
            {
                File jack = new File("tests/" + base + ".jack");
                File generated = new File("tests/" + base + "P.xml");
                File official = new File ("tests/" + base + "P_oficial.xml");

                if (!jack.exists() || !official.exists())
                {
                    System.out.println("⚠️ Pulando teste " + base + "(arquivos oficiais ausentes em tests/)");
                    continue;
                }
            }
            JackTokenizer tokenizer = new JackTokenizer(jack);
            CompilationEngine engine = new CompilationEngine(tokenizer, generated);
            engine.compileClass();

            String genContent = normalize(Files.readAllLines(generated.toPath()));
            String offContent = normalize(Files.readAllLines(official.toPath));

            if (genContent.equals(offContent)) 
            {
                System.out.println("✅ " + base + ": Árvore sintática 100% idêntica!");
            }
            else
            {
                System.err.println("❌ " + base + ": Discrepância estrutural encontrada.");
                allPassed = false;
            }
        } 
        if (allPassed)
        {
            System.out.println("\n🎉 Todos os testes disponíveis passaram com sucesso!");
        }


    private static String normalize(List<String> lines){
    StringBuilder sb = new StringBuilder();
    for (String line : lines)
    {
        String trimmed = line.trim();
        if (!trimmed.isEmpty())
        {
            sb.append(trimmed).append("\n");
        }
    }
    return sb.toString();
    }
}

