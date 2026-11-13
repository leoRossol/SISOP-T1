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
    private Disco disco;
    private Utilities utils;
    private List<PCB> processos;
    private List<PCB> prontos;
    private List<PCB> bloqueados;
    private PCB rodando;
    private int proximoId;
    private HW hw;
    private boolean execUnico; // [T1C] true = exec <id> (roda só um processo, sem preempção)
    private Semaphore mutex;
    private Semaphore processosDisponiveis;
    private boolean[] frameEmIO;              // [T2B] nao escolher frame enquanto o disco o usa
    private PedidoVM[] carregamentosPendentes; // [T2B] leitura que espera o salvamento da vitima
    private List<PedidoVM> pageFaultsPendentes;
    private List<Integer> bloqueadosPorVM;
    private int proximoFrameVitima;

    public GP(GM _gm, Disco _disco, Utilities _utils, HW _hw) {
        gm = _gm;
        disco = _disco;
        utils = _utils;
        hw = _hw;
        processos = new ArrayList<>();
        prontos = new ArrayList<>();
        bloqueados = new ArrayList<>();
        rodando = null;
        proximoId = 0;
        execUnico = false; // [T1C] começa no modo escalonado (execAll)

        mutex = new Semaphore(1, true);
        processosDisponiveis = new Semaphore(0);
        int nroFrames = hw.mem.pos.length / gm.getTamPg();
        frameEmIO = new boolean[nroFrames];
        carregamentosPendentes = new PedidoVM[nroFrames];
        pageFaultsPendentes = new ArrayList<>();
        bloqueadosPorVM = new ArrayList<>();
        proximoFrameVitima = 0;
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
            // [T2B] todo processo guarda uma copia completa de suas paginas no disco
            int[] tabelaDisco = disco.aloca(programa.length);
            if (tabelaDisco == null) {
                System.out.println("Sem espaco no disco para carregar o programa");
                return false;
            }
            disco.gravaPrograma(programa, tabelaDisco);

            // [T2B] a RAM recebe somente a primeira pagina do processo.
            int[] tabela = new int[tabelaDisco.length];
            Arrays.fill(tabela, PCB.PAGINA_AUSENTE);
            int frameInicial = gm.alocaFrame();

            if (frameInicial == PCB.PAGINA_AUSENTE) {
                System.out.println("Sem memoria para carregar a primeira pagina");
                disco.desaloca(tabelaDisco);
                return false;
            }

            disco.copiaParaMemoria(tabelaDisco[0], hw.mem.pos, frameInicial);
            tabela[0] = frameInicial;

            int[] reg = new int[10];

            PCB pcb = new PCB(
                    proximoId,
                    0,
                    reg,
                    tabela,
                    programa.length,
                    EstadoProcesso.PRONTO);
            pcb.setTabelaDisco(tabelaDisco);

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
        disco.desaloca(pcb.getTabelaDisco());
        for (int i = pageFaultsPendentes.size() - 1; i >= 0; i--) {
            if (pageFaultsPendentes.get(i).getIdProcesso() == id) {
                pageFaultsPendentes.remove(i);
            }
        }
        bloqueadosPorVM.remove(Integer.valueOf(id));
        processos.remove(pcb);
        prontos.remove(pcb);
        bloqueados.remove(pcb);

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

        // [T1C/T2B] modo exec: sem troca por tempo, mas espera page faults do disco.
        execUnico = true;
        try {
            while ((pcb = buscarProcesso(id)) != null) {
                if (pcb.getEstado() == EstadoProcesso.BLOQUEADO) {
                    // IN/OUT do Console continuam pertencendo ao modo start da T2A.
                    if (!bloqueadosPorVM.contains(id) || !esperaFimDeIOVMSequencial()) {
                        break;
                    }
                    continue;
                }

                rodando = pcb;
                pcb.setEstado(EstadoProcesso.RODANDO);
                prontos.remove(pcb);
                restauraContexto(pcb);
                hw.cpu.run();
            }
        } finally {
            execUnico = false;
        }

        return true;
    }

    // No modo sequencial nao ha EscalonadorThread para consumir o aviso do disco.
    private boolean esperaFimDeIOVMSequencial() {
        while (!hw.cpu.temSinalVM()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        finalizaIOVM(hw.cpu.pegaSinalVM());
        return true;
    }

    // [T2B] Versao sequencial do execAll: espera o disco se todos bloquearem por page fault.
    public void executaTodosSequencial() {
        while (!processos.isEmpty()) {
            if (rodando == null) {
                if (!prontos.isEmpty()) {
                    escalona();
                } else if (!bloqueadosPorVM.isEmpty()) {
                    if (!esperaFimDeIOVMSequencial()) {
                        return;
                    }
                    continue;
                } else {
                    // Pode haver processo bloqueado esperando IN/OUT do Console;
                    // esse caso continua pertencendo ao modo concorrente start.
                    return;
                }
            }

            if (rodando != null) {
                hw.cpu.run();
            }
        }
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
        System.out.println("tabela de paginas (RAM): " + Arrays.toString(pcb.getTabelaPaginas()));
        System.out.println("tabela de paginas (disco): " + Arrays.toString(pcb.getTabelaDisco()));
        System.out.println("registradores: " + Arrays.toString(pcb.getReg()));

        // 3- imprimir a memória do processo página por página
        int[] tabela = pcb.getTabelaPaginas();
        int tamPg = gm.getTamPg();
        for (int i = 0; i < tabela.length; i++) {
            int frame = tabela[i];
            if (frame == PCB.PAGINA_AUSENTE) {
                System.out.println("pagina " + i + " -> fora da RAM");
                continue;
            }
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

    // [T1C] true se o escalonador colocou um processo na CPU (usado pela EscalonadorThread)
    public boolean temRodando() {
        return rodando != null;
    }

    // [T2A] acorda a thread da CPU se ela estiver dormindo (chamado pelo console ao fim de um IO)
    public void acordaEscalonador() {
        if (processosDisponiveis.availablePermits() == 0) {
            processosDisponiveis.release();
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







public void bloqueiaProcesso() {
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
        bloqueiaProcessoInterno();
    } finally {
        mutex.release();

        if (interrompida) {
            Thread.currentThread().interrupt();
        }
    }
}

private void bloqueiaProcessoInterno() {
    //1- se ninguem rodando, volta
    if (rodando == null) {
        return;
    }

    //2- estado bloqueado
    PCB pcb = rodando;
    salvaContexto(pcb);

    //3- colocar em bloqueados
    pcb.setEstado(EstadoProcesso.BLOQUEADO);
    bloqueados.add(pcb);

    System.out.println(
            "ESCALONADOR: bloqueio de IO no processo " + pcb.getId());

    escalonaInterno();
}



// [T2A] fim de IO: o processo <id> sai de bloqueados e vai para prontos
// [T2A] fim de IO: o processo <_id> sai de bloqueados e vai para prontos
public void desbloqueiaProcesso(int _id) {
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
        desbloqueiaProcessoInterno(_id);
    } finally {
        mutex.release();

        if (interrompida) {
            Thread.currentThread().interrupt();
        }
    }
}

private void desbloqueiaProcessoInterno(int _id) {
    //1- achar o PCB com esse id na fila de bloqueados
    PCB pcb = null;
    for (PCB p : bloqueados) {
        if (p.getId() == _id) {
            pcb = p;
            break;
        }
    }

    //2- se nao achou, volta
    if (pcb == null) {
        return;
    }

    //3- sai de bloqueados e vai para o fim de prontos
    bloqueados.remove(pcb);
    pcb.setEstado(EstadoProcesso.PRONTO);
    prontos.add(pcb);

    System.out.println(
            "ESCALONADOR: fim de IO, processo " + pcb.getId() + " volta para prontos");

    // sem escalonaInterno(): quem esta rodando continua e termina a fatia dele
}

// =====================================================================
// [T2B] Memoria virtual: page fault, disco e substituicao FIFO
// =====================================================================

public void trataPageFault(int _pagina) {
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
        if (rodando == null) {
            return;
        }

        PCB pcb = rodando;
        if (_pagina < 0 || _pagina >= pcb.getTabelaPaginas().length) {
            return;
        }

        PedidoVM pedido = new PedidoVM(
                pcb.getId(),
                PedidoVM.CARREGAR,
                _pagina,
                PCB.PAGINA_AUSENTE,
                pcb.getTabelaDisco()[_pagina]);
        pageFaultsPendentes.add(pedido);
        bloqueadosPorVM.add(pcb.getId());

        System.out.println("ESCALONADOR: page fault no processo " + pcb.getId()
                + ", pagina " + _pagina);
        bloqueiaProcessoInterno();
        atendeUmPageFaultInterno();
    } finally {
        mutex.release();
        if (interrompida) {
            Thread.currentThread().interrupt();
        }
    }
}

// Chamado pela rotina de interrupcao quando o disco terminou um pedido.
public void finalizaIOVM(PedidoVM _pedido) {
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
        int frame = _pedido.getFrameMemoria();

        if (_pedido.getTipo() == PedidoVM.SALVAR) {
            PedidoVM carregar = carregamentosPendentes[frame];
            carregamentosPendentes[frame] = null;

            PCB destino = carregar == null ? null : buscarProcesso(carregar.getIdProcesso());
            if (destino == null) {
                frameEmIO[frame] = false;
                gm.desalocaFrame(frame);
            } else {
                disco.adicionaPedido(carregar);
            }
        } else if (_pedido.getTipo() == PedidoVM.CARREGAR) {
            frameEmIO[frame] = false;
            PCB pcb = buscarProcesso(_pedido.getIdProcesso());

            if (pcb == null) {
                bloqueadosPorVM.remove(Integer.valueOf(_pedido.getIdProcesso()));
                gm.desalocaFrame(frame);
            } else {
                pcb.getTabelaPaginas()[_pedido.getPagina()] = frame;
                bloqueadosPorVM.remove(Integer.valueOf(pcb.getId()));
                desbloqueiaProcessoInterno(pcb.getId());
            }
        }

        atendeUmPageFaultInterno();
    } finally {
        mutex.release();
        if (interrompida) {
            Thread.currentThread().interrupt();
        }
    }
}

// Inicia uma unica leitura pendente quando existe frame livre ou vitima disponivel.
private void atendeUmPageFaultInterno() {
    if (pageFaultsPendentes.isEmpty()) {
        return;
    }

    PedidoVM pedido = pageFaultsPendentes.get(0);
    PCB destino = buscarProcesso(pedido.getIdProcesso());
    if (destino == null) {
        pageFaultsPendentes.remove(0);
        return;
    }

    int frame = gm.alocaFrame();
    if (frame != PCB.PAGINA_AUSENTE) {
        frameEmIO[frame] = true;
        pageFaultsPendentes.remove(0);
        disco.adicionaPedido(new PedidoVM(
                pedido.getIdProcesso(), PedidoVM.CARREGAR, pedido.getPagina(),
                frame, pedido.getFrameDisco()));
        return;
    }

    frame = escolheFrameVitimaInterno();
    if (frame == PCB.PAGINA_AUSENTE) {
        return; // todos os frames estao em operacao no disco; espera a proxima interrupcao
    }

    PCB vitima = encontraDonoDoFrame(frame);
    int paginaVitima = encontraPaginaNoFrame(vitima, frame);
    if (vitima == null || paginaVitima == PCB.PAGINA_AUSENTE) {
        return;
    }

    // A pagina vitima deixa de estar mapeada antes de o disco reutilizar o frame.
    vitima.getTabelaPaginas()[paginaVitima] = PCB.PAGINA_AUSENTE;
    frameEmIO[frame] = true;
    pageFaultsPendentes.remove(0);

    PedidoVM carregar = new PedidoVM(
            pedido.getIdProcesso(), PedidoVM.CARREGAR, pedido.getPagina(),
            frame, pedido.getFrameDisco());
    carregamentosPendentes[frame] = carregar;

    disco.adicionaPedido(new PedidoVM(
            vitima.getId(), PedidoVM.SALVAR, paginaVitima, frame,
            vitima.getTabelaDisco()[paginaVitima]));
}

private int escolheFrameVitimaInterno() {
    for (int tentativa = 0; tentativa < frameEmIO.length; tentativa++) {
        int frame = proximoFrameVitima;
        proximoFrameVitima = (proximoFrameVitima + 1) % frameEmIO.length;

        if (!frameEmIO[frame] && encontraDonoDoFrame(frame) != null) {
            return frame;
        }
    }
    return PCB.PAGINA_AUSENTE;
}

private PCB encontraDonoDoFrame(int _frame) {
    for (PCB pcb : processos) {
        if (encontraPaginaNoFrame(pcb, _frame) != PCB.PAGINA_AUSENTE) {
            return pcb;
        }
    }
    return null;
}

private int encontraPaginaNoFrame(PCB _pcb, int _frame) {
    if (_pcb == null) {
        return PCB.PAGINA_AUSENTE;
    }

    int[] tabela = _pcb.getTabelaPaginas();
    for (int pagina = 0; pagina < tabela.length; pagina++) {
        if (tabela[pagina] == _frame) {
            return pagina;
        }
    }
    return PCB.PAGINA_AUSENTE;
}



// [T2A] monta o pedido de IO com os dados do processo que está rodando
public PedidoIO criaPedidoIO(int _tipo, int _endLogico) {
   
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        try {
            if (rodando == null) {
                return null;
            } else {
                return new PedidoIO(rodando.getId(), _tipo, _endLogico, rodando.getTabelaPaginas());
            }
        } finally { mutex.release(); }
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
