package server;
import common.Evento;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Database {
    private int diaAtual = 0;
    private SerieTemporal serieTemporal = new SerieTemporal(0);
    private HashMap<Integer, SerieTemporal> diasAnteriores = new HashMap<>();
    private HashMap<String, Produto> produtos = new HashMap<>(); // key é o nome do produto
    private ReentrantLock lock = new ReentrantLock();  
    private Condition cond = lock.newCondition();

    //Construtor vazio
    public Database() {
        this.diaAtual = 0;
        this.serieTemporal = new SerieTemporal(0);
        this.diasAnteriores = new HashMap<>();
        this.produtos = new HashMap<>();
    }
    
    //Construtor
    public Database(int diaAtual, SerieTemporal serieTemporal, HashMap<Integer, SerieTemporal> diasAnteriores) {
        this.diaAtual = diaAtual;
        this.serieTemporal = serieTemporal;
        this.diasAnteriores = diasAnteriores;
        this.produtos = new HashMap<>();
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
        lock.lock();
        try {
            serieTemporal.adicionarEvento(produto, quantidade, preco);
            // Invalidar a cache neste produto quando o dia atual é atualizado
            if(produtos.containsKey(produto)) {
                produtos.get(produto).invalidarCache();
            }
        } finally {
            lock.unlock();
        }
    }

    //Avança para o próximo dia
    public void novoDia() {
        lock.lock();
        try {
            diasAnteriores.put(diaAtual, serieTemporal);
            diaAtual++;
            serieTemporal = new SerieTemporal(diaAtual);
        } finally {
            lock.unlock();
        }
    }

    //Fase de agregação
    //return da quantidade de unidadades do produtoNome nos últimos  dias
    public int quantidadeVendas(String produtoNome, int dias) {
        lock.lock(); 
        try{
            Produto produto = criarOuObterProduto(produtoNome);
            int diaAtualAux = diaAtual;
            List<SerieTemporal> seriesT = new ArrayList<>();

            for (int i = 0; i <= dias; i++) {
                int diaAnt = diaAtualAux - i;
                if(diasAnteriores.containsKey(diaAnt)) {
                    seriesT.add(diasAnteriores.get(diaAnt));
                }
            }   
            //O produto é que vai iterar sobre a seriesT que passamos como arg, n é mt lógico mas funciona
            return produto.quantidadeVendas(seriesT,dias);
        } finally{
            lock.unlock();
        }
    }
    //função aux para obte o produto ou criar se n existir
    private Produto criarOuObterProduto(String nomeProduto) {
            if(produtos.containsKey(nomeProduto)) {
                Produto p  = produtos.get(nomeProduto);
                return p;
            }
            Produto newProduto = new Produto(nomeProduto);
            produtos.put(nomeProduto,newProduto);
            return newProduto;
    }
    // Calcula o volume de vendas de um produto nos últimos int dias
    public double volumeVendas(String produtoNome, int dias) {
        lock.lock();
        try {
            Produto produto = criarOuObterProduto(produtoNome);
            List<SerieTemporal> seriesT = new ArrayList<>();
            
            for (int i = 1; i <= dias; i++) {
                int diaAnt = diaAtual - i;
                if (diasAnteriores.containsKey(diaAnt)) {
                    seriesT.add(diasAnteriores.get(diaAnt));
                }
            }
            return produto.volumeVendas(seriesT, dias);
        } finally {
            lock.unlock();
        }
    }

    // Calcula o preço médio de um produto nos últimos int dias
    public double precoMedio(String produtoNome, int dias) {
        lock.lock();
        try {
            Produto produto = criarOuObterProduto(produtoNome);
            List<SerieTemporal> seriesT = new ArrayList<>();
            
            for (int i = 1; i <= dias; i++) {
                int diaAnt = diaAtual - i;
                if (diasAnteriores.containsKey(diaAnt)) {
                    seriesT.add(diasAnteriores.get(diaAnt));
                }
            }
            return produto.precoMedio(seriesT, dias);
        } finally {
            lock.unlock();
        }
    }
    // Calcula o preço máximo de um produto nos últimos int dias
    public double precoMaximo(String produtoNome, int dias) {
        lock.lock();
        try {
            Produto produto = criarOuObterProduto(produtoNome);
            List<SerieTemporal> seriesT = new ArrayList<>();
            
            for (int i = 1; i <= dias; i++) {
                int diaAnt = diaAtual - i;
                if (diasAnteriores.containsKey(diaAnt)) {
                    seriesT.add(diasAnteriores.get(diaAnt));
                }
            }
            return produto.precoMaximo(seriesT, dias);
        } finally {
            lock.unlock();
        }
    }

    //fase de filtrar eventos de series temporais, logo da return de uma lista de eventos
    public List<Evento> filtrarEventos(Set<String> produtosFiltro, int dia) {
        lock.lock();
        try {
            int diaAnt = diaAtual - dia;
            if (!diasAnteriores.containsKey(diaAnt)) {
                return new ArrayList<>(); //dia n existe
            }

            SerieTemporal serie = diasAnteriores.get(diaAnt);
            List<Evento> todosEventos = serie.obterEventos(); //obtem todos os eventos do dia ant passado antes 

            List<Evento> eventosFiltrados = new ArrayList<>();
            for (Evento ev : todosEventos) {
                if (produtosFiltro.contains(ev.getProduto())) {
                    eventosFiltrados.add(ev);
                }
            }
            return eventosFiltrados;
            
        } finally {
            lock.unlock();
        }
    }
}

