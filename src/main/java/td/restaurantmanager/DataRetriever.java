package td.restaurantmanager;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        String sql = """
                SELECT di.id, d.id as id_dish, d.name as dish_name, d.dish_type, d.selling_price,
                       di.id_ingredient, i.name as ingredient_name, i.price, i.category, di.quantity_required, di.unit
                FROM dish d
                LEFT JOIN dish_ingredient di ON di.id_dish = d.id
                LEFT JOIN ingredient i ON di.id_ingredient = i.id
                WHERE d.id = ?
                ORDER BY di.id""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            Dish dish = null;
            List<DishIngredient> dishIngredients = new ArrayList<>();

            while (rs.next()) {
                if (dish == null) {
                    dish = extractDishFromResultSet(rs);
                }
                if (rs.getInt("id_ingredient") > 0) {
                    dishIngredients.add(extractDishIngredientFromResultSet(rs, dish));
                }
            }

            if (dish == null) {
                throw new RuntimeException("Dish not found: id=" + id);
            }

            dish.setDishIngredients(dishIngredients);
            return dish;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding dish by id", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Ingredient> findIngredients(Integer page, Integer size) {
        boolean hasPagination = (page != null && size != null);

        if (hasPagination && (page <= 0 || size <= 0)) {
            throw new IllegalArgumentException("Page and size must be greater than 0");
        }

        String sql = """
                SELECT i.id as id_ingredient, i.name as ingredient_name, i.price, i.category,
                       di.id_dish, di.quantity_required, di.unit
                FROM ingredient i
                LEFT JOIN dish_ingredient di ON di.id_ingredient = i.id
                ORDER BY i.id""";

        if (hasPagination) {
            sql += " LIMIT ? OFFSET ?";
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            if (hasPagination) {
                ps.setInt(1, size);
                ps.setInt(2, (page - 1) * size);
            }
            ResultSet rs = ps.executeQuery();

            Map<Integer, Ingredient> ingredientsMap = new LinkedHashMap<>();

            while (rs.next()) {
                int ingredId = rs.getInt("id_ingredient");
                if (!ingredientsMap.containsKey(ingredId)) {
                    ingredientsMap.put(ingredId, extractIngredientFromResultSet(rs));
                }
            }

            return new ArrayList<>(ingredientsMap.values());
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredients", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Ingredient findIngredientById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        String sql = """
                SELECT id, name, price, category
                FROM ingredient
                WHERE id = ?""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                List<StockMovement> movements = getIngredientStockMovements(id);
                return new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        movements
                );
            } else {
                throw new RuntimeException("Ingredient not found: id=" + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredient by id", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Table findTableByNumber(int number) {
        String sql = """
                SELECT id, number
                FROM "table"
                WHERE number = ?""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, number);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Table(rs.getInt("id"), rs.getInt("number"));
            } else {
                throw new RuntimeException("Table not found with number: " + number);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding table by number", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Order saveOrder(Order orderToSave) {
        if (orderToSave == null) {
            throw new IllegalArgumentException("Order is null");
        }
        if (orderToSave.getDishOrders() == null || orderToSave.getDishOrders().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one dish");
        }
        if (orderToSave.getTable() == null) {
            throw new IllegalArgumentException("TableOrder is required");
        }
        TableOrder tableOrder = orderToSave.getTable();
        if (tableOrder.getArrivalDatetime() == null || tableOrder.getDepartureDatetime() == null ||
            tableOrder.getDepartureDatetime().isBefore(tableOrder.getArrivalDatetime())) {
            throw new IllegalArgumentException("Invalid arrival or departure datetime");
        }

        Table table = findTableByNumber(tableOrder.getTable().getNumber());
        int tableId = table.getId();
        int tableNumber = table.getNumber();
        Instant arrival = tableOrder.getArrivalDatetime();
        Instant departure = tableOrder.getDepartureDatetime();
        Instant creationTime = orderToSave.getCreationDatetime() != null ? orderToSave.getCreationDatetime() : Instant.now();

        Connection connCheck = dbConnection.getDBConnection();
        try {
            if (!isTableAvailable(connCheck, tableId, arrival, departure)) {
                List<Table> availableTables = getAvailableTables(connCheck, arrival, departure);
                String message;
                if (availableTables.isEmpty()) {
                    message = "Aucune table n'est disponible.";
                } else {
                    List<Integer> availNums = availableTables.stream()
                            .map(Table::getNumber)
                            .sorted()
                            .toList();
                    message = "La table " + tableNumber + " n'est pas disponible, mais les tables " + availNums + " le sont.";
                }
                throw new IllegalArgumentException(message);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking table availability", e);
        } finally {
            dbConnection.closeDBConnection(connCheck);
        }

        Map<Integer, Double> requiredQuantities = new HashMap<>();
        Map<Integer, UnitType> unitsByIngredient = new HashMap<>();
        Set<Integer> ingredientIds = new HashSet<>();
        for (DishOrder dishOrder : orderToSave.getDishOrders()) {
            int qty = dishOrder.getQuantity();
            for (DishIngredient di : dishOrder.getDish().getDishIngredients()) {
                int ingredId = di.getIngredient().getId();
                double req = di.getQuantityRequired() * qty;
                requiredQuantities.put(ingredId, requiredQuantities.getOrDefault(ingredId, 0.0) + req);
                unitsByIngredient.put(ingredId, di.getUnit());
                ingredientIds.add(ingredId);
            }
        }

        List<String> insufficientIngredients = new ArrayList<>();
        Map<Integer, Ingredient> fullIngredients = new HashMap<>();
        for (int ingredId : ingredientIds) {
            Ingredient fullIng = findIngredientById(ingredId);
            fullIngredients.put(ingredId, fullIng);
            double needed = requiredQuantities.get(ingredId);
            StockValue stock = fullIng.getStockValueAt(creationTime);
            if (!stock.getUnit().equals(unitsByIngredient.get(ingredId))) {
                throw new IllegalArgumentException("Unit mismatch for ingredient " + fullIng.getName());
            }
            if (stock.getQuantity() < needed) {
                insufficientIngredients.add(fullIng.getName());
            }
        }

        if (!insufficientIngredients.isEmpty()) {
            throw new IllegalArgumentException("Les ingrédients suivants ne sont pas suffisants pour le nombre de plats voulu : " + String.join(", ", insufficientIngredients));
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);

            String reference = generateOrderReference(conn);
            int orderId = nextId(conn, "order");

            String sqlOrder = """
                    INSERT INTO "order" (id, reference, creation_datetime)
                    VALUES (?, ?, ?)""";
            try (PreparedStatement psOrder = conn.prepareStatement(sqlOrder)) {
                psOrder.setInt(1, orderId);
                psOrder.setString(2, reference);
                psOrder.setTimestamp(3, Timestamp.from(creationTime));
                psOrder.executeUpdate();
            }

            String sqlTableOrder = """
                    INSERT INTO table_order (id_order, id_table, arrival_datetime, departure_datetime) VALUES (?, ?, ?, ?)""";
            try (PreparedStatement psTableOrder = conn.prepareStatement(sqlTableOrder)) {
                psTableOrder.setInt(1, orderId);
                psTableOrder.setInt(2, tableId);
                psTableOrder.setTimestamp(3, Timestamp.from(arrival));
                psTableOrder.setTimestamp(4, Timestamp.from(departure));
                psTableOrder.executeUpdate();
            }

            for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                int dishOrderId = nextId(conn, "dish_order");
                String sqlDishOrder = "INSERT INTO dish_order (id, id_dish, id_order, quantity) VALUES (?, ?, ?, ?)";
                try (PreparedStatement psDishOrder = conn.prepareStatement(sqlDishOrder)) {
                    psDishOrder.setInt(1, dishOrderId);
                    psDishOrder.setInt(2, dishOrder.getDish().getId());
                    psDishOrder.setInt(3, orderId);
                    psDishOrder.setInt(4, dishOrder.getQuantity());
                    psDishOrder.executeUpdate();
                }
            }

            for (Map.Entry<Integer, Double> entry : requiredQuantities.entrySet()) {
                int ingredId = entry.getKey();
                double qty = entry.getValue();
                Ingredient fullIng = fullIngredients.get(ingredId);
                StockValue currentStock = fullIng.getStockValueAt(creationTime);
                UnitType unit = currentStock.getUnit();

                int movementId = nextId(conn, "stock_movement");
                String sqlMovement = "INSERT INTO stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psMovement = conn.prepareStatement(sqlMovement)) {
                    psMovement.setInt(1, movementId);
                    psMovement.setInt(2, ingredId);
                    psMovement.setDouble(3, qty);
                    psMovement.setString(4, unit.name());
                    psMovement.setString(5, MovementTypeEnum.OUT.name());
                    psMovement.setTimestamp(6, Timestamp.from(creationTime));
                    psMovement.executeUpdate();
                }
            }

            conn.commit();

            Table savedTable = new Table(tableId, tableNumber);
            TableOrder savedTableOrder = new TableOrder(savedTable, arrival, departure);
            return new Order(orderId, reference, creationTime, orderToSave.getDishOrders(), savedTableOrder);
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Rollback failed", ex);
            }
            throw new RuntimeException("Error saving order", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private boolean isTableAvailable(Connection conn, int tableId, Instant arrival, Instant departure) throws SQLException {
        String sql = """
                SELECT 1 FROM table_order 
                WHERE id_table = ? 
                AND NOT (departure_datetime <= ? OR arrival_datetime >= ?) 
                LIMIT 1""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setTimestamp(2, Timestamp.from(arrival));
            ps.setTimestamp(3, Timestamp.from(departure));
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    private List<Table> getAvailableTables(Connection conn, Instant arrival, Instant departure) throws SQLException {
        List<Table> available = new ArrayList<>();
        String sqlTables = "SELECT id, number FROM \"table\" ORDER BY number";
        try (PreparedStatement ps = conn.prepareStatement(sqlTables);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int tid = rs.getInt("id");
                int num = rs.getInt("number");
                if (isTableAvailable(conn, tid, arrival, departure)) {
                    available.add(new Table(tid, num));
                }
            }
        }
        return available;
    }

    private List<StockMovement> getIngredientStockMovements(Integer ingredientId) {
        String sql = """
                SELECT id, quantity, unit, type, creation_datetime
                FROM stock_movement
                WHERE id_ingredient = ?
                ORDER BY creation_datetime""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();

            List<StockMovement> movements = new ArrayList<>();
            while (rs.next()) {
                movements.add(extractStockMovementFromResultSet(rs));
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException("Error getting stock movements", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private Dish extractDishFromResultSet(ResultSet rs) throws SQLException {
        return new Dish(
                rs.getInt("id_dish"),
                rs.getString("dish_name"),
                DishTypeEnum.valueOf(rs.getString("dish_type")),
                rs.getDouble("selling_price"),
                null
        );
    }

    private DishIngredient extractDishIngredientFromResultSet(ResultSet rs, Dish dish) throws SQLException {
        Ingredient ingredient = new Ingredient(
                rs.getInt("id_ingredient"),
                rs.getString("ingredient_name"),
                rs.getDouble("price"),
                CategoryEnum.valueOf(rs.getString("category"))
        );

        return new DishIngredient(
                rs.getInt("id"),
                dish,
                ingredient,
                rs.getDouble("quantity_required"),
                UnitType.valueOf(rs.getString("unit"))
        );
    }

    private Ingredient extractIngredientFromResultSet(ResultSet rs) throws SQLException {
        Integer ingredientId = rs.getInt("id_ingredient");
        List<StockMovement> stockMovements = getIngredientStockMovements(ingredientId);

        return new Ingredient(
                ingredientId,
                rs.getString("ingredient_name"),
                rs.getDouble("price"),
                CategoryEnum.valueOf(rs.getString("category")),
                stockMovements
        );
    }

    private StockMovement extractStockMovementFromResultSet(ResultSet rs) throws SQLException {
        return new StockMovement(
                rs.getInt("id"),
                new StockValue(
                        rs.getDouble("quantity"),
                        UnitType.valueOf(rs.getString("unit"))
                ),
                MovementTypeEnum.valueOf(rs.getString("type")),
                rs.getTimestamp("creation_datetime").toInstant()
        );
    }

    private int nextId(Connection conn, String tableName) throws SQLException {
        String sql = String.format("SELECT COALESCE(MAX(id), 0) + 1 FROM \"%s\"", tableName);

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1;
        }
    }

    private String generateOrderReference(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(reference AS INTEGER)), 0) + 1 FROM \"order\"";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("%06d", rs.getInt(1));
            }
            return "000001";
        }
    }
}