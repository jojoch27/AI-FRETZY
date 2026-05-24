package com.rplbo.app;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class MainController {
    private static final double DASHBOARD_ICON_WIDTH = 82.0;
    private static final double DASHBOARD_NAME_WIDTH = 280.0;
    private static final double DASHBOARD_CATEGORY_WIDTH = 150.0;
    private static final double DASHBOARD_DESCRIPTION_WIDTH = 260.0;
    private static final double DASHBOARD_PRICE_WIDTH = 140.0;
    private static final double DASHBOARD_ACTIONS_WIDTH = 110.0;
    private static final double ORDER_NUMBER_WIDTH = 120.0;
    private static final double ORDER_CUSTOMER_WIDTH = 150.0;
    private static final double ORDER_PRODUCT_WIDTH = 280.0;
    private static final double ORDER_STATUS_WIDTH = 140.0;
    private static final double ORDER_UPDATED_WIDTH = 150.0;
    private static final Locale INDONESIA_LOCALE = new Locale("id", "ID");

    private enum ActiveView {
        CHAT,
        ADMIN_LOGIN,
        ADMIN_DASHBOARD
    }

    private final Database database = new Database();
    private final Chatbot chatbot = new Chatbot(database);
    private final User user = new User();

    @FXML
    private AnchorPane sidebarContainer;

    @FXML
    private AnchorPane topBarContainer;

    @FXML
    private StackPane contentContainer;

    @FXML
    private AnchorPane bottomBarContainer;

    private Button chatNavButton;
    private Button adminNavButton;
    private Label topTitleLabel;
    private Label topSubtitleLabel;
    private TextField bottomInputField;
    private Button bottomSendButton;
    private VBox messagesBox;
    private ScrollPane chatScrollPane;
    private TextField adminEmailField;
    private PasswordField adminPasswordField;
    private Label adminLoginStatusLabel;
    private TableView<Product> dashboardTable;
    private TextField brandField;
    private TextField titleField;
    private TextField categoryField;
    private TextField priceField;
    private TextField descriptionField;
    private TextField imageUrlField;
    private Button saveProductButton;
    private Button cancelEditButton;
    private Button logoutButton;
    private Label dashboardStatusLabel;
    private TableView<Order> orderTable;
    private TextField orderNumberField;
    private TextField customerNameField;
    private ComboBox<Product> orderProductComboBox;
    private Button addOrderButton;
    private Button deleteOrderButton;
    private TextField selectedOrderField;
    private ComboBox<String> orderStatusComboBox;
    private Button updateOrderStatusButton;
    private Integer editingProductId;
    private Order selectedOrder;
    private boolean adminLoggedIn;

    private Parent chatView;
    private Parent adminLoginView;
    private Parent adminDashboardView;
    private ActiveView activeView = ActiveView.CHAT;

    @FXML
    private void initialize() {
        try {
            loadShellSections();
            showChatView();
            addBotMessage("Halo aku Fretzy. Apa yang bisa ku bantu?");
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memuat layout aplikasi.", e);
        }
    }

    private void loadShellSections() throws IOException {
        Parent sidebar = loadInto(sidebarContainer, "SidebarSection.fxml");
        chatNavButton = (Button) sidebar.lookup("#chatNavButton");
        adminNavButton = (Button) sidebar.lookup("#adminNavButton");
        chatNavButton.setOnAction(event -> showChatView());
        adminNavButton.setOnAction(event -> showAdminArea());

        Parent topBar = loadInto(topBarContainer, "TopBarSection.fxml");
        topTitleLabel = (Label) topBar.lookup("#topTitleLabel");
        topSubtitleLabel = (Label) topBar.lookup("#topSubtitleLabel");

        Parent bottomBar = loadInto(bottomBarContainer, "BottomInputSection.fxml");
        bottomInputField = (TextField) bottomBar.lookup("#bottomInputField");
        bottomSendButton = (Button) bottomBar.lookup("#bottomSendButton");
        bottomInputField.setOnAction(event -> handleBottomInput());
        bottomSendButton.setOnAction(event -> handleBottomInput());

        chatView = FXMLLoader.load(getClass().getResource("/com/rplbo/app/ChatSection.fxml"));
        chatScrollPane = (ScrollPane) chatView;
        messagesBox = (VBox) chatScrollPane.getContent();

        adminLoginView = FXMLLoader.load(getClass().getResource("/com/rplbo/app/AdminLoginSection.fxml"));
        adminEmailField = (TextField) adminLoginView.lookup("#adminEmailField");
        adminPasswordField = (PasswordField) adminLoginView.lookup("#adminPasswordField");
        adminLoginStatusLabel = (Label) adminLoginView.lookup("#adminLoginStatusLabel");
        Button loginButton = (Button) adminLoginView.lookup("#adminLoginButton");
        loginButton.setOnAction(event -> handleAdminLogin());
        adminPasswordField.setOnAction(event -> handleAdminLogin());

        adminDashboardView = FXMLLoader.load(getClass().getResource("/com/rplbo/app/AdminDashboardSection.fxml"));
        dashboardTable = (TableView<Product>) lookupAdminDashboardNode("dashboardTable");
        brandField = (TextField) lookupAdminDashboardNode("brandField");
        titleField = (TextField) lookupAdminDashboardNode("titleField");
        categoryField = (TextField) lookupAdminDashboardNode("categoryField");
        priceField = (TextField) lookupAdminDashboardNode("priceField");
        descriptionField = (TextField) lookupAdminDashboardNode("descriptionField");
        imageUrlField = (TextField) lookupAdminDashboardNode("imageUrlField");
        saveProductButton = (Button) lookupAdminDashboardNode("saveProductButton");
        cancelEditButton = (Button) lookupAdminDashboardNode("cancelEditButton");
        logoutButton = (Button) lookupAdminDashboardNode("logoutButton");
        dashboardStatusLabel = (Label) lookupAdminDashboardNode("dashboardStatusLabel");
        orderTable = (TableView<Order>) lookupAdminDashboardNode("orderTable");
        orderNumberField = (TextField) lookupAdminDashboardNode("orderNumberField");
        customerNameField = (TextField) lookupAdminDashboardNode("customerNameField");
        orderProductComboBox = (ComboBox<Product>) lookupAdminDashboardNode("orderProductComboBox");
        addOrderButton = (Button) lookupAdminDashboardNode("addOrderButton");
        deleteOrderButton = (Button) lookupAdminDashboardNode("deleteOrderButton");
        selectedOrderField = (TextField) lookupAdminDashboardNode("selectedOrderField");
        orderStatusComboBox = (ComboBox<String>) lookupAdminDashboardNode("orderStatusComboBox");
        updateOrderStatusButton = (Button) lookupAdminDashboardNode("updateOrderStatusButton");
        if (dashboardTable == null) {
            throw new IllegalStateException("Komponen tabel produk admin tidak berhasil dimuat.");
        }
        if (orderTable == null) {
            throw new IllegalStateException("Komponen tabel pesanan admin tidak berhasil dimuat.");
        }
        configureDashboardTable();
        configureOrderTable();
        saveProductButton.setOnAction(event -> handleSaveProduct());
        cancelEditButton.setOnAction(event -> clearProductForm());
        logoutButton.setOnAction(event -> handleAdminLogout());
        addOrderButton.setOnAction(event -> handleAddOrder());
        deleteOrderButton.setOnAction(event -> handleDeleteOrder());
        updateOrderStatusButton.setOnAction(event -> handleUpdateOrderStatus());
    }

    private Node lookupAdminDashboardNode(String id) {
        Node node = findNodeById(adminDashboardView, id);
        if (node == null) {
            throw new IllegalStateException("Komponen admin dashboard tidak ditemukan: " + id);
        }
        return node;
    }

    private Node findNodeById(Node node, String id) {
        if (node == null) {
            return null;
        }
        if (id.equals(node.getId())) {
            return node;
        }
        if (node instanceof ScrollPane) {
            Node result = findNodeById(((ScrollPane) node).getContent(), id);
            if (result != null) {
                return result;
            }
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                Node result = findNodeById(child, id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private Parent loadInto(AnchorPane container, String resourceName) throws IOException {
        Parent section = FXMLLoader.load(getClass().getResource("/com/rplbo/app/" + resourceName));
        AnchorPane.setTopAnchor(section, 0.0);
        AnchorPane.setRightAnchor(section, 0.0);
        AnchorPane.setBottomAnchor(section, 0.0);
        AnchorPane.setLeftAnchor(section, 0.0);
        container.getChildren().setAll(section);
        return section;
    }

    private void showChatView() {
        activeView = ActiveView.CHAT;
        contentContainer.getChildren().setAll(chatView);
        updateTopBar("Chat with Fretzy Bot", "Ask me anything about guitars!");
        updateNavState(true);
        setBottomBarState(true, false, "Type your message...");
    }

    private void showAdminLoginView() {
        activeView = ActiveView.ADMIN_LOGIN;
        contentContainer.getChildren().setAll(adminLoginView);
        updateTopBar("Admin Login", "");
        updateNavState(false);
        setBottomBarState(false, true, "Type your message...");
        adminLoginStatusLabel.setText("");
    }

    private void showAdminArea() {
        if (adminLoggedIn) {
            showAdminDashboardView();
        } else {
            showAdminLoginView();
        }
    }

    private void showAdminDashboardView() {
        if (!adminLoggedIn) {
            showAdminLoginView();
            adminLoginStatusLabel.setText("Silakan login admin terlebih dahulu.");
            return;
        }

        activeView = ActiveView.ADMIN_DASHBOARD;
        contentContainer.getChildren().setAll(adminDashboardView);
        updateTopBar("Admin Dashboard", "");
        updateNavState(false);
        setBottomBarState(true, true, "Admin dashboard mode");
        clearProductForm();
        refreshDashboardItems();
        refreshOrderItems();
    }

    private void updateTopBar(String title, String subtitle) {
        topTitleLabel.setText(title);
        topSubtitleLabel.setText(subtitle);
        topSubtitleLabel.setVisible(!subtitle.isEmpty());
        topSubtitleLabel.setManaged(!subtitle.isEmpty());
    }

    private void updateNavState(boolean chatActive) {
        applyNavStyle(chatNavButton, chatActive);
        applyNavStyle(adminNavButton, !chatActive);
    }

    private void applyNavStyle(Button button, boolean active) {
        if (active) {
            button.setStyle("-fx-background-color: #506B82; -fx-text-fill: #f7efe4; -fx-font-size: 14px; -fx-background-radius: 6;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #405a70; -fx-font-size: 14px; -fx-background-radius: 6;");
        }
    }

    private void setBottomBarState(boolean visible, boolean disabled, String promptText) {
        bottomBarContainer.setVisible(visible);
        bottomBarContainer.setManaged(visible);
        bottomInputField.setDisable(disabled);
        bottomSendButton.setDisable(disabled);
        bottomInputField.setPromptText(promptText);
    }

    private void handleBottomInput() {
        if (activeView != ActiveView.CHAT) {
            return;
        }

        String input = bottomInputField.getText().trim();
        if (input.isEmpty()) {
            return;
        }

        addUserMessage(input);
        database.logChat("USER", input);
        String response = chatbot.receiveInput(user.askQuestion(input.toLowerCase()));
        addBotMessage(response);
        database.logChat("BOT", response);
        bottomInputField.clear();
    }

    private void handleAdminLogin() {
        String username = adminEmailField.getText().trim();
        String password = adminPasswordField.getText();
        Admin admin = database.getAdmin();

        if (admin != null && admin.login(username, password)) {
            adminLoggedIn = true;
            adminLoginStatusLabel.setText("");
            adminEmailField.clear();
            adminPasswordField.clear();
            showAdminDashboardView();
        } else {
            adminLoginStatusLabel.setText("Login gagal. Gunakan akun admin yang terdaftar.");
        }
    }

    private void handleAdminLogout() {
        adminLoggedIn = false;
        clearProductForm();
        showAdminLoginView();
        adminLoginStatusLabel.setText("Admin berhasil logout.");
    }

    private void refreshDashboardItems() {
        List<Product> products = database.getProductsForDashboard(100);
        dashboardTable.getItems().setAll(products);
        dashboardTable.refresh();
        refreshOrderProductOptions(products);
    }

    private void refreshOrderItems() {
        List<Order> orders = database.getOrdersForDashboard(100);
        orderTable.getItems().setAll(orders);
        orderTable.refresh();
    }

    private void refreshOrderProductOptions(List<Product> products) {
        if (orderProductComboBox == null) {
            return;
        }

        Product selectedProduct = orderProductComboBox.getValue();
        Integer selectedProductId = selectedProduct == null ? null : Integer.valueOf(selectedProduct.getProductId());
        orderProductComboBox.getItems().setAll(products);
        if (selectedProductId == null) {
            return;
        }

        for (Product product : products) {
            if (product.getProductId() == selectedProductId.intValue()) {
                orderProductComboBox.setValue(product);
                return;
            }
        }
        orderProductComboBox.getSelectionModel().clearSelection();
    }

    private void configureDashboardTable() {
        dashboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        dashboardTable.setFixedCellSize(82);
        dashboardTable.setPlaceholder(createDashboardPlaceholder());
        dashboardTable.setFocusTraversable(false);
        dashboardTable.getColumns().setAll(
                createIconColumn(),
                createTextColumn("Name", Product::getName, DASHBOARD_NAME_WIDTH, Pos.CENTER_LEFT, 14, true),
                createTextColumn("Category", Product::getCategory, DASHBOARD_CATEGORY_WIDTH, Pos.CENTER_LEFT, 13, false),
                createTextColumn("Description", Product::getDescription, DASHBOARD_DESCRIPTION_WIDTH, Pos.CENTER_LEFT, 12, false),
                createTextColumn("Price", product -> formatPrice(product.getPrice()), DASHBOARD_PRICE_WIDTH, Pos.CENTER_RIGHT, 14, true),
                createActionsColumn()
        );
    }

    private void configureOrderTable() {
        orderStatusComboBox.getItems().setAll("Pending", "ON PACKING", "ON DELIVERY", "COMPLETED", "CANCELLED");
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderTable.setFixedCellSize(44);
        orderTable.setPlaceholder(createOrderPlaceholder());
        orderTable.setFocusTraversable(false);
        orderTable.getColumns().setAll(
                createOrderTextColumn("Order No", Order::getOrderNumber, ORDER_NUMBER_WIDTH, Pos.CENTER_LEFT, true),
                createOrderTextColumn("Customer", Order::getCustomerName, ORDER_CUSTOMER_WIDTH, Pos.CENTER_LEFT, false),
                createOrderTextColumn("Product", Order::getProductName, ORDER_PRODUCT_WIDTH, Pos.CENTER_LEFT, false),
                createOrderTextColumn("Status", Order::getStatus, ORDER_STATUS_WIDTH, Pos.CENTER_LEFT, true),
                createOrderTextColumn("Updated", Order::getUpdatedAt, ORDER_UPDATED_WIDTH, Pos.CENTER_LEFT, false)
        );
        orderTable.getSelectionModel().selectedItemProperty().addListener((observable, oldOrder, newOrder) -> {
            selectedOrder = newOrder;
            if (newOrder == null) {
                selectedOrderField.clear();
                orderStatusComboBox.getSelectionModel().clearSelection();
                return;
            }
            selectedOrderField.setText(newOrder.getOrderNumber());
            orderStatusComboBox.setValue(newOrder.getStatus());
        });
    }

    private Label createDashboardPlaceholder() {
        Label placeholder = new Label("Belum ada produk untuk ditampilkan.");
        placeholder.setTextFill(Color.web("#5f7284"));
        placeholder.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        return placeholder;
    }

    private Label createOrderPlaceholder() {
        Label placeholder = new Label("Belum ada pesanan untuk ditampilkan.");
        placeholder.setTextFill(Color.web("#5f7284"));
        placeholder.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        return placeholder;
    }

    private TableColumn<Order, String> createOrderTextColumn(
            String title,
            Function<Order, String> valueProvider,
            double width,
            Pos alignment,
            boolean bold
    ) {
        TableColumn<Order, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setMinWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(sanitizeDashboardText(valueProvider.apply(cellData.getValue()))));
        column.setCellFactory(col -> new TableCell<Order, String>() {
            private final Label label = new Label();

            {
                label.setTextFill(Color.web("#405a70"));
                label.setMaxWidth(Double.MAX_VALUE);
                label.setTextOverrun(OverrunStyle.ELLIPSIS);
                label.setFont(Font.font("System", bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, 12));
                setAlignment(alignment);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                label.setText(item);
                label.setTooltip(new Tooltip(item));
                label.setAlignment(alignment);
                label.setPrefWidth(getTableColumn().getWidth() - 18);
                setGraphic(label);
            }
        });
        return column;
    }

    private TableColumn<Product, String> createIconColumn() {
        TableColumn<Product, String> column = new TableColumn<>("Gambar");
        column.setPrefWidth(DASHBOARD_ICON_WIDTH);
        column.setMinWidth(DASHBOARD_ICON_WIDTH);
        column.setMaxWidth(DASHBOARD_ICON_WIDTH + 8);
        column.setResizable(false);
        column.setSortable(false);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(sanitizeDashboardText(cellData.getValue().getImageUrl())));
        column.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String imageUrl, boolean empty) {
                super.updateItem(imageUrl, empty);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                if (empty || imageUrl == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(createProductImage(imageUrl, getTableRow() == null ? null : getTableRow().getItem()));
            }
        });
        return column;
    }

    private TableColumn<Product, String> createTextColumn(
            String title,
            Function<Product, String> valueProvider,
            double width,
            Pos alignment,
            int fontSize,
            boolean bold
    ) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setMinWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(sanitizeDashboardText(valueProvider.apply(cellData.getValue()))));
        column.setCellFactory(col -> new TableCell<Product, String>() {
            private final Label label = new Label();

            {
                label.setTextFill(Color.web("#405a70"));
                label.setMaxWidth(Double.MAX_VALUE);
                label.setTextOverrun(OverrunStyle.ELLIPSIS);
                label.setFont(Font.font("System", bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, fontSize));
                setAlignment(alignment);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                label.setText(item);
                label.setTooltip(new Tooltip(item));
                label.setAlignment(alignment);
                label.setPrefWidth(getTableColumn().getWidth() - 18);
                setGraphic(label);
            }
        });
        return column;
    }

    private TableColumn<Product, Product> createActionsColumn() {
        TableColumn<Product, Product> column = new TableColumn<>("Actions");
        column.setPrefWidth(DASHBOARD_ACTIONS_WIDTH + 12);
        column.setMinWidth(DASHBOARD_ACTIONS_WIDTH + 12);
        column.setMaxWidth(DASHBOARD_ACTIONS_WIDTH + 20);
        column.setResizable(false);
        column.setSortable(false);
        column.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        column.setCellFactory(col -> new TableCell<Product, Product>() {
            private final VBox actions = new VBox(8);
            private final Button editButton = createActionButton("Edit", "#3d8b5f", "#f7efe4");
            private final Button deleteButton = createActionButton("Delete", "#b74646", "#f7efe4");

            {
                actions.setAlignment(Pos.CENTER);
                actions.getChildren().addAll(editButton, deleteButton);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    return;
                }
                editButton.setOnAction(event -> populateProductForm(product));
                deleteButton.setOnAction(event -> handleDeleteProduct(product.getProductId()));
                setGraphic(actions);
            }
        });
        return column;
    }

    private StackPane createProductImage(String imageUrl, Product product) {
        if (!"-".equals(imageUrl)) {
            Image image = new Image(imageUrl, 56, 56, true, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(56);
            imageView.setFitHeight(56);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            StackPane pane = new StackPane(imageView);
            pane.setMinWidth(DASHBOARD_ICON_WIDTH);
            pane.setPrefWidth(DASHBOARD_ICON_WIDTH);
            pane.setMaxWidth(DASHBOARD_ICON_WIDTH);
            return pane;
        }

        String initials = product == null ? "GT" : buildProductInitials(product);
        Circle circle = new Circle(20, Color.web("#506B82"));
        Label initialsLabel = new Label(initials);
        initialsLabel.setTextFill(Color.WHITE);
        initialsLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        StackPane icon = new StackPane(circle, initialsLabel);
        icon.setMinWidth(DASHBOARD_ICON_WIDTH);
        icon.setPrefWidth(DASHBOARD_ICON_WIDTH);
        icon.setMaxWidth(DASHBOARD_ICON_WIDTH);
        return icon;
    }

    private String buildProductInitials(Product product) {
        String brand = product.getBrand() == null ? "" : product.getBrand().trim();
        String title = product.getTitle() == null ? "" : product.getTitle().trim();
        String source = !brand.isEmpty() ? brand : title;
        String[] parts = source.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "PR";
        }
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String sanitizeDashboardText(String text) {
        return text == null || text.trim().isEmpty() ? "-" : text.trim();
    }

    private String formatPrice(double price) {
        NumberFormat formatter = NumberFormat.getNumberInstance(INDONESIA_LOCALE);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return "Rp " + formatter.format(price);
    }

    private Button createActionButton(String text, String backgroundColor, String textColor) {
        Button button = new Button(text);
        button.setPrefWidth(92);
        button.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 6;"
        );
        return button;
    }

    private void handleSaveProduct() {
        String brand = brandField.getText().trim();
        String title = titleField.getText().trim();
        String category = categoryField.getText().trim();
        String priceText = priceField.getText().trim();
        String description = descriptionField.getText().trim();
        String imageUrl = imageUrlField.getText().trim();

        if (brand.isEmpty() || title.isEmpty() || category.isEmpty() || priceText.isEmpty()) {
            dashboardStatusLabel.setText("Brand, nama, kategori, dan harga wajib diisi.");
            return;
        }

        int priceIdr;
        try {
            priceIdr = Integer.parseInt(priceText.replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            dashboardStatusLabel.setText("Harga harus berupa angka.");
            return;
        }

        try {
            String successMessage;
            if (editingProductId == null) {
                database.addProduct(brand, title, category, priceIdr, description, imageUrl);
                successMessage = "Produk berhasil ditambahkan.";
            } else {
                database.updateProduct(editingProductId.intValue(), brand, title, category, priceIdr, description, imageUrl);
                successMessage = "Produk berhasil diperbarui.";
                showSuccessPopup("Edit Berhasil", successMessage);
            }
            clearProductForm();
            dashboardStatusLabel.setText(successMessage);
            refreshDashboardItems();
        } catch (IllegalStateException e) {
            dashboardStatusLabel.setText(e.getMessage());
        }
    }

    private void handleDeleteProduct(int productId) {
        try {
            database.deleteProduct(productId);
            if (editingProductId != null && editingProductId.intValue() == productId) {
                clearProductForm();
            }
            dashboardStatusLabel.setText("Produk berhasil dihapus.");
            showSuccessPopup("Delete Berhasil", "Produk berhasil dihapus.");
            refreshDashboardItems();
        } catch (IllegalStateException e) {
            dashboardStatusLabel.setText(e.getMessage());
        }
    }

    private void handleAddOrder() {
        String orderNumber = orderNumberField.getText().trim().toUpperCase(Locale.ROOT);
        String customerName = customerNameField.getText().trim();
        Product product = orderProductComboBox.getValue();

        if (orderNumber.isEmpty() || customerName.isEmpty() || product == null) {
            dashboardStatusLabel.setText("Nomor pesanan, nama customer, dan produk wajib diisi.");
            return;
        }

        try {
            database.addOrder(orderNumber, product.getProductId(), customerName, "Pending");
            clearOrderForm();
            refreshOrderItems();
            selectOrderByNumber(orderNumber);
            dashboardStatusLabel.setText("Pesanan " + orderNumber + " berhasil ditambahkan.");
            showSuccessPopup("Tambah Berhasil", "Pesanan " + orderNumber + " berhasil ditambahkan.");
        } catch (IllegalStateException e) {
            dashboardStatusLabel.setText(e.getMessage());
        }
    }

    private void handleDeleteOrder() {
        if (selectedOrder == null) {
            dashboardStatusLabel.setText("Pilih pesanan yang ingin dihapus.");
            return;
        }

        String orderNumber = selectedOrder.getOrderNumber();
        try {
            database.deleteOrder(selectedOrder.getOrderId());
            clearOrderSelection();
            refreshOrderItems();
            dashboardStatusLabel.setText("Pesanan " + orderNumber + " berhasil dihapus.");
            showSuccessPopup("Delete Berhasil", "Pesanan " + orderNumber + " berhasil dihapus.");
        } catch (IllegalStateException e) {
            dashboardStatusLabel.setText(e.getMessage());
        }
    }

    private void handleUpdateOrderStatus() {
        if (selectedOrder == null) {
            dashboardStatusLabel.setText("Pilih pesanan yang ingin diperbarui.");
            return;
        }

        String status = orderStatusComboBox.getValue();
        if (status == null || status.trim().isEmpty()) {
            dashboardStatusLabel.setText("Pilih status pesanan terlebih dahulu.");
            return;
        }

        try {
            database.updateOrderStatus(selectedOrder.getOrderId(), status);
            String orderNumber = selectedOrder.getOrderNumber();
            refreshOrderItems();
            selectOrderByNumber(orderNumber);
            dashboardStatusLabel.setText("Status pesanan " + orderNumber + " berhasil diperbarui.");
            showSuccessPopup("Update Berhasil", "Status pesanan " + orderNumber + " berhasil diperbarui.");
        } catch (IllegalStateException e) {
            dashboardStatusLabel.setText(e.getMessage());
        }
    }

    private void selectOrderByNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return;
        }

        for (Order order : orderTable.getItems()) {
            if (orderNumber.equalsIgnoreCase(order.getOrderNumber())) {
                orderTable.getSelectionModel().select(order);
                orderTable.scrollTo(order);
                return;
            }
        }
        selectedOrder = null;
        selectedOrderField.clear();
        orderStatusComboBox.getSelectionModel().clearSelection();
    }

    private void clearOrderForm() {
        if (orderNumberField != null) {
            orderNumberField.clear();
            customerNameField.clear();
            orderProductComboBox.getSelectionModel().clearSelection();
        }
    }

    private void clearOrderSelection() {
        selectedOrder = null;
        if (orderTable != null) {
            orderTable.getSelectionModel().clearSelection();
        }
        if (selectedOrderField != null) {
            selectedOrderField.clear();
            orderStatusComboBox.getSelectionModel().clearSelection();
        }
    }

    private void populateProductForm(Product product) {
        editingProductId = Integer.valueOf(product.getProductId());
        brandField.setText(product.getBrand());
        titleField.setText(product.getTitle());
        categoryField.setText(product.getCategory());
        priceField.setText(String.format("%.0f", product.getPrice()));
        descriptionField.setText(product.getDescription());
        imageUrlField.setText(product.getImageUrl());
        saveProductButton.setText("Simpan Perubahan");
        dashboardStatusLabel.setText("Mode edit produk aktif.");
    }

    private void clearProductForm() {
        editingProductId = null;
        if (brandField != null) {
            brandField.clear();
            titleField.clear();
            categoryField.clear();
            priceField.clear();
            descriptionField.clear();
            imageUrlField.clear();
        }
        if (saveProductButton != null) {
            saveProductButton.setText("Tambah Produk");
        }
        if (dashboardStatusLabel != null) {
            dashboardStatusLabel.setText("");
        }
    }

    private void addBotMessage(String message) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Circle avatar = new Circle(18, Color.web("#6ba46a"));
        Label avatarLabel = new Label("F");
        avatarLabel.setTextFill(Color.WHITE);
        avatarLabel.setStyle("-fx-font-weight: bold;");
        StackPane avatarPane = new StackPane(avatar, avatarLabel);

        VBox bubbleContent = new VBox(4);
        bubbleContent.setAlignment(Pos.TOP_LEFT);
        bubbleContent.setPadding(new Insets(12, 16, 12, 16));
        bubbleContent.setStyle("-fx-background-color: #59728a; -fx-background-radius: 8;");

        String imageUrl = extractImageUrl(message);
        String displayMessage = removeImageUrlLine(message);
        Label messageLabel = createMessageLabel(displayMessage);
        Label timeLabel = createTimeLabel();
        bubbleContent.getChildren().add(messageLabel);
        if (!imageUrl.isEmpty()) {
            bubbleContent.getChildren().add(createChatProductImage(imageUrl));
        }
        bubbleContent.getChildren().add(timeLabel);

        row.getChildren().addAll(avatarPane, bubbleContent);
        messagesBox.getChildren().add(row);
        scrollChatToBottom();
    }

    private void addUserMessage(String message) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_RIGHT);

        VBox bubbleContent = new VBox(4);
        bubbleContent.setAlignment(Pos.TOP_RIGHT);
        bubbleContent.setPadding(new Insets(12, 16, 12, 16));
        bubbleContent.setStyle("-fx-background-color: #5e7d98; -fx-background-radius: 8;");

        Label messageLabel = createMessageLabel(message);
        Label timeLabel = createTimeLabel();
        bubbleContent.getChildren().addAll(messageLabel, timeLabel);

        Circle avatar = new Circle(18, Color.web("#aa8a6b"));
        Label avatarLabel = new Label("U");
        avatarLabel.setTextFill(Color.WHITE);
        avatarLabel.setStyle("-fx-font-weight: bold;");
        StackPane avatarPane = new StackPane(avatar, avatarLabel);

        row.getChildren().addAll(bubbleContent, avatarPane);
        messagesBox.getChildren().add(row);
        scrollChatToBottom();
    }

    private Label createMessageLabel(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(520);
        label.setTextFill(Color.web("#f7efe4"));
        return label;
    }

    private Node createChatProductImage(String imageUrl) {
        Image image = new Image(imageUrl, 240, 160, true, true, true);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(240);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        StackPane frame = new StackPane(imageView);
        frame.setAlignment(Pos.CENTER_LEFT);
        frame.setMinHeight(120);
        frame.setMaxWidth(260);
        frame.setStyle("-fx-background-color: #f6eee2; -fx-background-radius: 6; -fx-padding: 8;");
        return frame;
    }

    private String extractImageUrl(String message) {
        String marker = "Gambar:";
        int markerIndex = message.indexOf(marker);
        if (markerIndex < 0) {
            return "";
        }

        int urlStart = markerIndex + marker.length();
        int urlEnd = message.indexOf("\n", urlStart);
        if (urlEnd < 0) {
            urlEnd = message.length();
        }
        return message.substring(urlStart, urlEnd).trim();
    }

    private String removeImageUrlLine(String message) {
        String marker = "Gambar:";
        int markerIndex = message.indexOf(marker);
        if (markerIndex < 0) {
            return message;
        }

        int lineEnd = message.indexOf("\n", markerIndex);
        if (lineEnd < 0) {
            return message.substring(0, markerIndex).trim();
        }
        return (message.substring(0, markerIndex) + message.substring(lineEnd + 1)).trim();
    }

    private Label createTimeLabel() {
        Label label = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        label.setTextFill(Color.web("#d9e3eb"));
        label.setStyle("-fx-font-size: 10px;");
        return label;
    }

    private void scrollChatToBottom() {
        chatScrollPane.layout();
        chatScrollPane.setVvalue(1.0);
    }

    private void showSuccessPopup(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
