package common;
import java.io.DataOutputStream;
import java.io.IOException;


public class Evento {
    //Serve para guardar a informação de um evento específico
    private String produto;
    private int quantidade;
    private double preco;
    private long timestamp; // pode ser usado útil para ordenação 

    //Construtores
    public Evento() {
        this.produto = "";
        this.quantidade = 0;
        this.preco = 0.0;
        this.timestamp = System.currentTimeMillis();
    }

    public Evento(String produto, int quantidade, double preco) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.preco = preco;
        this.timestamp = System.currentTimeMillis();
    }

    //Getters
    public String getProduto() {
        return this.produto;
    }

    public int getQuantidade() {
        return this.quantidade;
    }

    public double getPreco() {
        return this.preco;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    //Setters
    public void setProduto(String produto) {
        this.produto = produto;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    //toString
    public String toString() {
        return "Evento{" +
               "produto='" + produto + '\'' +
               ", quantidade=" + quantidade +
               ", preco=" + preco +
               ", timestamp=" + timestamp +
               '}';
    }

    //clone
    public Evento clone() {
        return new Evento(this.produto, this.quantidade, this.preco);
    }


    //adicionamos agora o serialize e deserialize para poder gravar 
    //em files os dados de eventos para a fase de persistência na fase 7

    public void serialize(DataOutputStream out) throws IOException {
        out.writeUTF(produto);
        out.writeInt(quantidade);
        out.writeDouble(preco);
        out.writeLong(timestamp);
    }

    //
    public static Evento deserialize(java.io.DataInputStream in) throws IOException {
        String produto = in.readUTF();
        int quantidade = in.readInt();
        double preco = in.readDouble();
        long timestamp = in.readLong();
        Evento evento = new Evento(produto, quantidade, preco);
        evento.setTimestamp(timestamp); //temos de manter o timestamp original
        return evento;
    }

}

