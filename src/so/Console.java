// [T2A] Criado pelo grupo - dispositivo de console (fila de pedidos de IO)
package so;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Console {
    private List<PedidoIO> fila;
    private Semaphore mutexFila;
    private Semaphore pedidosDisponiveis;

    public Console() {
        fila = new ArrayList<>();
        mutexFila = new Semaphore(1);
        pedidosDisponiveis = new Semaphore(0);
    }

    // lado do produtor: chamado pela syscall (thread da CPU)
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
}
