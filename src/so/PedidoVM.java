// [T2B] Pedido de leitura ou gravacao de uma pagina no disco
package so;

public class PedidoVM {
    public static final int CARREGAR = 1;
    public static final int SALVAR = 2;

    private int idProcesso;
    private int tipo;
    private int pagina;
    private int frameMemoria;
    private int frameDisco;

    public PedidoVM(int _idProcesso, int _tipo, int _pagina,
                    int _frameMemoria, int _frameDisco) {
        idProcesso = _idProcesso;
        tipo = _tipo;
        pagina = _pagina;
        frameMemoria = _frameMemoria;
        frameDisco = _frameDisco;
    }

    public int getIdProcesso() { return idProcesso; }
    public int getTipo() { return tipo; }
    public int getPagina() { return pagina; }
    public int getFrameMemoria() { return frameMemoria; }
    public int getFrameDisco() { return frameDisco; }
}
