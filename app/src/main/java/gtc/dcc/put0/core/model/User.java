package gtc.dcc.put0.core.model;

public class User {
    private String id;
    private String emailAddress;
    private String userPassword;
    private String names;
    private String surNames;
    private String userName;
    private String state;
    private Rol rol; // Enum for user roles

    // Empty constructor
    public User() {}

    public User(String names) {
        this.names = names;
    }

    public User(String names, Rol rol) {
        this.names = names;
        this.rol = rol;
    }

    public User(String id, String emailAddress, String userPassword, String names, String surNames, String userName, String state, Rol rol) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.userPassword = userPassword;
        this.names = names;
        this.surNames = surNames;
        this.userName = userName;
        this.state = state;
        this.rol = rol;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getSurNames() {
        return surNames;
    }

    public void setSurNames(String surNames) {
        this.surNames = surNames;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }
}
