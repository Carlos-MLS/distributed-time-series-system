package server;
import common.User;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class UserManager {
    private HashMap<String, User> users; // Guarda os users em que a chave é o username

    private ReentrantLock lock = new ReentrantLock();

    //Construtor vazio
    public UserManager() {
        this.users = new HashMap<>();
    }

    //Construtor com parametros
    public UserManager(HashMap<String, User> users) {
        this.users = users;
    }

    //Getters
    public HashMap<String, User> getUsers() {
        return this.users;
    }

    //Setters
    public void setUsers(HashMap<String, User> users) {
        this.users = users;
    }

    //toString
    public String toString() {
        return "UserManager{" +
               "users=" + users +
               '}';
    }

    //clone
    public UserManager clone() {
        return new UserManager((HashMap<String, User>) this.users.clone());
    }

    //equals
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        UserManager um = (UserManager) o;
        return this.users.equals(um.getUsers());
    }

    /*------------------------Métodos para a classe------------------------*/

    //É uma função auxiliar que verifica se um user existe na users, não estamos a usar locks porque só será
    //chamada dentro de métodos que já usam locks
    private boolean userExists(String username) {
        return this.users.containsKey(username);
    } 

    //FUnção para registar um user, adicionando-o ao hashmap, agora já usamos locks porque é um método público
    //que pode ser chamado por várias threads ao mesmo tempo visto que no enunciado diz que o server é concorrente
    public boolean registarUser(User user) {
        lock.lock();
        try {
            if(userExists(user.getUsername())) {
                return false; // User já existe
            } 
            else {
                User novoUser = new User(user.getUsername(), user.getPassword());
                users.put(user.getUsername(), novoUser);
                return true; // Registou corretamente
            }
        } finally {
            lock.unlock();
        }
    }

    //Função para validar um user, verifica se o user existe e se a password está correta
    public boolean validarUser(String username, String password) { 
        lock.lock();
        try {
            User user = users.get(username);
            if(user == null) { 
                //não encontrou no hashmap
                return false;
            }
            if(!user.getPassword().equals(password)) {
                return false;
                //A pass do user não corresponde à passe guardada
            }
            return true; // Se não der returns em cima é porque o user é válido
        } finally {
            lock.unlock();
        }
    }

    //Só usamos esta função para verificar o número de users registados no sistema em testes
    public int getQtdUsers() {
        lock.lock();
        try {
            return users.size();
        } finally {
            lock.unlock();
        }
    }
}