// [T2B] Criado pelo grupo - disco simulado para guardar paginas dos processos
package so;

import hardware.HW;
import hardware.Opcode;
import hardware.Word;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Disco extends Thread {
    private final int tamPg;
    private Word[] pos;
    private boolean[] frameLivre;
    private HW hw;
    private GP gp;
    private List<PedidoVM> fila;
    private Semaphore mutexFila;
    private Semaphore pedidosDisponiveis;
    private volatile boolean ativa;

    public Disco(HW _hw, int tamDisco, int _tamPg) {
        tamPg = _tamPg;
        hw = _hw;
        pos = new Word[tamDisco];

        for (int i = 0; i < pos.length; i++) {
            pos[i] = new Word(Opcode.___, -1, -1, -1);
        }

        int nroFrames = tamDisco / tamPg;
        frameLivre = new boolean[nroFrames];
        for (int i = 0; i < frameLivre.length; i++) {
            frameLivre[i] = true;
        }

        fila = new ArrayList<>();
        mutexFila = new Semaphore(1);
        pedidosDisponiveis = new Semaphore(0);
        ativa = true;
    }

    public void setGP(GP _gp) {
        gp = _gp;
    }

    // Reserva frames no disco para a quantidade de palavras informada.
    // Devolve a tabela de paginas, ou null se nao houver espaco.
    public int[] aloca(int nroPalavras) {
        int nroPaginas = nroPalavras / tamPg;
        if (nroPalavras % tamPg > 0) {
            nroPaginas++;
        }

        int livres = 0;
        for (int i = 0; i < frameLivre.length; i++) {
            if (frameLivre[i]) {
                livres++;
            }
        }

        if (livres < nroPaginas) {
            return null;
        }

        int[] tabela = new int[nroPaginas];
        int pagina = 0;
        for (int frame = 0; frame < frameLivre.length; frame++) {
            if (frameLivre[frame]) {
                tabela[pagina] = frame;
                frameLivre[frame] = false;
                pagina++;
                if (pagina == tabela.length) {
                    break;
                }
            }
        }
        return tabela;
    }

    public void desaloca(int[] tabela) {
        for (int pagina = 0; pagina < tabela.length; pagina++) {
            int frame = tabela[pagina];
            if (frame >= 0 && frame < frameLivre.length) {
                frameLivre[frame] = true;
            }
        }
    }

    // produtor: o GP pede uma leitura ou gravacao de pagina
    public void adicionaPedido(PedidoVM _pedido) {
        try {
            mutexFila.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            fila.add(_pedido);
        } finally {
            mutexFila.release();
        }
        pedidosDisponiveis.release();
    }

    private PedidoVM pegaPedido() {
        try {
            mutexFila.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        try {
            return fila.remove(0);
        } finally {
            mutexFila.release();
        }
    }

    public void pararThread() {
        ativa = false;
        interrupt();
    }

    @Override
    public void run() {
        while (ativa) {
            try {
                pedidosDisponiveis.acquire();
            } catch (InterruptedException e) {
                break;
            }

            PedidoVM pedido = pegaPedido();
            if (pedido == null) {
                break;
            }

            if (pedido.getTipo() == PedidoVM.CARREGAR) {
                copiaParaMemoria(pedido.getFrameDisco(), hw.mem.pos, pedido.getFrameMemoria());
                System.out.println("DISCO: carregou pagina " + pedido.getPagina()
                        + " do processo " + pedido.getIdProcesso());
            } else if (pedido.getTipo() == PedidoVM.SALVAR) {
                copiaDaMemoria(hw.mem.pos, pedido.getFrameMemoria(), pedido.getFrameDisco());
                System.out.println("DISCO: salvou pagina " + pedido.getPagina()
                        + " do processo " + pedido.getIdProcesso());
            }

            hw.cpu.sinalizaVM(pedido);
            if (gp != null) {
                gp.acordaEscalonador();
            }
        }
    }

    // Grava o programa nas paginas reservadas no disco.
    public void gravaPrograma(Word[] programa, int[] tabelaDisco) {
        for (int i = 0; i < programa.length; i++) {
            int pagina = i / tamPg;
            int offset = i % tamPg;
            int frameDisco = tabelaDisco[pagina];
            int enderecoDisco = frameDisco * tamPg + offset;
            copiaWord(programa[i], pos[enderecoDisco]);
        }
    }

    // Copia uma pagina completa da memoria principal para o disco.
    public void copiaDaMemoria(Word[] memoria, int frameMemoria, int frameDisco) {
        for (int offset = 0; offset < tamPg; offset++) {
            int enderecoMemoria = frameMemoria * tamPg + offset;
            int enderecoDisco = frameDisco * tamPg + offset;
            copiaWord(memoria[enderecoMemoria], pos[enderecoDisco]);
        }
    }

    // Copia uma pagina completa do disco para a memoria principal.
    public void copiaParaMemoria(int frameDisco, Word[] memoria, int frameMemoria) {
        for (int offset = 0; offset < tamPg; offset++) {
            int enderecoDisco = frameDisco * tamPg + offset;
            int enderecoMemoria = frameMemoria * tamPg + offset;
            copiaWord(pos[enderecoDisco], memoria[enderecoMemoria]);
        }
    }

    private void copiaWord(Word origem, Word destino) {
        destino.opc = origem.opc;
        destino.ra = origem.ra;
        destino.rb = origem.rb;
        destino.p = origem.p;
    }

    public int getTamPg() {
        return tamPg;
    }
}
