// [T2A] Criado pelo grupo - dispositivo de console (fila de pedidos de IO)
package so;

import hardware.HW;
import hardware.Interrupts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Console extends Thread {
    private List<PedidoIO> fila;
    private Semaphore mutexFila;
    private Semaphore pedidosDisponiveis;
    private volatile boolean ativa;
    private HW hw;
    private int tamPg;
    private GP gp;      // para acordar a thread da CPU quando ela está ociosa


    public Console(HW _hw, int _tamPg, GP _gp) {
        fila = new ArrayList<>();
        mutexFila = new Semaphore(1);
        pedidosDisponiveis = new Semaphore(0);
        ativa = true;
        hw = _hw;
        tamPg = _tamPg;
        gp = _gp;
    }



    // produtor- chamado pela syscall (thread da CPU)
    public void adicionaPedido(PedidoIO _pedido) {
        
        //1- pegar o mutexFila
        try {
            mutexFila.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        //2- colocar o pedido no fim da fila
        try {
            fila.addLast(_pedido);
        } finally {
            mutexFila.release();
        }

        // 3- avisar que existe mais um pedido
        pedidosDisponiveis.release();
    }




    // consumidor- tira o primeiro pedido da fila
    private PedidoIO pegaPedido() {
        //1- pegar o mutexFila
        try {
            mutexFila.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        //2- pega o primeiro pedido da fila
        try {
            return fila.removeFirst();
        } finally {
            mutexFila.release();
        }

    }


    private int traduz(int[] _tabela, int _endLogico) {
        //1- enderco negativo -> invalido
        if (_endLogico < 0){
                return -1;
        }
        //2- calcular a pagina e offset
        int pagina = _endLogico/tamPg;
        int offset = _endLogico%tamPg;
        //3- a pagina existe na tabela desse processo
        if (pagina >= _tabela.length){
            return -1;
        }
        //4- calcular o fisico e devolver
        int frame = _tabela[pagina];
        int fisico = (frame * tamPg) + offset;
        return fisico;
    }



    public void pararThread() {
        ativa = false;
        interrupt();
    }


    @Override
    public void run() {
        while (ativa) {

            //esperar um pedido
            try {
                pedidosDisponiveis.acquire();   // dorme até ter pedido
            } catch (InterruptedException e) {
                break;                          // acordaram à força = pararThread() = fim
            }

            PedidoIO pedido = pegaPedido();
            if (pedido == null){
                break;
            }

            System.out.println("CONSOLE: atendendo pedido do processo " + pedido.getIdProcesso() + " (tipo " + pedido.getTipo() + ", endereco " + pedido.getEndLogico() + ")");
            
            if (pedido.getTipo() == 2) {             // OUT
                int fis = traduz(pedido.getTabelaPaginas(), pedido.getEndLogico());
                if (fis >= 0) {
                    System.out.println("OUT:   " + hw.mem.pos[fis].p);
                } else {
                    System.out.println("CONSOLE: endereco invalido no pedido do processo " + pedido.getIdProcesso());
                }
            }

            // avisa a CPU que o IO deste processo terminou
            hw.cpu.sinalizaIO(pedido.getIdProcesso());

            // se a CPU estiver ociosa (thread dormindo), acorda ela para tratar o aviso
            gp.acordaEscalonador();
        }
    }
}

