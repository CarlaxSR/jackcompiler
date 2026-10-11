import java.io.File;
import java.io.IOException;

public class JackCompiler 
{
    public static void main(String[] args) 
    {
        if(args.length==0)
        {
            System.out.println("Uso: java JackCompiler <arquivo.jack ou diretorio>");
            return;
        }
        File input = new File(args[0]);if(input.isFile()&&input.getName().endsWith(".jack"))
        {
            processFile(input);
        }else if(input.isDirectory())
        {
            File[] files = input.listFiles((dir, name) -> name.endsWith(".jack"));
            if (files != null) 
            {
                for (File file : files) 
                {
                    processFile(file);
                }
            }
        }
    }


    private static void processFile(File jackFile)
    {
        try
        {
            JackTokenizer tokenizer = new JackTokenizer(jackFile);
            String outPath = jackFile.getAbsolutePath().replaceAll("\\.jack$","T.xml");
            writeTokensXml(tokenizer, new File(outPath));
            System.out.println("Gerado: " + outPath);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    private static void writeTokensXml(JackTokenizer tokenize, File outputFile) throws IOException
    {
//falta implementar
    }
}