package com.rplbo.app;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:sqlite:fretzy_catalog.db";

    public void saveData() {
        setupDatabase();
    }

    public void retrieveData() {
        setupDatabase();
    }

    public void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "brand TEXT, " +
                    "title TEXT, " +
                    "category TEXT DEFAULT 'Acoustic Guitar', " +
                    "description TEXT, " +
                    "image_url TEXT, " +
                    "price_eur REAL, " +
                    "price_idr INTEGER)");
            ensureProductColumns(stmt);
            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "order_number TEXT NOT NULL UNIQUE, " +
                    "status TEXT NOT NULL DEFAULT 'Pending')");
            stmt.execute("CREATE TABLE IF NOT EXISTS chat_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "speaker TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            seedAdminIfNeeded();
            seedSampleOrdersIfNeeded();
            refreshGeneratedProductImages();
            System.out.println("Database Katalog siap.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void importKatalogDariJSON(String namaFile) {
        try {
            if (hasAnyProduct()) {
                return;
            }

            String targetFile = (namaFile == null || namaFile.trim().isEmpty()) ? "guitar_data.json" : namaFile;
            String content = new String(Files.readAllBytes(Paths.get(targetFile)));
            content = content.trim();
            if (content.startsWith("[")) {
                content = content.substring(1);
            }
            if (content.endsWith("]")) {
                content = content.substring(0, content.length() - 1);
            }

            String[] items = content.split("\\},\\s*\\{");
            String sql = "INSERT INTO products(brand, title, category, description, image_url, price_eur, price_idr) VALUES(?,?,?,?,?,?,?)";

            try (Connection conn = DriverManager.getConnection(URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                for (String item : items) {
                    String title = ekstrakJSON(item, "title");
                    item = item.replace("{", "").replace("}", "");
                    String brand = ekstrakJSON(item, "brand");
                    String category = tentukanKategori(title);
                    pstmt.setString(1, brand);
                    pstmt.setString(2, title);
                    pstmt.setString(3, category);
                    pstmt.setString(4, buatDeskripsiProduk(brand, title, category));
                    pstmt.setString(5, gambarDefaultUntukProduk(brand, title, category));
                    pstmt.setDouble(6, Double.parseDouble(ekstrakJSON(item, "price_eur")));
                    pstmt.setInt(7, Integer.parseInt(ekstrakJSON(item, "price_idr")));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                System.out.println("Berhasil memasukkan " + items.length + " gitar ke database.");
            }
        } catch (Exception e) {
            System.err.println("Gagal import JSON: " + e.getMessage());
        }
    }

    public List<Product> findProducts(String keyword) {
        List<Product> products = new ArrayList<Product>();
        String sql = "SELECT id, brand, title, category, description, image_url, price_idr " +
                "FROM products " +
                "WHERE lower(title) LIKE ? " +
                "OR lower(brand) LIKE ? " +
                "OR lower(category) LIKE ? " +
                "OR lower(trim(COALESCE(brand, '') || ' ' || COALESCE(title, ''))) LIKE ? " +
                "LIMIT 5";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase().trim();
            String likeKeyword = "%" + normalizedKeyword + "%";
            pstmt.setString(1, likeKeyword);
            pstmt.setString(2, likeKeyword);
            pstmt.setString(3, likeKeyword);
            pstmt.setString(4, likeKeyword);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = "[" + rs.getString("brand") + "] " + rs.getString("title");
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("brand"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        if (products.isEmpty()) {
            products.addAll(findProductsByTokens(keyword));
        }

        return products;
    }

    private List<Product> findProductsByTokens(String keyword) {
        List<Product> products = new ArrayList<Product>();
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase().trim();
        if (normalizedKeyword.length() < 2) {
            return products;
        }

        String[] tokens = normalizedKeyword.split("\\s+");
        StringBuilder sql = new StringBuilder("SELECT id, brand, title, category, description, image_url, price_idr FROM products WHERE 1 = 1");
        List<String> searchableTokens = new ArrayList<String>();
        for (String token : tokens) {
            if (token.length() < 2 || isIgnoredSearchToken(token)) {
                continue;
            }
            searchableTokens.add(token);
            sql.append(" AND lower(trim(COALESCE(brand, '') || ' ' || COALESCE(title, '') || ' ' || COALESCE(category, ''))) LIKE ?");
        }
        sql.append(" LIMIT 5");

        if (searchableTokens.isEmpty()) {
            return products;
        }

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < searchableTokens.size(); i++) {
                pstmt.setString(i + 1, "%" + searchableTokens.get(i) + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("brand"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        return products;
    }

    private boolean isIgnoredSearchToken(String token) {
        return "gitar".equals(token)
                || "guitar".equals(token)
                || "harga".equals(token)
                || "info".equals(token)
                || "detail".equals(token)
                || "deskripsi".equals(token)
                || "gambar".equals(token)
                || "foto".equals(token);
    }

    public List<Product> getProductsForDashboard(int limit) {
        List<Product> products = new ArrayList<Product>();
        String sql = "SELECT id, brand, title, category, description, image_url, price_idr FROM products ORDER BY id ASC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String brand = rs.getString("brand");
                    products.add(new Product(
                            rs.getInt("id"),
                            brand,
                            title,
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        return products;
    }

    public List<Product> findProductsByCategory(String category, int limit) {
        List<Product> products = new ArrayList<Product>();
        String sql = "SELECT id, brand, title, category, description, image_url, price_idr FROM products WHERE lower(category) LIKE ? ORDER BY price_idr ASC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + category.toLowerCase() + "%");
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("brand"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        return products;
    }

    public List<Product> findProductsByMaxPrice(int maxPriceIdr, int limit) {
        List<Product> products = new ArrayList<Product>();
        String sql = "SELECT id, brand, title, category, description, image_url, price_idr FROM products WHERE price_idr <= ? ORDER BY price_idr DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, maxPriceIdr);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("brand"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        return products;
    }

    public List<Product> findProductsByCategoryAndMaxPrice(String category, int maxPriceIdr, int limit) {
        List<Product> products = new ArrayList<Product>();
        String sql = "SELECT id, brand, title, category, description, image_url, price_idr " +
                "FROM products " +
                "WHERE price_idr <= ? AND " + buildCategoryWhereClause(category) + " " +
                "ORDER BY price_idr DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, maxPriceIdr);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("brand"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        return products;
    }

    public List<Product> findProductsByCategoryNearPrice(String category, int targetPriceIdr, int minPriceIdr, int maxPriceIdr, int limit) {
        List<Product> products = new ArrayList<Product>();
        String sql = "SELECT id, brand, title, category, description, image_url, price_idr " +
                "FROM products " +
                "WHERE price_idr BETWEEN ? AND ? AND " + buildCategoryWhereClause(category) + " " +
                "ORDER BY ABS(price_idr - ?) ASC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, minPriceIdr);
            pstmt.setInt(2, maxPriceIdr);
            pstmt.setInt(3, targetPriceIdr);
            pstmt.setInt(4, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("id"),
                            rs.getString("brand"),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getInt("price_idr"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            products.clear();
        }

        return products;
    }

    public void addProduct(String brand, String title, String category, int priceIdr, String description, String imageUrl) {
        String sql = "INSERT INTO products(brand, title, category, description, image_url, price_eur, price_idr) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, title);
            pstmt.setString(3, category);
            pstmt.setString(4, normalizeDescription(brand, title, category, description));
            pstmt.setString(5, normalizeImageUrl(brand, title, category, imageUrl));
            pstmt.setDouble(6, convertIdrToEur(priceIdr));
            pstmt.setInt(7, priceIdr);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal menambah produk.", e);
        }
    }

    public void updateProduct(int productId, String brand, String title, String category, int priceIdr, String description, String imageUrl) {
        String sql = "UPDATE products SET brand = ?, title = ?, category = ?, description = ?, image_url = ?, price_eur = ?, price_idr = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, title);
            pstmt.setString(3, category);
            pstmt.setString(4, normalizeDescription(brand, title, category, description));
            pstmt.setString(5, normalizeImageUrl(brand, title, category, imageUrl));
            pstmt.setDouble(6, convertIdrToEur(priceIdr));
            pstmt.setInt(7, priceIdr);
            pstmt.setInt(8, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengubah produk.", e);
        }
    }

    public void deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal menghapus produk.", e);
        }
    }

    public Order findOrder(String orderNumber) {
        String sql = "SELECT id, order_number, status FROM orders WHERE lower(order_number) = lower(?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderNumber == null ? "" : orderNumber.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Order(
                            rs.getInt("id"),
                            rs.getString("order_number"),
                            "",
                            "",
                            rs.getString("status"),
                            ""
                    );
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    public List<Order> getOrdersForDashboard(int limit) {
        List<Order> orders = new ArrayList<Order>();
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            boolean hasCustomerName = hasColumn(stmt, "orders", "customer_name");
            boolean hasProductId = hasColumn(stmt, "orders", "product_id");
            boolean hasUpdatedAt = hasColumn(stmt, "orders", "updated_at");
            String productNameSelect = hasProductId
                    ? "COALESCE(p.brand || ' ' || p.title, '-') AS product_name"
                    : "'-' AS product_name";
            String customerNameSelect = hasCustomerName ? "o.customer_name" : "'' AS customer_name";
            String updatedAtSelect = hasUpdatedAt ? "o.updated_at" : "'' AS updated_at";
            String joinClause = hasProductId ? " LEFT JOIN products p ON p.id = o.product_id" : "";
            String sql = "SELECT o.id, o.order_number, " + customerNameSelect + ", " +
                    productNameSelect + ", o.status, " + updatedAtSelect +
                    " FROM orders o" + joinClause +
                    " ORDER BY o.id DESC LIMIT ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        orders.add(new Order(
                                rs.getInt("id"),
                                rs.getString("order_number"),
                                rs.getString("customer_name"),
                                rs.getString("product_name"),
                                rs.getString("status"),
                                rs.getString("updated_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            orders.clear();
        }
        return orders;
    }

    public void updateOrderStatus(int orderId, String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalStateException("Status pesanan wajib dipilih.");
        }

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            boolean hasUpdatedAt = hasColumn(stmt, "orders", "updated_at");
            String sql = hasUpdatedAt
                    ? "UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
                    : "UPDATE orders SET status = ? WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.trim());
                pstmt.setInt(2, orderId);
                if (pstmt.executeUpdate() == 0) {
                    throw new IllegalStateException("Pesanan tidak ditemukan.");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal memperbarui status pesanan.", e);
        }
    }

    public void addOrder(String orderNumber, int productId, String customerName, String status) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new IllegalStateException("Nomor pesanan wajib diisi.");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalStateException("Nama customer wajib diisi.");
        }
        if (productId <= 0) {
            throw new IllegalStateException("Produk pesanan wajib dipilih.");
        }

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            boolean hasProductId = hasColumn(stmt, "orders", "product_id");
            boolean hasCustomerName = hasColumn(stmt, "orders", "customer_name");
            String trimmedStatus = status == null || status.trim().isEmpty() ? "Pending" : status.trim();
            String sql = hasProductId && hasCustomerName
                    ? "INSERT INTO orders(order_number, product_id, customer_name, status) VALUES(?, ?, ?, ?)"
                    : "INSERT INTO orders(order_number, status) VALUES(?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, orderNumber.trim().toUpperCase());
                if (hasProductId && hasCustomerName) {
                    pstmt.setInt(2, productId);
                    pstmt.setString(3, customerName.trim());
                    pstmt.setString(4, trimmedStatus);
                } else {
                    pstmt.setString(2, trimmedStatus);
                }
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                throw new IllegalStateException("Nomor pesanan sudah terdaftar.", e);
            }
            throw new IllegalStateException("Gagal menambah pesanan.", e);
        }
    }

    public void deleteOrder(int orderId) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            if (pstmt.executeUpdate() == 0) {
                throw new IllegalStateException("Pesanan tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal menghapus pesanan.", e);
        }
    }

    public void logChat(String speaker, String message) {
        String sql = "INSERT INTO chat_logs(speaker, message) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, speaker);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Gagal menyimpan log chat: " + e.getMessage());
        }
    }

    public Admin getAdmin() {
        String sql = "SELECT id, username, password FROM admins LIMIT 1";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return new Admin(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    private boolean hasAnyProduct() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void ensureProductColumns(Statement stmt) throws SQLException {
        if (!hasColumn(stmt, "products", "category")) {
            stmt.execute("ALTER TABLE products ADD COLUMN category TEXT DEFAULT 'Acoustic Guitar'");
            stmt.execute("UPDATE products SET category = 'Acoustic Guitar' WHERE category IS NULL");
        }
        if (!hasColumn(stmt, "products", "description")) {
            stmt.execute("ALTER TABLE products ADD COLUMN description TEXT");
        }
        if (!hasColumn(stmt, "products", "image_url")) {
            stmt.execute("ALTER TABLE products ADD COLUMN image_url TEXT");
        }
        stmt.execute("UPDATE products SET category = 'Bass Guitar' WHERE lower(COALESCE(title, '')) LIKE '%bass%'");
        stmt.execute("UPDATE products SET category = 'Classical Guitar' WHERE lower(COALESCE(title, '')) LIKE '%classical%' OR lower(COALESCE(title, '')) LIKE '%nylon%' OR lower(COALESCE(title, '')) LIKE '%klasik%'");
        stmt.execute("UPDATE products SET category = 'Electric Guitar' WHERE " +
                "lower(COALESCE(title, '')) LIKE '%electric%' " +
                "OR lower(COALESCE(title, '')) LIKE '%strat%' " +
                "OR lower(COALESCE(title, '')) LIKE '%tele%' " +
                "OR lower(COALESCE(title, '')) LIKE '%single cut%' " +
                "OR lower(COALESCE(title, '')) LIKE '%les paul%' " +
                "OR lower(COALESCE(title, '')) LIKE 'sc-%' " +
                "OR lower(COALESCE(title, '')) LIKE '% sc-%' " +
                "OR lower(COALESCE(title, '')) LIKE '%sc-custom%' " +
                "OR lower(COALESCE(title, '')) LIKE '% lp %' " +
                "OR lower(COALESCE(title, '')) LIKE '% lp' " +
                "OR lower(COALESCE(title, '')) LIKE 'lp %' " +
                "OR lower(COALESCE(title, '')) LIKE '%duojet%' " +
                "OR lower(COALESCE(title, '')) LIKE '%jazzmaster%' " +
                "OR lower(COALESCE(title, '')) LIKE '%mustang%'");
        stmt.execute("UPDATE products SET category = 'Electric Guitar' WHERE category = 'Acoustic Guitar' " +
                "AND lower(COALESCE(title, '')) NOT LIKE '%acoustic%' " +
                "AND lower(COALESCE(title, '')) NOT LIKE '%akustik%' " +
                "AND lower(COALESCE(title, '')) NOT LIKE '%dreadnought%' " +
                "AND lower(COALESCE(title, '')) NOT LIKE '%solid top%' " +
                "AND lower(COALESCE(title, '')) NOT LIKE '%fg800%'");
        stmt.execute("UPDATE products SET description = 'Gitar ' || COALESCE(brand, '') || ' ' || COALESCE(title, '') || ' kategori ' || COALESCE(category, 'Acoustic Guitar') || ', cocok untuk latihan, tampil, dan koleksi.' WHERE description IS NULL OR trim(description) = ''");
    }

    private void refreshGeneratedProductImages() throws SQLException {
        String selectSql = "SELECT id, brand, title, category, image_url FROM products";
        String updateSql = "UPDATE products SET image_url = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             ResultSet rs = selectStmt.executeQuery()) {
            while (rs.next()) {
                String currentImageUrl = rs.getString("image_url");
                if (!shouldReplaceGeneratedImage(currentImageUrl)) {
                    continue;
                }

                updateStmt.setString(1, gambarDefaultUntukProduk(
                        rs.getString("brand"),
                        rs.getString("title"),
                        rs.getString("category")
                ));
                updateStmt.setInt(2, rs.getInt("id"));
                updateStmt.addBatch();
            }
            updateStmt.executeBatch();
        }
    }

    private boolean shouldReplaceGeneratedImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return true;
        }

        String lowerImageUrl = imageUrl.toLowerCase();
        return lowerImageUrl.contains("images.unsplash.com/photo-1510915361894")
                || lowerImageUrl.contains("images.unsplash.com/photo-1519389950473")
                || lowerImageUrl.contains("images.unsplash.com/photo-1498038432885")
                || lowerImageUrl.contains("images.unsplash.com/photo-1525201548942")
                || lowerImageUrl.contains("tse1.mm.bing.net/th?q=");
    }

    private boolean hasColumn(Statement stmt, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void seedAdminIfNeeded() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admins")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO admins(username, password) VALUES(?, ?)")) {
            pstmt.setString(1, "admin");
            pstmt.setString(2, "admin123");
            pstmt.executeUpdate();
        }
    }

    private void seedSampleOrdersIfNeeded() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE lower(order_number) = lower(?)")) {
            pstmt.setString(1, "AKB148");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                rs.close();
                return;
            }
            rs.close();
        }

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            boolean hasProductId = hasColumn(stmt, "orders", "product_id");
            boolean hasCustomerName = hasColumn(stmt, "orders", "customer_name");
            String sql = hasProductId && hasCustomerName
                    ? "INSERT INTO orders(order_number, product_id, customer_name, status) VALUES(?, ?, ?, ?)"
                    : "INSERT INTO orders(order_number, status) VALUES(?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "AKB148");
                if (hasProductId && hasCustomerName) {
                    pstmt.setInt(2, findFirstProductId(conn));
                    pstmt.setString(3, "Customer Demo");
                    pstmt.setString(4, "Pending");
                } else {
                    pstmt.setString(2, "Pending");
                }
                pstmt.executeUpdate();
            }
        }
    }

    private int findFirstProductId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM products ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1;
    }

    private String ekstrakJSON(String item, String key) {
        String pattern = "\"" + key + "\":";
        int start = item.indexOf(pattern) + pattern.length();
        String sub = item.substring(start).trim();

        if (sub.startsWith("\"")) {
            return sub.substring(1, sub.indexOf("\"", 1));
        }

        int end = sub.indexOf(",");
        if (end == -1) {
            end = sub.length();
        }
        return sub.substring(0, end).trim();
    }

    private String tentukanKategori(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("bass")) {
            return "Bass Guitar";
        }
        if (lowerTitle.contains("classical") || lowerTitle.contains("nylon") || lowerTitle.contains("klasik")) {
            return "Classical Guitar";
        }
        if (lowerTitle.contains("electric")
                || lowerTitle.contains("strat")
                || lowerTitle.contains("tele")
                || lowerTitle.contains("single cut")
                || lowerTitle.contains("les paul")
                || lowerTitle.startsWith("sc-")
                || lowerTitle.contains(" sc-")
                || lowerTitle.contains("sc-custom")
                || lowerTitle.contains("lp")
                || lowerTitle.contains("duojet")
                || lowerTitle.contains("jazzmaster")
                || lowerTitle.contains("mustang")) {
            return "Electric Guitar";
        }
        if (lowerTitle.contains("acoustic")
                || lowerTitle.contains("akustik")
                || lowerTitle.contains("dreadnought")
                || lowerTitle.contains("solid top")
                || lowerTitle.contains("fg800")) {
            return "Acoustic Guitar";
        }
        return "Electric Guitar";
    }

    private String buildCategoryWhereClause(String category) {
        String lowerCategory = category == null ? "" : category.toLowerCase();
        if (lowerCategory.contains("electric")) {
            return "(lower(category) LIKE '%electric%' OR lower(title) LIKE '%electric%' OR lower(title) LIKE '%strat%' OR lower(title) LIKE '%tele%' OR lower(title) LIKE '%single cut%' OR lower(title) LIKE '%les paul%' OR lower(title) LIKE 'sc-%' OR lower(title) LIKE '% sc-%' OR lower(title) LIKE '%sc-custom%' OR lower(title) LIKE '% lp %' OR lower(title) LIKE '% lp' OR lower(title) LIKE 'lp %')";
        }
        if (lowerCategory.contains("bass")) {
            return "(lower(category) LIKE '%bass%' OR lower(title) LIKE '%bass%')";
        }
        if (lowerCategory.contains("classical")) {
            return "(lower(category) LIKE '%classical%' OR lower(title) LIKE '%classical%' OR lower(title) LIKE '%klasik%')";
        }
        return "((lower(category) LIKE '%acoustic%' OR lower(title) LIKE '%acoustic%' OR lower(title) LIKE '%akustik%' OR lower(title) LIKE '%dreadnought%' OR lower(title) LIKE '%solid top%' OR lower(title) LIKE '%fg800%') AND lower(title) NOT LIKE 'sc-%' AND lower(title) NOT LIKE '% sc-%' AND lower(title) NOT LIKE '%single cut%' AND lower(title) NOT LIKE '%les paul%')";
    }

    private String normalizeDescription(String brand, String title, String category, String description) {
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        return buatDeskripsiProduk(brand, title, category);
    }

    private String normalizeImageUrl(String brand, String title, String category, String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }
        return gambarDefaultUntukProduk(brand, title, category);
    }

    private String buatDeskripsiProduk(String brand, String title, String category) {
        return "Gitar " + brand + " " + title + " kategori " + category + ", cocok untuk latihan, tampil, dan koleksi.";
    }

    private String gambarDefaultUntukProduk(String brand, String title, String category) {
        String query = ((brand == null ? "" : brand) + " " +
                (title == null ? "" : title) + " " +
                (category == null ? "" : category) + " guitar").trim();
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://tse1.mm.bing.net/th?q=" + encodedQuery + "&w=240&h=160&c=7&rs=1&p=0&o=5&dpr=1.5&pid=1.7";
    }

    private double convertIdrToEur(int priceIdr) {
        return priceIdr / 18000.0;
    }
}
