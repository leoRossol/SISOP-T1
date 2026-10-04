// [T1B] Criado pelo grupo - Gerente de Processos (cria, busca, remove e lista processos)
// [T1C] + troca de contexto e escalonador
package so;

import hardware.HW;
import hardware.Word;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

public class GP {
    private GM gm;
    private Utilities utils;
    private List<PCB> processos;
    private List<PCB> prontos;
    private PCB rodando;
    private int proximoId;
    private HW hw;
    private boolean execUnico; // [T1C] true = exec <id> (roda só um processo, sem preempção)
    private Semaphore mutex;
    private Semaphore processosDisponiveis;

    public GP(GM _gm, Utilities _utils, HW _hw) {
        gm = _gm;
        utils = _utils;
        hw = _hw;
        processos = new ArrayList<>();
        prontos = new ArrayList<>();
        rodando = null;
        proximoId = 0;
        execUnico = false; // [T1C] começa no modo escalonado (execAll)

        mutex = new Semaphore(1, true);
        processosDisponiveis = new Semaphore(0);
    }

    // =====================================================================
    // [T1B] Gerência de processos: cria, busca, remove, lista, executa, dump
    // =====================================================================

    public boolean criaProcesso(Word[] programa) {
        if (programa == null) {
            return false;
        }

        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        boolean criou = false;

        try {
            int[] tabela = utils.loadProgram(programa);

            if (tabela == null) {
                return false;
            }

            int[] reg = new int[10];

            PCB pcb = new PCB(
                    proximoId,
                    0,
                    reg,
                    tabela,
                    programa.length,
                    EstadoProcesso.PRONTO);

            processos.add(pcb);
            prontos.add(pcb);
            proximoId++;

            criou = true;
            return true;
        } finally {
            mutex.release();

            if (criou && processosDisponiveis.availablePermits() ==0) {
                processosDisponiveis.release();
            }
        }
    }

    public PCB buscarProcesso(int id) {
        for (PCB pcb : processos) {
            if (pcb.getId() == id) {
                return pcb;
            }
        }
        return null;
    }

    private boolean desalocaProcessoInterno(int id) {
        PCB pcb = buscarProcesso(id);

        if (pcb == null) {
            return false;
        }

        gm.desaloca(pcb.getTabelaPaginas());
        processos.remove(pcb);
        prontos.remove(pcb);

        if (rodando == pcb) {
            rodando = null;
        }

        return true;
    }

    public boolean desalocaProcesso(int id) {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        try {
            PCB pcb = buscarProcesso(id);

            if (pcb == null) {
                return false;
            }

            if (pcb == rodando || pcb.getEstado() == EstadoProcesso.RODANDO) {
                System.out.println(
                        "Processo " + id
                                + " esta em execucao e nao pode ser removido agora.");
                return false;
            }

            return desalocaProcessoInterno(id);
        } finally {
            mutex.release();
        }
    }

    
    public void mostraProcessos() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            System.out.println("ID\tESTADO\t\tPC");

