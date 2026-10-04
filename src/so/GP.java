// [T1B] Criado pelo grupo - Gerente de Processos (cria, busca, remove e lista processos)
// [T1C] + troca de contexto e escalonador
package so;

import hardware.HW;
import hardware.Word;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GP {
    private GM gm;
    private Utilities utils;
    private List<PCB> processos;
    private List<PCB> prontos;
    private PCB rodando;
    private int proximoId;
    private HW hw;

    public GP(GM _gm, Utilities _utils, HW _hw) {
        gm = _gm;
        utils = _utils;
        hw = _hw;
        processos = new ArrayList<>();
        prontos = new ArrayList<>();
        rodando = null;
        proximoId = 0;
    }

    // =====================================================================
    // [T1B] Gerência de processos: cria, busca, remove, lista, executa, dump
    // =====================================================================

    public boolean criaProcesso(Word[] programa) {
        if (programa == null) {
            return false;
        } // verifica se o programa é válido

        int[] tabela = utils.loadProgram(programa);
        if (tabela == null) {
            return false;
        } // se não houver memória, loadProgram retorna null

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

        return true;
    }

    public PCB buscarProcesso(int id) {
        for (PCB pcb : processos) {
            if (pcb.getId() == id) {
                return pcb;
            }
        }
        return null;
    }

    public boolean desalocaProcesso(int id) {
        PCB pcb = buscarProcesso(id);

        if (pcb == null) {
            return false;
        }

        gm.desaloca(pcb.getTabelaPaginas()); // libera memória alocada ao processo
        processos.remove(pcb);
        prontos.remove(pcb);

        if (rodando == pcb) {
            rodando = null;
        } // verifica processo rodando

        return true;
    }

    
    public void mostraProcessos() {
        System.out.println("ID\tESTADO\t\tPC");
        for (PCB pcb : processos) {
            System.out.println(
                    pcb.getId() + "\t"
                            + pcb.getEstado() + "\t"
                            + pcb.getPc());
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

        // 4- rodar a CPU
        hw.cpu.run();

        // 5- salvar o contexto de volta no PCB (pc e registradores)
        salvaContexto(pcb);

        // 6- processo terminou, atualizar estados
        rodando = null;
        pcb.setEstado(EstadoProcesso.TERMINADO);

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

    public List<PCB> getProcessos() { return processos; }
    public int getUltimoId() { return proximoId - 1;}

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

    // escalonador round robin: põe na CPU o primeiro processo da fila de prontos
    public void escalona() {
            // 1- se prontos estiver vazia para tudo
        if (prontos.isEmpty()){
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
    public void trocaPorTempo() {
            // 0- relógio tocou mas o último processo acabou nesta instrucao -> nada a trocar
        if (rodando == null){
            return;
        }
            // 1- salvar o contexto do processo que está rodando
        PCB pcb = rodando;
        salvaContexto(rodando);
            // 2- estado PRONTO
        pcb.setEstado(EstadoProcesso.PRONTO);
            // 3- colocar no fim da fila de prontos
        prontos.addLast(pcb);
        System.out.println("ESCALONADOR: fim do slice, sai processo: " +pcb.getId());
            // 4- chamar o escalonador
        escalona();
    }

    // fim do processo que está rodando (STOP ou erro): mostra o resultado, libera e escalona outro
    public void terminaProcesso() {
            // 1- pegar o processo que está rodando
        PCB pcb = rodando;
            // 2- mensagem "ESCALONADOR: processo X terminou"
        System.out.println("ESCALONADOR: termino do processo: " +pcb.getId());
            // 3- salva estado final e mostra o dump
        salvaContexto(pcb);
        pcb.setEstado(EstadoProcesso.TERMINADO);
        dumpProcesso(pcb.getId());
            // 4- liberar memória e PCB
        desalocaProcesso(pcb.getId());
            // 5- chamar o escalonador
        escalona();
    }





}
