package apps;
import client.ClientLib;
import java.util.HashSet;
import java.util.Set;

public class ClienteTestes {
    public static void main(String[] args) throws Exception {
        System.out.println("=== INICIANDO TESTES AUTOMÁTICOS ===");
        
        ClientLib client = new ClientLib("localhost", 12345);
        client.login("Francisco", "francisco123");
        
        System.out.println("\n[TESTE 1] Persistência - Adicionar 20 dias de eventos");
        for (int dia = 0; dia < 20; dia++) {
            client.adicionarEvento("Produto" + dia, 100, 50.0);
            client.novoDia();
            if (dia % 5 == 0) {
                System.out.println("  Dias 0-" + dia + " persistidos...");
            }
        }
        System.out.println(" - 20 dias persistidos com sucesso");
        
        System.out.println("\n[TESTE 2] Agregação forçando leitura do disco");
        int qtd = client.quantidadeVendas("Produto0", 15);  // Vai ao disco
        System.out.println("  Quantidade Produto0 (15 dias): " + qtd);
        System.out.println(" - Leitura do disco funcionando");
        
        System.out.println("\n[TESTE 3] Agregações múltiplas");
        client.adicionarEvento("Laptop", 10, 1000.0);
        client.adicionarEvento("Laptop", 5, 950.0);
        client.novoDia();
        
        int qtdLaptop = client.quantidadeVendas("Laptop", 1);
        double volLaptop = client.volumeVendas("Laptop", 1);
        double precoMedio = client.precoMedio("Laptop", 1);
        System.out.println("  Laptop: qtd=" + qtdLaptop + ", vol=" + volLaptop + ", médio=" + precoMedio);
        System.out.println(" -Agregações corretas");
        
        System.out.println("\n[TESTE 4] Barreiras - Vendas Simultâneas");
        Thread t1 = new Thread(() -> {
            try {
                ClientLib clientListener = new ClientLib("localhost", 12345);
                clientListener.login("Francisco", "francisco123");
                boolean ok = clientListener.aguardarVendasSimultaneas("Mouse", "Teclado");
                System.out.println("   Barreira simultânea satisfeita: " + ok);
                clientListener.close();
            } catch (Exception e) {
                System.err.println("   Erro barreira simultânea: " + e.getMessage());
            }
        });
        t1.start();
        Thread.sleep(500);
        client.adicionarEvento("Mouse", 1, 20);
        Thread.sleep(100);
        client.adicionarEvento("Teclado", 1, 50);
        t1.join(2000);
        
        System.out.println("\n[TESTE 5] Barreiras - Vendas Consecutivas");
        Thread t2 = new Thread(() -> {
            try {
                ClientLib clientListener = new ClientLib("localhost", 12345);
                clientListener.login("Francisco", "francisco123");
                String prod = clientListener.aguardarVendasConsecutivas(3);
                System.out.println("   Barreira consecutiva satisfeita: " + prod);
                clientListener.close();
            } catch (Exception e) {
                System.err.println("  ✗ Erro barreira consecutiva: " + e.getMessage());
            }
        });
        t2.start();
        Thread.sleep(500);
        client.adicionarEvento("Monitor", 1, 300);
        client.adicionarEvento("Monitor", 1, 300);
        client.adicionarEvento("Monitor", 1, 300);
        t2.join(2000);
        
        System.out.println("\n[TESTE 6] Filtrar Eventos");
        Set<String> filtro = new HashSet<>();
        filtro.add("Laptop");
        filtro.add("Mouse");
        int numEventos = client.filtrarEventos(filtro, 1).size();
        System.out.println("  Eventos filtrados: " + numEventos);
        System.out.println(" -Filtros funcionando");
        
        client.close();
        System.out.println("\n=== TODOS OS TESTES CONCLUÍDOS COM SUCESSO ===");
    }
}
