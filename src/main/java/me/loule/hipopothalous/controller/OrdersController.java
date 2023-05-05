package me.loule.hipopothalous.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import me.loule.hipopothalous.model.Accounting;
import me.loule.hipopothalous.model.DatabaseConnection;
import me.loule.hipopothalous.model.TableModel;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

class OrderDish {
    private final String name;
    private int quantity;
    private final double price;


    public OrderDish(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }


    /**
     * @param obj
     * @return
     * This function is used to compare two OrderDish objects and check if they are the same
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OrderDish orderDish) {
            return orderDish.getName().equals(this.name);
        }
        return false;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public void decrementQuantity() {
        this.quantity--;
    }

    public double getPrice() {
        return price;
    }

    public double getTotalPrice() {
        return price * quantity;
    }

    public String toString() {
        return name + " x" + quantity;
    }
}

public class OrdersController {

    @FXML
    ComboBox tableListComboBox;

    List<TableModel> tables = new ArrayList<>();
    @FXML
    Pagination dishesPagination;
    @FXML
    ListView<OrderDish> lvOrder;
    @FXML
    Button btnConfirmOrder;
    @FXML
    Label lblTotalPrice;
    @FXML
    TextField tfTableNumber = new TextField();
    @FXML
    TextField tfPersonNumber;
    ArrayList<OrderDish> orderDishes = new ArrayList<>();
    List<OrderDish> dishes = new ArrayList<>();

    /**
     * This function is called when the view is initialized
     * It will load the dishes from the database and display them in the pagination
     * It will also initialize the order list view
     */
    public void initialize() {
        try {
            Connection connection = DatabaseConnection.getConnection();
            ResultSet rs;
            try (Statement statement = connection.createStatement()) {
                String sql = "SELECT * FROM dishes ORDER BY name ASC";
                rs = statement.executeQuery(sql);
                while (rs.next()) {
                    dishes.add(new OrderDish(rs.getString("name"), 1, rs.getDouble("price")));
                }

                dishesPagination.setPageCount((int) Math.ceil(dishes.size() * 1.0 / 10));
                dishesPagination.setPageFactory(param -> {
                    GridPane gridPane = new GridPane();
                    gridPane.setHgap(10);
                    gridPane.setVgap(10);

                    for (int i = 0; i < dishes.size(); i++) {
                        if (param * 10 + i < dishes.size()) {
                            VBox vBox = new VBox();
                            vBox.setSpacing(10);
                            vBox.setPrefWidth(150);
                            vBox.setPrefHeight(100);
                            vBox.setStyle("-fx-border-color: #000000; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-padding: 10px;-fx-background-color: rgba(0,0,0,0.2)");
                            Label lblName = new Label(dishes.get(param * 10 + i).getName());
                            Label lblPrice = new Label(dishes.get(param * 10 + i).getPrice() + "€");
                            vBox.getChildren().addAll(lblName, lblPrice);
                            lblName.prefWidthProperty().bind(vBox.widthProperty());
                            lblName.prefHeightProperty().bind(vBox.heightProperty());
                            lblName.setStyle("-fx-alignment: center;-fx-font-weight: bold");
                            lblPrice.prefWidthProperty().bind(vBox.widthProperty());
                            lblPrice.prefHeightProperty().bind(vBox.heightProperty());
                            lblPrice.setStyle("-fx-alignment: center; -fx-font-size: 20px; -fx-font-weight: bold");

                            int finalI = i;
                            vBox.setOnMouseClicked(event -> {
                                OrderDish orderDish = new OrderDish(lblName.getText(), 1, dishes.get(param * 10 + finalI).getPrice());
                                if (orderDishes.contains(orderDish)) {
                                    orderDishes.get(orderDishes.indexOf(orderDish)).incrementQuantity();
                                } else {
                                    orderDishes.add(orderDish);
                                }
                                lvOrder.getItems().clear();
                                lvOrder.getItems().addAll(orderDishes);
                                String price = String.format("%.2f", orderDishes.stream().mapToDouble(OrderDish::getTotalPrice).sum());
                                lblTotalPrice.setText("Total: %s€".formatted(price));


                            });
                            gridPane.add(vBox, i % 5, i / 5);
                        }
                    }
                    return gridPane;
                });
            }
        } catch (SQLException e) {
            Logger.getLogger(e.getMessage());
        }

        addTableToList();
        // on listview item press decrement quantity
        lvOrder.setOnMouseClicked(event1 -> {
            if (event1.getClickCount() == 2) {
                OrderDish orderDish1 = lvOrder.getSelectionModel().getSelectedItem();
                if (orderDish1.getQuantity() > 1) {
                    orderDish1.decrementQuantity();
                    lblTotalPrice.setText("Total: " + String.format("%.2f", orderDishes.stream().mapToDouble(OrderDish::getTotalPrice).sum()) + "€");
                } else {
                    orderDishes.remove(orderDish1);
                    lblTotalPrice.setText("Total: " + String.format("%.2f", orderDishes.stream().mapToDouble(OrderDish::getTotalPrice).sum()) + "€");
                }
                lvOrder.getItems().clear();
                lvOrder.getItems().addAll(orderDishes);
            }
        });

    }


