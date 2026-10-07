// [T1A] Criado pelo grupo - Gerente de Memória com paginação
package so;
public class GM {
    private final int tamPg;             // tamanho da página (= tamanho do frame)
    private boolean[] frameLivre;  // frameLivre[f] == true  -> frame f está livre

    // divide a memoria em frames do tamanho uma pagina
    // anota que o frame esta livre
    public GM(int tamMem, int _tamPg) {
        tamPg = _tamPg;

        int nroFrames = tamMem / tamPg;

        frameLivre = new boolean[nroFrames];
            for (int i = 0; i<frameLivre.length; i++){
                frameLivre[i] = true;
            }
    }


    // Aloca frames para nroPalavras. Devolve a tabela de páginas, ou null se não houver espaço.
    // calcula quantas paginas o prog ocupa
    // conta quantos frames livres tem
        // se tem o suficiente, aloca os primeiros que achar livre
        // se nao tem suficiente, retorna sem fazer nada
    public int[] aloca(int nroPalavras) {

        // 1- quantas páginas são necessárias, arredonda pra cima
        int nroPaginas = nroPalavras / tamPg;
        if (nroPalavras % tamPg > 0){
            nroPaginas +=1;
        }
        // 2- quantos frames livres existem
        int livres = 0;
        for (int i=0; i<frameLivre.length; i++){
            if (frameLivre[i] == true) {
                livres += 1;
            }
        }
        // 3- se não houver livres suficientes, desista
        if (livres < nroPaginas){ return null; }

        // 4- crie a tabela (um array de int com nroPaginas posições)
        int [] tabela = new int[nroPaginas];

        // 5- percorra os frames; a cada frame livre encontrado, marque como ocupado
        int p = 0;
        for (int i=0; i<frameLivre.length; i++){
            if (frameLivre[i] == true) {
                tabela[p] = i;
                frameLivre[i] = false;
                p++;
                if (p == tabela.length){ break; }
            }
        } return tabela;
    }


    // Libera os frames de uma tabela de paginas
    // percorre a tabela do processo e marca seus frames como livres
    public void desaloca(int[] tabela){
        for (int p = 0; p < tabela.length; p++){
            frameLivre[tabela[p]] = true;    // libera o frame onde estava a página p
        }
    }


    // util - getter tamPG
    public int getTamPg(){ return tamPg; }
}
