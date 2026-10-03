package so;

import java.util.ArrayList;
import java.util.List;
import hardware.Word;

public class GP {
    private GM gm;
    private Utilities utils;
    private List<PCB> processos;
    private List<PCB> prontos;
    private PCB rodando;
    private int proximoId;

    public GP(GM _gm, Utilities _utils){
        gm = _gm;
        utils = _utils;
        processos = new ArrayList<>();
        prontos = new ArrayList<>();
        rodando = null;
        proximoId = 0;
    }

    public boolean criaProcesso(Word[] programa){
        if (programa == null){ return false; } // verifica se o programa é válido
        
        int[] tabela = utils.loadProgram(programa);
        if(tabela == null){ return false; } // se não houver memória, loadProgram retorna null
        
        int[] reg = new int[10];
        PCB pcb = new PCB(
            proximoId,
            0,
            reg,
            tabela,
            programa.length,
            EstadoProcesso.PRONTO
        );

        processos.add(pcb);
        prontos.add(pcb);
        proximoId++;

        return true;
    }

    public PCB buscarProcesso(int id){
        for(PCB pcb : processos){
            if(pcb.getId() == id){
                return pcb;
            }
        }
        return null;
    }

    public boolean desalocaProcesso(int id){
        PCB pcb = buscarProcesso(id);

        if(pcb == null){ return false; }

        gm.desaloca(pcb.getTabelaPaginas()); // libera memória alocada ao processo
        processos.remove(pcb);
        prontos.remove(pcb);

        if(rodando == pcb){ rodando = null; } //verifica processo rodando

        return true;
    }

    public List<PCB> getProcessos(){ return processos; }

    public void mostraProcessos() {
    System.out.println("ID\tESTADO\t\tPC");

    for (PCB pcb : processos) {
        System.out.println(
            pcb.getId() + "\t"
            + pcb.getEstado() + "\t"
            + pcb.getPc()
        );
    }
}


}
