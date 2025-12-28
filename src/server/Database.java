package server;
import common.Evento;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.io.File;


public class Database {

    private int diaAtual = 0;
    private SerieTemporal serieTemporal = new SerieTemporal(0);
    private HashMap<Integer, SerieTemporal> diasAnteriores = new HashMap<>();
    private HashMap<String, Produto> produtos = new HashMap<>(); // key é o nome do produto
    private ReentrantLock lock = new ReentrantLock();  
    //conditions para  a fase 5 de notificações
    Condition condVendasSimultaneas = lock.newCondition();
    Condition condVendasConsecutivas = lock.newCondition();


    //parâmetros para a fase 7 de persistência
    private String PATHDADOS = "data/series/";
    private int D = 30; //n dias mantidos, metemos o default a 30
    private int S = 5; //máximo de séries temporais em memória, default = 5
    private Set<Integer> diasEmDisco = new HashSet<>(); //para saber que dias estão em disco
    private LinkedHashMap<Integer, SerieTemporal> memoriaCache; //inicializado no construtor


    //Construtor vazio
    public Database() {
        this.diaAtual = 0;
        this.serieTemporal = new SerieTemporal(0);
        this.diasAnteriores = new HashMap<>();
        this.produtos = new HashMap<>();
        // Criar diretório se não existir
        new File(PATHDADOS).mkdirs();
        // Inicializar cache LRU
        inicializarCache();
    }
    
    //Construtor parametrizado
    public Database(int d, int s) {
        this.D = d;
        this.S = s;
        this.diaAtual = 0;
        this.serieTemporal = new SerieTemporal(0);
        this.diasAnteriores = new HashMap<>();
        this.produtos = new HashMap<>();
        inicializarCache();
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


    //clone - removido porque não faz sentido copiar toda a database com D, S e cache LRU

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
    
    //Avança para o próximo dia, e grava o dia atual em disco (devido à fase 7)
    public void novoDia() {
        lock.lock();
        try {
            // Persistir dia atual antes de avançar
            String caminhoArquivo = PATHDADOS + "dia_" + diaAtual + ".dat";
            serieTemporal.saveToFile(caminhoArquivo);
            
            // Adicionar à cache de memória 
            diasAnteriores.put(diaAtual, serieTemporal);
            memoriaCache.put(diaAtual, serieTemporal);
            diasEmDisco.add(diaAtual);
            
            // Invalidar cache de produtos já que há um novo dia
            for (Produto p : produtos.values()) {
                p.invalidarCache();
            }
            
            //limpámos as vendas da fase 5
            vendasSimultaneas.clear();
            vendasConsecutivas.clear();
            ultimoProdutoVendido = null;
            counterConsecutivo = 0;
            
            // Avançamos para um novo dia
            diaAtual++;
            serieTemporal = new SerieTemporal(diaAtual);
            
            //acordamos as threads à espera
            condVendasConsecutivas.signalAll();
            condVendasSimultaneas.signalAll();
        } catch (IOException e) {
            System.err.println("deu erro aqui no database testeaaa");
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    //return da quantidade de unidadades do produtoNome nos últimos  dias
    public int quantidadeVendas(String produtoNome, int dias) {
        lock.lock(); 
        try{
            Produto produto = criarOuObterProduto(produtoNome);
            int diaAtualAux = diaAtual;

            int diasProcessados = 0;
            List<SerieTemporal> seriesT = new ArrayList<>();

            for (int d = diaAtual - 1; d >= 0 && diasProcessados < dias; d--) {
                try {
                    //obtemos a série temporal do dia d, se existir adicionamos à lista e incrementamos o contador
                    SerieTemporal serieT = obterSerieTemporal(d);
                    if (serieT != null) {
                        seriesT.add(serieT);
                        diasProcessados++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            // Se  nós já temos S séries em memória e precisamos de mais dados,
            // usamos o streaming para processar do disco sem carregar
            if (diasEmDisco.size() >= S && diasProcessados < dias) {
                // Processamos os dias restantes diretamente do disco
                int totalDisco = 0;
                for (int d = diaAtual - diasProcessados - 1; d >= 0 && diasProcessados < dias; d--) {
                    String path = PATHDADOS + "dia_" + d + ".dat";
                    File file = new File(path);
                    
                    if (file.exists()) {
                        try {
                            totalDisco += SerieTemporal.contarQuantidadeProdutoStreaming(
                                produtoNome, path);
                            diasProcessados++;
                        } catch (IOException e) {
                            System.err.println("Erro streaming dia " + d + ": " + e.getMessage());
                        }
                    }
                }
                
                // combinamos o resultado das séries em memória mais os dos dias em disco
                return produto.quantidadeVendas(seriesT, seriesT.size()) + totalDisco;
            }
            //O produto é que vai iterar sobre a seriesT que passamos como arg, n é mt lógico mas funciona
            //return aqui se todas as séries temporais já estão em memória
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
            //QUANDO SAIR AQUI FOI PORQUE CHEGOU AO OBJETIVO OU O DIA TERMINOU
            vendasConsecutivas.remove(pedidoVendaConsecutiva);
            return pedidoVendaConsecutiva.nomeProduto;
        } finally {
            lock.unlock();
        }
    }


    //para a fase 7 de persistência dos dados, temos estas duas funcs e atualizamos a novoDia()

    // Método auxiliar para inicializar a cache lru
    // Não precisa de lock porque é chamado apenas nos construtores antes da publicação do objeto
    //1º param da linkedhashMap  é a capacidade inicial, 2º é o load factor (0.75f é o default, se estiver 0.75 cheio da resize), 3º é true se queremos que se comporto como LRU
    private void inicializarCache() {
        this.memoriaCache = new LinkedHashMap<Integer, SerieTemporal>(S, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, SerieTemporal> eldest) {
                if (size() > S) {
                    //se ultrapassar S, salvamos a série temporal mais antiga em disco
                    try {
                        int diaRemover = eldest.getKey();
                        SerieTemporal serieRemover = eldest.getValue();
                        serieRemover.saveToFile(PATHDADOS + "serie_dia_" + diaRemover + ".dat");
                        diasEmDisco.remove(diaRemover); //removemos dos dias em disco porque já salvamos
                        //para debug verificar se salvou direito
                        System.out.println("Série temporal do dia " + diaRemover + " salva em disco.");
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return false;
            }
        };
    }

    //função para obter série temporal de um dia, seja da cache ou do disco
    private SerieTemporal obterSerieTemporal(int dia) throws IOException {
        //verificamos se está na memória 
        if (memoriaCache.containsKey(dia)) {
            return memoriaCache.get(dia); //assim tbm atualiza a ordem de acesso
        }
            
        //verificamos se está no hashmap antigo diasAnteriores (antes de termos persistência)
        if (diasAnteriores.containsKey(dia)) {
            SerieTemporal serie = diasAnteriores.get(dia);
            memoriaCache.put(dia, serie); //adicionamos à cache
            diasEmDisco.add(dia);
            return serie;
        }


        //carregamos do disco 
        String caminhoArquivo = PATHDADOS + "dia_" + dia + ".dat";
        File arquivo = new File(caminhoArquivo);

        if (!arquivo.exists()) {
            return null; // Dia não existe
        }

        SerieTemporal serieCarregada = SerieTemporal.loadFromFile(caminhoArquivo, dia);
        memoriaCache.put(dia, serieCarregada); //adicionamos à cache
        diasEmDisco.add(dia);
        return serieCarregada;
    }
}


