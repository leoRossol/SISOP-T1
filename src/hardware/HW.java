package hardware;

// ------------------- HW - constituido de CPU e MEMORIA
public class HW {
    public Memory mem;
    public CPU cpu;

    public HW(int tamMem, int _tamPg) {     // [T1A] recebe tamanho de página
        mem = new Memory(tamMem);
        cpu = new CPU(mem, _tamPg, true); // true liga debug  // [T1A] repassa tamPg para a CPU
    }
}
