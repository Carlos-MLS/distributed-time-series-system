package common;

public class User {
    private String username;
    private String password;
    
    //Construtor vazio
    public User() {
        this.username = "";
        this.password = "";
    }

    //Construtor com parametros
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    //Getters
    public String getUsername() {
        return this.username;
    }
    public String getPassword() {
        return this.password;
    }

    //Setters
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    //toString
    public String toString() {
        return "User{" + 
               "username='" + username + '\'' +
               ", password='" + password + '\'' +
               '}';
    }

    //clone
    public User clone() {
        return new User(this.username, this.password);
    }

    //equals
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return this.username.equals(user.getUsername()) &&
               this.password.equals(user.getPassword());
    }



}