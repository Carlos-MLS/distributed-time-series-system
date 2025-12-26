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
    //conditions para  a fase 5 de notificações
    Condition condVendasSimultaneas = lock.newCondition();
    Condition condVendasConsecutivas = lock.newCondition();

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

    //fase de notificação ocorrências, a que temos barreira
    class VendaSimultanea {
        private String p1, p2;
        boolean diaTerminou = false;
        boolean p1Vendido = false, p2Vendido = false;
        boolean p1ep2Vendidos = false;

        public VendaSimultanea(String p1, String p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }
    //para guardar as vendas simultaneas
    List<VendaSimultanea> vendasSimultaneas = new ArrayList<>();


    class VendaConsecutiva {
        private int quantidadeVendasConsecutivas = 0;
        String nomeProduto = null;
        boolean diaTerminou = false;
        boolean chegouObjetivo = false;

        public VendaConsecutiva(int n) {
            this.quantidadeVendasConsecutivas = n;
        }

    }
    List<VendaConsecutiva> vendasConsecutivas = new ArrayList<>();


    String ultimoProdutoVendido = null;
    int counterConsecutivo = 0;

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

            //agora temos de adicionar aqui para a fase 5  
            //vendas simultaneas
            for(VendaSimultanea vs : vendasSimultaneas) {
                    if(produto.equals(vs.p1)) {
                        vs.p1Vendido = true;
                    }
                    if(produto.equals(vs.p2)) {
                        vs.p2Vendido = true;
                    }
                    if(vs.p1Vendido && vs.p2Vendido) {
                        vs.p1ep2Vendidos = true;
                        condVendasSimultaneas.signalAll(); //quando ambos vendidos acorda todos
                    }
            }

            //para vendas consecutivas
            if(produto.equals(ultimoProdutoVendido)) {
                counterConsecutivo++;
            } else {
                ultimoProdutoVendido = produto;
                counterConsecutivo = 1;
            }

            //verifica se algum pedido de vendas consecutivas chegou ao objetivo
            for(VendaConsecutiva vc : vendasConsecutivas) {
                if(counterConsecutivo >= vc.quantidadeVendasConsecutivas) {
                        vc.nomeProduto = produto;
                        vc.chegouObjetivo = true;
                        condVendasConsecutivas.signalAll(); //acorda todos
                }
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

            //aqui temos de cancelar os pedidos de vendas simultaneas e consecutivas porque o dia terminou
            for(VendaSimultanea vs : vendasSimultaneas) {
                vs.diaTerminou = true;
            }
            //como a condição é atingir o objetivo ou o dia terminar, acordamos todos aqui tbm
            condVendasSimultaneas.signalAll(); //acorda todos
            for(VendaConsecutiva vc : vendasConsecutivas) {
                vc.diaTerminou = true;
            }
            condVendasConsecutivas.signalAll(); //acorda todos

            //reset das vendas consecutivas
            ultimoProdutoVendido = null;
            counterConsecutivo = 0;
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



    //fase 5 de notificação ocorrências
    public boolean vendasSimultaneas(String produto1, String produto2) throws InterruptedException {
        lock.lock();
        try {
            //criamos pedido
            VendaSimultanea pedidoVendaSimultanea = new VendaSimultanea(produto1,produto2);

            //primeiro vemos se já existe foram vendidos hoje 
            for(Evento ev: serieTemporal.obterEventos()) {
                if(ev.getProduto().equals(produto1)) {
                    pedidoVendaSimultanea.p1Vendido = true;
                }
                if(ev.getProduto().equals(produto2)) {
                    pedidoVendaSimultanea.p2Vendido = true;
                }
            }
            //se foram ambos vendidos retornamos true
            if(pedidoVendaSimultanea.p1Vendido && pedidoVendaSimultanea.p2Vendido) {
                return true;
            }

            //senão adicionamos à lista de pedidos ativos e esperamos
            vendasSimultaneas.add(pedidoVendaSimultanea);

            //esperamos até sermos notificados
            while(!pedidoVendaSimultanea.p1ep2Vendidos && !pedidoVendaSimultanea.diaTerminou) {
                condVendasSimultaneas.await();
            }

            //se chegar aqui o dia terminou ou foram vendidos ambos
            vendasSimultaneas.remove(pedidoVendaSimultanea);
            return pedidoVendaSimultanea.p1ep2Vendidos; //true se foram vendidos ambos, false se dia terminou
        } finally {
            lock.unlock();
        }
    }

    //Mesma cena para vendas consecutivas
    public String vendasConsecutivas(int n) throws InterruptedException {
        lock.lock();
        try {
            //já aconteceu isto hoje?
            if(counterConsecutivo >= n) {
                return ultimoProdutoVendido;
            }

            VendaConsecutiva pedidoVendaConsecutiva = new VendaConsecutiva(n);
            vendasConsecutivas.add(pedidoVendaConsecutiva);

            while(!pedidoVendaConsecutiva.chegouObjetivo && !pedidoVendaConsecutiva.diaTerminou) {
                condVendasConsecutivas.await();
            }
            //QUANDO SAIR AQUI FOI PORQUE CHEGOU AO OBJETIVO OU O DIA TERMINO
            vendasConsecutivas.remove(pedidoVendaConsecutiva);
            return pedidoVendaConsecutiva.nomeProduto;
        } finally {
            lock.unlock();
        }
    }


}

