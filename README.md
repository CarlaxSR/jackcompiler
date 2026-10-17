# JackCompiler

Entrega Parcial 1: Analisador Léxico (Scanner / Tokenizer)

## Integrantes da Dupla
- Carla Sofia Santos Ribeiro - Matrícula: 20260001259
- Marcos Davi Taveira de Sousa - Matrícula: 20260001197

---

## Tecnologias e Requisitos
- **Linguagem:** Java (JDK 17+)
- **Ferramentas:** Git, compilador `javac`

---

## Estrutura do Projeto
```text
jackcompiler/
├── src/
│   ├── TokenType.java       # Enum com as categorias de tokens
│   ├── JackTokenizer.java   # Scanner: remoção de ruídos e fatiamento léxico
│   └── JackCompiler.java    # Ponto de entrada e gerador de XML
├── tests/   
│   └── TesteCustomizado.jack
│   └── TesteCustomizadoT.xml                # Arquivo .jack e gabarito (*T.xml)
├── README.md
└── .gitignore
```
---

## Compilando

```bash
javac src/*.java