package so;

import hardware.HW;
import hardware.Word;

// ------------------ U T I L I T A R I O S   D O   S I S T E M A
// ------------------ load é invocado a partir de requisição do usuário

// carga na memória
public class Utilities {
    private HW hw;
    private GM gm;                        // [T1A] usado na carga para alocar frames

    public Utilities(HW _hw, GM _gm) {    // [T1A] recebe o GM
        hw = _hw;
        gm = _gm;                         
    }



    // [T1A] carga paginada: aloca frames no GM e copia cada pagina para o seu frame
    // devolve tabela de paginas, ou null se nao houver memoria
    public int[] loadProgram(Word[] p) {
        Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
        int tamPg = gm.getTamPg();

        //1- pedir ao GM alocacao para o tamanho do programa
        int[] tabela = gm.aloca(p.length);
        if (tabela == null) { 
            System.out.println("Sem memória para carregar o programa");
            return  null; 
        }

        //2- calcular o endereco fisico da palavra i
        for (int i = 0; i < p.length; i++) {
            int pagina = i/tamPg;
            int offset = i%tamPg;
            int frame = tabela[pagina];
            int fisico = (frame * tamPg) + offset;

            m[fisico].opc = p[i].opc;
            m[fisico].ra = p[i].ra;
            m[fisico].rb = p[i].rb;
            m[fisico].p = p[i].p;
        }
        return tabela;
    }

    // dump da memória
    public void dump(Word w) { // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
        System.out.print("[ ");
        System.out.print(w.opc);
        System.out.print(", ");
        System.out.print(w.ra);
        System.out.print(", ");
        System.out.print(w.rb);
        System.out.print(", ");
        System.out.print(w.p);
        System.out.println("  ] ");
    }

    public void dump(int ini, int fim) {
        Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
        for (int i = ini; i < fim; i++) {
            System.out.print(i);
            System.out.print(":  ");
            dump(m[i]);
        }
    }

    public void loadAndExec(Word[] p) {
        int[] tabela = loadProgram(p); // [T1A] carga paginada; guarda a tabela de páginas
        if (tabela == null) { return; }   // [T1A] não coube na memória: não executa
        System.out.println("---------------------------------- programa carregado na memoria");
        dump(0, p.length); // dump da memoria nestas posicoes
        hw.cpu.setContext(0, tabela); // seta pc para endereço 0 - ponto de entrada dos programas  // [T1A] passa a tabela de páginas para a CPU
        System.out.println("---------------------------------- inicia execucao ");
        hw.cpu.run(); // cpu roda programa ate parar
        System.out.println("---------------------------------- memoria após execucao ");
        dump(0, p.length); // dump da memoria com resultado
    }
}
