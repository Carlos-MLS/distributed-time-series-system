package server;
import common.Evento;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;



public class SerieTemporal {
//Serve para guardar todos os eventos de um dia específico

    //ainda n sei se será preciso usar isto ou se passamos o path diretamente na func
    private String PATHTOSAVE = "data/seriestemporais/";
    
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

    //precisamos de guardar a serie temporal de eventos em files agora para a fase de persistência na fase 7
    public void saveToFile(String path) throws IOException {
        lockST.lock();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(fileOutputStream));

            //escrever o número de eventos primeiro
            output.writeInt(eventos.size());

            for(Evento ev: eventos) {
                ev.serialize(output); //criamos o serialize na classe Evento
            }

            output.flush();
            output.close();

        } finally {
            lockST.unlock();
        }
    }


    //dar load dos dados agora de um file para a série temporal
    //n usamos lock aqui porque é um método estático que cria uma nova série temporal
    public static SerieTemporal loadFromFile(String path, int dia) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(path);
        DataInputStream input = new DataInputStream(new BufferedInputStream(fileInputStream));

        SerieTemporal serieTemporal = new SerieTemporal(dia);

        //ler o num de events
        int numeroEventos = input.readInt();

        for(int i = 0; i < numeroEventos; i++) {
            Evento ev = Evento.deserialize(input); //criamos o deserialize na classe Evento
            serieTemporal.eventos.add(ev);
        }
        input.close();
        return serieTemporal;
    }
    //quando já temos S séries em memória, processamos a série sem carregar tudo 
    public static int contarQuantidadeProdutoStreaming (String path, String produtoNome) throws IOException {
        int count = 0;
        FileInputStream fileInputStream = new FileInputStream(path);
        DataInputStream input = new DataInputStream(new BufferedInputStream(fileInputStream));

        int numEventos = input.readInt();

        for(int i = 0; i < numEventos; i++) {
            Evento ev = Evento.deserialize(input);
            if(ev.getProduto().equals(produtoNome)) {
                count += ev.getQuantidade();
            }
            //não adicionamos o evento à lista, só processamos, ou seja poupamos memória
        }
        input.close();
        return count;
    }
}