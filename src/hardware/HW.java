package hardware;

// ------------------- HW - constituido de CPU e MEMORIA
public class HW {
    public Memory mem;
    public CPU cpu;

    public HW(int tamMem, int _tamPg, int _delta) {     // [T1A] recebe tamanho de página  // [T1C] recebe delta
        mem = new Memory(tamMem);
        cpu = new CPU(mem, _tamPg, _delta, false); // false: começa sem trace  // [T1A] repassa tamPg para a CPU  // [T1B] antes true; agora o shell liga com traceOn  // [T1C] repassa delta
    }
}
