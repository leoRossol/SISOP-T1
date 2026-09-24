package so;

import hardware.HW;

// ------------------- S O - reune as rotinas do sistema operacional
public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;

    public SO(HW hw) {
        ih = new InterruptHandling(hw); // rotinas de tratamento de int
        sc = new SysCallHandling(hw); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
        utils = new Utilities(hw);
    }
}
