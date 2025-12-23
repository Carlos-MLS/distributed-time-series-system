package server;
import common.Evento;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;



public class SerieTemporal {
//Serve para guardar todos os eventos de um dia específico

    private int dia;
    private List<Evento> eventos = new ArrayList<>();
    private ReentrantLock lockST = new ReentrantLock();
    

    public SerieTemporal(int dia) {
        this.dia = dia;
    }

    //Getters
    public int getDia() {
        return this.dia;
    }


    //Setters
    public void setDia(int dia) {
        this.dia = dia;
    }

    public void setEventos(List<Evento> eventos) {
        this.eventos = eventos;
    }
    //toString
    public String toString() {
        return "SerieTemporal{" +
               "dia=" + dia +
               ", eventos=" + eventos +
               '}';
    }
    //clone
    public SerieTemporal clone() {
        return new SerieTemporal(this.dia);
    }


/*-------------------------------Métodos agora------------------------------- */

    // Adiciona um evento à série temporal
    public void adicionarEvento (String produto, int quantidade, double preco) {
        lockST.lock();
        try {
            Evento evento = new Evento(produto, quantidade, preco);
            this.eventos.add(evento);
        } finally {
            lockST.unlock();
        }
    }

    // Obtém uma cópia da lista de eventos
    public List<Evento> obterEventos() {
        lockST.lock();
        try {
            return new ArrayList<>(this.eventos);
        } finally {
            lockST.unlock();
        }
    }

    public int getNumeroEventos() {
        lockST.lock();
        try {
            return this.eventos.size();
        } finally {
            lockST.unlock();
        }
    }

    //fase de agregação
    
    // Conta a quantidade vendida de um produto neste dia
    public int contarQuantidadeProduto(String produtoNome) {
        lockST.lock();
        try{
            int total = 0;
            for(Evento ev : eventos) {
                if (ev.getProduto().equals(produtoNome)) {
                    total += ev.getQuantidade();
                }
            }
            return total;
        } finally {
            lockST.unlock();
        }
    }

    // Calcula volume de vendas (quantidade × preço) de um produto neste dia
    public double calcularVolumeProduto(String produtoNome) {
        lockST.lock();
        try {
            double total = 0.0;
            for (Evento ev : eventos) {
                if (ev.getProduto().equals(produtoNome)) {
                    total += ev.getQuantidade() * ev.getPreco(); //calculo
                }
            }
            return total;
        } finally {
            lockST.unlock();
        }
    }

    // Retorna o preço máximo de um produto neste dia (-1 se não houver)
    public double precoMaximoProduto(String produtoNome) {
        lockST.lock();
        try {
            double max = -1.0;
            for (Evento ev : eventos) {
                if (ev.getProduto().equals(produtoNome)) {
                    if (max == -1.0 || ev.getPreco() > max) {
                        max = ev.getPreco();
                    }
                }
            }
            return max;
        } finally {
            lockST.unlock();
        }
    }
}
