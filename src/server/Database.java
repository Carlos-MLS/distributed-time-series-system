package server;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Database {
    private int diaAtual;
    private SerieTemporal serieTemporal;
    private HashMap<Integer, SerieTemporal> diasAnteriores;
    private ReentrantLock lock;  

    //Construtor
    public Database() {
        this.diaAtual = 0;
        this.serieTemporal = new SerieTemporal(0);
        this.diasAnteriores = new HashMap<>();
        this.lock = new ReentrantLock();
    }

    public Database(int diaAtual, SerieTemporal serieTemporal, HashMap<Integer, SerieTemporal> diasAnteriores) {
        this.diaAtual = diaAtual;
        this.serieTemporal = serieTemporal;
        this.diasAnteriores = diasAnteriores;
        this.lock = new ReentrantLock();
    }

    //Getters
    public int getDiaAtual() {
        lock.lock();
        try {
            return this.diaAtual;
        } finally {
            lock.unlock();
        }
    }

    public SerieTemporal getSerieTemporal() {
        lock.lock();
        try {
            return this.serieTemporal;
        } finally {
            lock.unlock();
        }
    }

    public HashMap<Integer, SerieTemporal> getDiasAnteriores() {
        lock.lock();
        try {
            return this.diasAnteriores;
        } finally {
            lock.unlock();
        }
    }

    //Não sei se realmente é preciso isto depois mas vou fazer na mesma para manter a consistência
    public ReentrantLock getLock() {
        return this.lock;
    }

    //Setters provavelmente não iremos usar 
    public void setDiaAtual(int diaAtual) {
        this.diaAtual = diaAtual;
    }

    public void setSerieTemporal(SerieTemporal serieTemporal) {
        this.serieTemporal = serieTemporal;
    }

    public void setDiasAnteriores(HashMap<Integer, SerieTemporal> diasAnteriores) {
        this.diasAnteriores = diasAnteriores;
    }

    //same
    public void setLock(ReentrantLock lock) {
        this.lock = lock;
    }
    
    //toString
    public String toString() {
        return "Database{" +
               "diaAtual=" + diaAtual +
               ", serieTemporal=" + serieTemporal +
               ", diasAnteriores=" + diasAnteriores +
               '}';
    }


    //clone
    public Database clone() {
        return new Database(this.diaAtual, this.serieTemporal.clone(), new HashMap<>(this.diasAnteriores));
    }
  /*-------------------------METODOS------------------------------------ */

    //Adiciona um evento ao dia atual
    public void adicionarEvento(String produto, int quantidade, double preco) {
        this.lock.lock();
        try {
            this.serieTemporal.adicionarEvento(produto, quantidade, preco);
        } finally {
            this.lock.unlock();
        }
    }

    //Avança para o próximo dia
    public void novoDia() {
        this.lock.lock();
        try {
            this.diasAnteriores.put(this.diaAtual, this.serieTemporal);
            this.diaAtual++;
            this.serieTemporal = new SerieTemporal(this.diaAtual);
        } finally {
            this.lock.unlock();
        }
    }

}