    /**
     * This function is called when the user clicks on the confirm order button
     * It will check if all the fields are filled in and if they are it will add the order to the database
     */
    public void addOrder() {
        if (tfPersonNumber.getText().equals("") || tfTableNumber.getText().equals("") || orderDishes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error adding order");
            alert.setHeaderText("Error adding order");
            alert.setContentText("Please fill in all the fields");
            alert.showAndWait();
        } else {
            Connection connection = DatabaseConnection.getConnection();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            try {
                double price = 0.0;
                try (Statement statement = connection.createStatement()) {
                    for (OrderDish orderDish : orderDishes) {
                        ResultSet rs = statement.executeQuery("SELECT price FROM dishes WHERE name = '" + orderDish.getName() + "'");
                        rs.next();
                        price += rs.getDouble(1) * orderDish.getQuantity();
                    }

                    String sql = "INSERT INTO orders (status, price, table_number, persons_per_table,date) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setString(1, "pending");
                        preparedStatement.setDouble(2, Math.round(price * 100.0) / 100.0);
                        preparedStatement.setString(3, tfTableNumber.getText());
                        preparedStatement.setInt(4, Integer.parseInt(tfPersonNumber.getText()));
                        preparedStatement.setTimestamp(5, timestamp);
                        Accounting.createAccounting("Gain", Math.round(price * 100.0) / 100.0, timestamp);
                        preparedStatement.executeUpdate();
                    }

                    ResultSet rs = statement.executeQuery("SELECT MAX(id) FROM orders");
                    rs.next();

                    int orderId = rs.getInt(1);
                    for (OrderDish orderDish : orderDishes) {
                        sql = "INSERT INTO order_dishes (order_id, dish_id, quantity) VALUES (" + orderId + ", (SELECT dishes_id FROM dishes WHERE name = '" + orderDish.getName() + "'), " + orderDish.getQuantity() + ")";
                        statement.executeUpdate(sql);
                    }
                }

                orderDishes.clear();
                lvOrder.getItems().clear();
                lblTotalPrice.setText("Total: 0€");
                tfPersonNumber.setText("");
                tfTableNumber.setText("");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Order added");
                alert.setHeaderText("Order added");
                alert.setContentText("Order added successfully");
                alert.showAndWait();

            } catch (SQLException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error");
                alert.setContentText("Error while adding order");
                alert.showAndWait();
            }
        }
    }

    //When a table is selected from the combobox, the table number is added to the textfield
    public void addTableNumber(){
        for (TableModel tableModel : tables) {
            if (tableModel.getLocation().equals(tableListComboBox.getValue())){
                tfTableNumber.setText(String.valueOf(tableModel.getId()));
            }
        }
    }

    public void addTableToList(){
        //Get all table from teh database and add them to the list
        try {
            Connection connection = DatabaseConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM tables");
            while (rs.next()) {
                tables.add(new TableModel(rs.getInt("id"),rs.getInt("size"), rs.getString("location"), rs.getTimestamp("date")));
            }
            tables.stream()
                    .sorted(Comparator.comparing(TableModel::getLocation))
                    .forEach(tableModel -> tableListComboBox.getItems().add(tableModel.getLocation()));
        } catch (SQLException e) {
            Logger.getLogger(e.getMessage());
        }
    }
}