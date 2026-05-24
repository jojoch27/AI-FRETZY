package com.rplbo.app;

public class Admin {
    private final int adminId;
    private final String username;
    private final String password;

    public Admin(int adminId, String username, String password) {
        this.adminId = adminId;
        this.username = username;
        this.password = password;
    }

    public int getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }

    public boolean login(String inputUsername, String inputPassword) {
        return username.equals(inputUsername) && password.equals(inputPassword);
    }

    public String manageData() {
        return "Admin dapat mengelola data produk dan pesanan.";
    }
}