            for (PCB pcb : processos) {
                System.out.println(
                        pcb.getId() + "\t"
                                + pcb.getEstado() + "\t"
                                + pcb.getPc());
            }
        } finally {
            mutex.release();
        }
    }

    public boolean executaProcesso(int id) {
        // 1- buscar o PCB. Se não existir, avisar e devolver false.
        PCB pcb = buscarProcesso(id);
        if (pcb == null){
            System.out.println("Processo com id: " +id +" nao encontrado");
            return false;
        }
        if (pcb.getEstado()==EstadoProcesso.TERMINADO){
            System.out.println("Processo com id: " +id +" ja foi finalizado");
            return false;
        }

        // 2- marcar como rodando a variável e estado, tirar o PCB da lista de prontos.
        rodando = pcb;
        pcb.setEstado(EstadoProcesso.RODANDO);
        prontos.remove(pcb);

        // 3- restaurar o contexto na CPU
        restauraContexto(pcb);

        // 4- rodar a CPU só com este processo   // [T1C] modo exec: sem troca por tempo
        execUnico = true;
        hw.cpu.run();
        execUnico = false;  // [T1C] volta ao modo escalonado para o próximo execAll

        // [T1C] passos 5 e 6 comentados: quem cuida do fim agora é o terminaProcesso
        // 5- salvar o contexto de volta no PCB (pc e registradores)
        //salvaContexto(pcb);

        // 6- processo terminou, atualizar estados
        //rodando = null;
        //pcb.setEstado(EstadoProcesso.TERMINADO);

        return true;
    }

    public boolean dumpProcesso(int id) {
        // 1- buscar o PCB
        PCB pcb = buscarProcesso(id);
        if (pcb == null){
            System.out.println("Processo com id: " +id +" nao encontrado");
            return false;
        }

        // 2- imprimir os dados do PCB
        System.out.println("id: " +pcb.getId());
        System.out.println("estado: " +pcb.getEstado());
        System.out.println("pc: " +pcb.getPc());
        System.out.println("tamanho: " +pcb.getTamanhoPrograma());
        System.out.println("tabela de paginas: " + Arrays.toString(pcb.getTabelaPaginas()));
        System.out.println("registradores: " + Arrays.toString(pcb.getReg()));

        // 3- imprimir a memória do processo página por página
        int[] tabela = pcb.getTabelaPaginas();
        int tamPg = gm.getTamPg();
        for (int i = 0; i < tabela.length; i++) {
            int frame = tabela[i];
            int inicio = frame * tamPg;
            int fim = inicio + tamPg;
            System.out.println("pagina " + i + " -> frame " + frame);
            utils.dump(inicio, fim);
        }

        return true;
    }

    public List<PCB> getProcessos() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }

        try {
            return new ArrayList<>(processos);
        } finally {
            mutex.release();
        }
    }

    public boolean esperaProcessoDisponivel() {
        try {
            processosDisponiveis.acquire();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int getUltimoId() {
        return proximoId - 1;
    }

    // =====================================================================
    // [T1C] Escalonamento: troca de contexto e escalonador
    // =====================================================================

    // coloca na CPU o contexto guardado no PCB (processo entrando na CPU)
    private void restauraContexto(PCB pcb) {
        // 1- setContext com o pc e a tabela de páginas do PCB
        hw.cpu.setContext(pcb.getPc(), pcb.getTabelaPaginas());
            //registradores: copiar pcb.getReg() para cpu.reg
        int[] regsProcesso = pcb.getReg();
        for (int i=0; i < regsProcesso.length; i++){
            hw.cpu.reg[i] = regsProcesso[i];
        }
    }

    // guarda no PCB o contexto atual da CPU (processo saindo da CPU)
    private void salvaContexto(PCB pcb) {
        // 1- guardar hw.cpu.pc no PCB
        pcb.setPc(hw.cpu.pc);
        int[] regsProcesso = pcb.getReg();
            // registradores: copiar hw.cpu.reg para os registradores do PCB
        for (int i=0; i<regsProcesso.length; i++){
            regsProcesso[i] = hw.cpu.reg[i];
        }
    }

    // entrada publica do escalonador q protege o acesso às estruturas compartilhadas
    public void escalona() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            escalonaInterno();
        } finally {
            mutex.release();
        }
    }

    // escalonador round robin: põe na CPU o primeiro processo da fila de prontos
    // sem adquirir mutex
    // chamador deve possuir mutex
    private void escalonaInterno() {
            // 1- se prontos estiver vazia para tudo
            //    [T1C] no modo exec também para: só aquele processo roda, os outros ficam PRONTO
        if (prontos.isEmpty() || execUnico){
            rodando = null;
            hw.cpu.para();
            return;
        }
            // 2- tirar o primeiro PCB de prontos
        PCB pcb = prontos.get(0);
        prontos.remove(0);
            // 3- rodando passa a ser esse PCB; estado RODANDO
        rodando = pcb;
        pcb.setEstado(EstadoProcesso.RODANDO);
            // 4- restaurar o contexto dele na CPU
        restauraContexto(pcb);
        System.out.println("ESCALONADOR: entra processo: " +pcb.getId());
    }

    // fim da fatia de tempo: processo atual volta para o fim da fila e outro entra
    // Entrada pública da troca de tempo: protege fila e contexto.
public void trocaPorTempo() {
    boolean interrompida = false;

    while (true) {
        try {
            mutex.acquire();
            break;
        } catch (InterruptedException e) {
            interrompida = true;
        }
    }

    try {
        trocaPorTempoInterno();
    } finally {
        mutex.release();

        if (interrompida) {
            Thread.currentThread().interrupt();
        }
    }
}

// Troca de tempo sem adquirir o mutex.
// O chamador deve possuir o mutex.
private void trocaPorTempoInterno() {
    // O relógio pode tocar na mesma instrução que finalizou o processo.
    if (rodando == null) {
        return;
    }

    // no modo exec o mesmo processo recebe uma nova fatia
    if (execUnico) {
        salvaContexto(rodando);
        restauraContexto(rodando);
        return;
    }

    PCB pcb = rodando;

    salvaContexto(pcb);

    pcb.setEstado(EstadoProcesso.PRONTO);
    prontos.add(pcb);

    System.out.println(
            "ESCALONADOR: fim do slice, sai processo: " + pcb.getId());

    escalonaInterno();
}

    // fim do processo que está rodando (STOP ou erro): mostra o resultado, libera e escalona outro
    public void terminaProcesso() {
        boolean interrompida = false;

        while (true) {
            try {
                mutex.acquire();
                break;
            } catch (InterruptedException e) {
                interrompida = true;
            }
        }
        
            // 1- pegar o processo que está rodando
        try {
            PCB pcb = rodando;

            if(pcb == null){
                return;
            }
        
            // 2- mensagem "ESCALONADOR: processo X terminou"
            System.out.println("ESCALONADOR: termino do processo: " +pcb.getId());
            
                // 3- salva estado final e mostra o dump
            salvaContexto(pcb);
            pcb.setEstado(EstadoProcesso.TERMINADO);

            dumpProcesso(pcb.getId());

                // 4- liberar memória e PCB
            desalocaProcessoInterno(pcb.getId());

                // 5- chama o escalonador sem tentar adquirir mutex novamente
            escalonaInterno();

        } finally {
            mutex.release();

            if(interrompida){
                Thread.currentThread().interrupt();
            }
        }
    }


}
