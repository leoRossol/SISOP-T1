package so;

import hardware.HW;
import hardware.Interrupts;

// ------------------- I N T E R R U P C O E S - rotinas de tratamento
public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private GP gp;

    public InterruptHandling(HW _hw, GP _gp) {
        hw = _hw;
        gp = _gp;
    }

    public void handle(Interrupts irpt) {
        System.out.println(
                "                                               Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
        
        if (irpt == Interrupts.intTempo){
            gp.trocaPorTempo();
        } else if (irpt == Interrupts.intIO) {          // [T2A] fim de IO: quem está rodando continua
            int id = hw.cpu.pegaSinalIO();              // id do processo cujo IO terminou (libera a vaga)
            gp.desbloqueiaProcesso(id);                 // bloqueados -> prontos
            hw.cpu.limpaInterrupcao();                  // sem troca de contexto, ninguém zeraria o irpt
        } else {gp.terminaProcesso();}
    }
}
