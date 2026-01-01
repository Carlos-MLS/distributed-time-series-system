package client;
import common.Demultiplexer;
import common.Evento;
import common.TaggedConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import protocol.Message;


public class ClientLib {
    private Demultiplexer demultiplexer;
    private int tagNumber = 0;
    private ReentrantLock tagLock = new ReentrantLock();

    //Construtor vazio
    public ClientLib() {
        this.demultiplexer = null;
    }

    //Metodo construtor que cria a socket e os streams de input e output, é chamado no clientInterativo
    public ClientLib(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        TaggedConnection conn = new TaggedConnection(socket);
        this.demultiplexer = new Demultiplexer(conn);
        this.demultiplexer.start(); //incia a thread para ler
    }

    //agr uma func para gerar as tags
    private int getTagNumber() {
        tagLock.lock();
        try {
            return tagNumber++;
        } finally {
            tagLock.unlock();
        }
    }


    //Envia para o servidor o pedido de registo de um novo user
    public boolean registar(String username, String password) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.REGISTAR);
        output.writeUTF(username);
        output.writeUTF(password);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte();
        String mensagem = input.readUTF();
        System.out.println(mensagem);

        return status == Message.STATUS_SUCCESSO;
    }

    //Envia para o servidor o pedido de login de um user
    public boolean login(String username, String password) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.LOGIN);
        output.writeUTF(username);
        output.writeUTF(password);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte();
        String mensagem = input.readUTF();
        System.out.println(mensagem);
        return status == Message.STATUS_SUCCESSO;
    }

    //Fecha a socket
    public void close() throws IOException {
        if (demultiplexer != null) {
            demultiplexer.close();
        }
    }
    
    //
    public boolean adicionarEvento(String produto, int quantidade, double preco) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.ADICIONAR_EVENTO);
        output.writeUTF(produto);
        output.writeInt(quantidade);
        output.writeDouble(preco);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte();

        return status == Message.STATUS_SUCCESSO;
    }

    //Avança para o próximo dia, mesma cena do database mas temos aqui no client porque o client interativo chama este método
    public boolean novoDia() throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.NOVO_DIA);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte();

        return status == Message.STATUS_SUCCESSO;
    }
    //fase de agregação

    //mesma cena do database
    public int quantidadeVendas(String produto, int dias) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.QUANTIDADE_VENDAS);
        output.writeUTF(produto);
        output.writeInt(dias);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));
        
        byte status = input.readByte();
        if (status != Message.STATUS_SUCCESSO) {
            System.out.println("Erro ao consultar quantidade");
            return -1;
        }
        
        int resultado = input.readInt();
        return resultado;
    }

    public double volumeVendas(String produto, int dias) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.VOLUME_VENDAS);
        output.writeUTF(produto);
        output.writeInt(dias);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1.0;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));
        
        byte status = input.readByte();
        if (status != Message.STATUS_SUCCESSO) {
            System.out.println("Erro ao consultar volume");
            return -1.0;
        }
        
        double resultado = input.readDouble();
        return resultado;
    }

    public double precoMedio(String produto, int dias) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.PRECO_MEDIO);
        output.writeUTF(produto);
        output.writeInt(dias);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1.0;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));
        
        byte status = input.readByte();
        if (status != Message.STATUS_SUCCESSO) {
            System.out.println("Erro ao consultar preço médio");
            return -1.0;
        }
        
        double resultado = input.readDouble();
        return resultado;
    }

    public double precoMaximo(String produto, int dias) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.PRECO_MAXIMO);
        output.writeUTF(produto);
        output.writeInt(dias);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1.0;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));
        
        byte status = input.readByte();
        if (status != Message.STATUS_SUCCESSO) {
            System.out.println("Erro ao consultar preço máximo");
            return -1.0;
        }
        
        double resultado = input.readDouble();
        return resultado;
    }



    //fase de filtrar eventos de series temporais

    //envia para o server o pedido de filtrar eventos, com um set de produtos e o dia 
    public List<Evento> filtrarEventos(Set<String> produtos, int dia) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.FILTRAR_EVENTOS);
        output.writeInt(produtos.size());
        for (String produto : produtos) {
            output.writeUTF(produto);
        }
        output.writeInt(dia);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte();
        if (status != Message.STATUS_SUCCESSO) {
            return new ArrayList<>();
        }

        //lê a lista de eventos filtrados que o server enviou de volta
        int numProdutos = input.readInt();
        String[] arrayProdutos = new String[numProdutos];
        for (int i = 0; i < numProdutos; i++) {
            arrayProdutos[i] = input.readUTF();
        }

        int numEventos = input.readInt();
        List<Evento> eventosFiltrados = new ArrayList<>();
        for (int i = 0; i < numEventos; i++) {
            int indProduto = input.readInt();
            int qtd = input.readInt();
            double preco = input.readDouble();
            long timestamp = input.readLong();

            eventosFiltrados.add(new Evento(arrayProdutos[indProduto], qtd, preco));
        }
        return eventosFiltrados;
    }


    //fase de notificação ocorrências, a de vendas simultaneas e consecutivas

    //retorna true se houve vendas simultaneas dos dois produtos
    public boolean aguardarVendasSimultaneas(String produto1, String produto2) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.VENDAS_SIMULTANEAS);
        output.writeUTF(produto1);
        output.writeUTF(produto2);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte(); 
        if(status != Message.STATUS_SUCCESSO) return false;
        boolean result = input.readBoolean();
        return result;
    }

    //retorna o nome do produto que atingiu o número de vendas consecutivas
    public String aguardarVendasConsecutivas(int n) throws IOException {
        int tag = getTagNumber();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(byteArrayOutputStream);

        output.writeByte(Message.VENDAS_CONSECUTIVAS);
        output.writeInt(n);
        output.flush();

        demultiplexer.send(tag, byteArrayOutputStream.toByteArray());

        byte[] respostaBytes = null;
        try {
            respostaBytes = demultiplexer.receive(tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(respostaBytes));

        byte status = input.readByte();
        if(status != Message.STATUS_SUCCESSO) return null; 
        
        boolean hasResult = input.readBoolean();
        if(!hasResult) return null;
        String produtoNome = input.readUTF();
        return produtoNome;
    }
}
