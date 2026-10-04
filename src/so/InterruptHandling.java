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
        } else {gp.terminaProcesso();}
    }
}
