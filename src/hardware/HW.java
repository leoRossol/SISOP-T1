package hardware;

// ------------------- HW - constituido de CPU e MEMORIA
public class HW {
    public Memory mem;
    public CPU cpu;

    public HW(int tamMem, int _tamPg) {
        mem = new Memory(tamMem);
        cpu = new CPU(mem, _tamPg, true); // true liga debug
    }
}
