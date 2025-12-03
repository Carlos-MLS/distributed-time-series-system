package server;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import common.Evento;


public class SerieTemporal {
    private int dia;
    private List<Evento> eventos;
    private ReentrantLock lock;
    

    public SerieTemporal(int dia) {
        this.dia = dia;
        this.eventos = new ArrayList<>();
        this.lock = new ReentrantLock();
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
        lock.lock();
        try {
            Evento evento = new Evento(produto, quantidade, preco);
            this.eventos.add(evento);
        } finally {
            lock.unlock();
        }
    }

    // Obtém uma cópia da lista de eventos
    public List<Evento> obterEventos() {
        lock.lock();
        try {
            return new ArrayList<>(this.eventos);
        } finally {
            lock.unlock();
        }
    }

    public int getNumeroEventos() {
        lock.lock();
        try {
            return this.eventos.size();
        } finally {
            lock.unlock();
        }
    }
}
