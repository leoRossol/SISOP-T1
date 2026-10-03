// [T1B] Criado pelo grupo - Gerente de Processos (cria, busca, remove e lista processos)
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
        hw.cpu.setContext(pcb.getPc(), pcb.getTabelaPaginas());
            //registradores: copiar pcb.getReg() para cpu.reg
        int[] regsProcesso = pcb.getReg();
        for (int i=0; i < regsProcesso.length; i++){
            hw.cpu.reg[i] = regsProcesso[i];
        }

        // 4- rodar a CPU
        hw.cpu.run();

        // 5- salvar o contexto de volta no PCB (pc e registradores)
        pcb.setPc(hw.cpu.pc);
        for (int i=0; i<regsProcesso.length; i++){
            regsProcesso[i] = hw.cpu.reg[i];
        }

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
}
