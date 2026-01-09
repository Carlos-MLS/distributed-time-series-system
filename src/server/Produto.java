package server;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


class Produto {
    // Agregações Lazy com Cache
    // Cada produto mantém uma cache própria das suas agregações (quantidade, volume,  epreço médio/máximo)
    // A Cache é calculada on-demand na primeira consulta e reutilizada em consultas subsequentes que fazemos depois
    // A cache é invalidada quando um novo dia começav ou o evento é adicionado ao dia atual

    private String nome; // Identificador único do produto (ex: "Laptop", "Mouse")
    private HashMap<String, Double> cache = new HashMap<>(); // Chave: "tipo:dias" (ex: "quantidade:15")
                                                              // Valor: resultado agregado calculado
    private ReentrantLock lockProduto = new ReentrantLock(); // Lock para acesso concorrente à cache
    
    public Produto(String nome) {
        this.nome = nome;
    }
    
    // Cache específico deste produto
    public int quantidadeVendas(List<SerieTemporal> series, int dias) {
        String key = "quantidade:" + dias;
        
        lockProduto.lock();
        try {
            //pensei em usar só o String.valueOf(dias) mas ia dar conflito por causa dos tipos de agregação.
            //
            if (cache.containsKey(key)) {
                return cache.get(key).intValue();
            }
            
            // Calcula fora do lock
        } finally {
            lockProduto.unlock();
        }
        //aqui podemos calcular fora porque já colocamos locks na serieTemporal
        int total = 0;
        for (SerieTemporal s : series) {
            total += s.contarQuantidadeProduto(nome);
        }   
        
        lockProduto.lock();
        try {
            cache.put(key, (double)total);
            return total;
        } finally {
            lockProduto.unlock();
        }
    }
    
    public double volumeVendas(List<SerieTemporal> series, int dias) {
        String key = "volume:" + dias;
        lockProduto.lock();
        try {
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
        } finally {
            lockProduto.unlock();
        }
        double total = 0.0;
        //já podemos calcular fora porque já colocamos locks na serieTemporal
        for (SerieTemporal s : series) {
            total += s.calcularVolumeProduto(nome);
        }
        lockProduto.lock();
        try {
            cache.put(key, total);
            return total;
        } finally {
            lockProduto.unlock();
        }
    }
    // Calcula o preço médio de um produto nos últimos int dias
    public double precoMedio(List<SerieTemporal> series, int dias) {
        String key = "preco_medio:" + dias;
        lockProduto.lock();
        try {
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
        } finally {
            lockProduto.unlock();
        }
        
        // Calcula: volume total / quantidade total
        double volume = 0.0;
        int quantidade = 0;
        for (SerieTemporal s : series) {
            volume += s.calcularVolumeProduto(nome);
            quantidade += s.contarQuantidadeProduto(nome);
        }
        double media = (quantidade > 0) ? (volume / quantidade) : 0.0;
        lockProduto.lock();
        try {
            cache.put(key, media);
            return media;
        } finally {
            lockProduto.unlock();
        }
    }

    public double precoMaximo(List<SerieTemporal> series, int dias) {
        String key = "preco_maximo:" + dias;
        lockProduto.lock();
        try {
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
        } finally {
            lockProduto.unlock();
        }
        double max = -1.0;
        for (SerieTemporal s : series) {
            double maxDia = s.precoMaximoProduto(nome);
            if (maxDia > max) {
                max = maxDia;
            }
        }
        lockProduto.lock();
        try {
            cache.put(key, max);
            return max;
        } finally {
            lockProduto.unlock();
        }
    }
    public void invalidarCache() { //a cache é invalidado quando um novo evento é adicionado ou seja haja um novo dia
        lockProduto.lock();
        try {
            cache.clear();
        } finally {
            lockProduto.unlock();
        }
    }
}
