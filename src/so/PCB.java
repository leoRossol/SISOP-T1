package so;

public class PCB {
    private int id;
    private int pc;
    private int[] reg;
    private int[] tabelaPaginas;
    private int tamanhoPrograma;
    private EstadoProcesso estado;

    public PCB(int id, int pc, int[] reg, int[] tabelaPaginas, int tamanhoPrograma, EstadoProcesso estado){
        this.id = id;
        this.pc = pc;
        this.reg = reg;
        this.tabelaPaginas = tabelaPaginas;
        this.tamanhoPrograma = tamanhoPrograma;
        this.estado = estado;
    }

    public int getId() { return id; }
    public int getPc() { return pc; }
    public int[] getReg() { return reg; }
    public int[] getTabelaPaginas() { return tabelaPaginas; }
    public int getTamanhoPrograma() { return tamanhoPrograma; }
    public EstadoProcesso getEstado() { return estado; }

    public void setPc(int pc) {this.pc = pc;}
    public void setReg(int[] reg) {this.reg = reg;}
    public void setTabelaPaginas(int[] tabelaPaginas) {this.tabelaPaginas = tabelaPaginas;}
    public void setEstado(EstadoProcesso estado) {this.estado = estado;}

}