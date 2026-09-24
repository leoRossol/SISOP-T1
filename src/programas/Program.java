package programas;

import hardware.Word;

// programa armazenado: um nome e sua imagem (codigo + dados) em palavras de memoria
public class Program {
    public String name;
    public Word[] image;

    public Program(String n, Word[] i) {
        name = n;
        image = i;
    }
}
