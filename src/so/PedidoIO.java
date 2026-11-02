// [T2A] Criado pelo grupo - pedido de IO que fica na fila do console
package so;

public class PedidoIO {
    private int idProcesso;
    private int tipo;
    private int endLogico;
    private int[] tabelaPaginas;


    public PedidoIO(int idProcesso, int tipo, int endLogico, int[] tabelaPaginas) {
        this.idProcesso = idProcesso;
        this.tipo = tipo;
        this.endLogico = endLogico;
        this.tabelaPaginas = tabelaPaginas;
    }

    
    public int getIdProcesso() { return idProcesso; }
    public int getTipo() { return tipo; }
    public int getEndLogico() { return endLogico; }
    public int[] getTabelaPaginas() { return tabelaPaginas; }
}
