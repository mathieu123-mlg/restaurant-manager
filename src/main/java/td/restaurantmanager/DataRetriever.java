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

            Map<Integer, Ingredient> ingredientMap = new LinkedHashMap<>();
            while (rs.next()) {
                Integer ingredientId = rs.getInt("id_ingredient");
                if (!ingredientMap.containsKey(ingredientId)) {
                    ingredientMap.put(ingredientId, extractIngredientFromResultSet(rs));
                }
            }

            return new ArrayList<>(ingredientMap.values());
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredients", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return Collections.emptyList();
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                    INSERT INTO ingredient (id, name, price, category)
                    VALUES (?, ?, ?, ?::ingredient_category)
                    RETURNING id, name, price, category""";

            List<Ingredient> savedIngredients = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient newIngredient : newIngredients) {
                    int nextId = nextId(conn, "ingredient");

                    ps.setInt(1, nextId);
                    ps.setString(2, newIngredient.getName());
                    ps.setDouble(3, newIngredient.getPrice());
                    ps.setString(4, newIngredient.getCategory().name());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            savedIngredients.add(new Ingredient(
                                    rs.getInt("id"),
                                    rs.getString("name"),
                                    rs.getDouble("price"),
                                    CategoryEnum.valueOf(rs.getString("category"))
                            ));
                        }
                    }
                }
                conn.commit();
                return savedIngredients;
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // Ignore rollback exception
            }
            throw new RuntimeException("Error creating ingredients", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Dish saveDish(Dish dishToSave) {
        if (dishToSave == null) {
            throw new IllegalArgumentException("Dish cannot be null");
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);

            Integer dishId = upsertDish(conn, dishToSave);
            updateDishIngredients(conn, dishId, dishToSave.getDishIngredients());

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // Ignore rollback exception
            }
            throw new RuntimeException("Error saving dish", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private Integer upsertDish(Connection conn, Dish dish) throws SQLException {
        String sql = """
                INSERT INTO dish (id, name, dish_type, selling_price)
                VALUES (?, ?, ?::dish_type, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    dish_type = EXCLUDED.dish_type,
                    selling_price = EXCLUDED.selling_price
                RETURNING id""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Integer dishId = dish.getId();
            if (dishId == null || dishId <= 0) {
                dishId = nextId(conn, "dish");
            }

            ps.setInt(1, dishId);
            ps.setString(2, dish.getName());
            ps.setString(3, dish.getDishType().name());
            ps.setDouble(4, dish.getSellingPrice());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Failed to upsert dish");
                }
            }
        }
    }

    private void updateDishIngredients(Connection conn, Integer dishId, List<DishIngredient> dishIngredients)
            throws SQLException {
        if (dishId == null) {
            throw new IllegalArgumentException("Dish ID cannot be null");
        }

        // Delete existing dish ingredients
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }

        // Insert new dish ingredients if provided
        if (dishIngredients != null && !dishIngredients.isEmpty()) {
            String insertSql = """
                    INSERT INTO dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
                    VALUES (?, ?, ?, ?, ?::unit_type)""";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int nextId = nextId(conn, "dish_ingredient");

                for (DishIngredient di : dishIngredients) {
                    // Ensure ingredient exists
                    ensureIngredientExists(conn, di.getIngredient());

                    ps.setInt(1, nextId++);
                    ps.setInt(2, dishId);
                    ps.setInt(3, di.getIngredient().getId());
                    ps.setDouble(4, di.getQuantityRequired());
                    ps.setString(5, di.getUnit().name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private void ensureIngredientExists(Connection conn, Ingredient ingredient) throws SQLException {
        if (ingredient.getId() == null || !ingredientExists(conn, ingredient.getId())) {
            saveIngredientInternal(conn, ingredient);
        }
    }

    private boolean ingredientExists(Connection conn, Integer ingredientId) throws SQLException {
        String sql = "SELECT 1 FROM ingredient WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Order saveOrder(Order orderToSave) {
        if (orderToSave == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);

            // Check stock availability
            checkStockAvailability(orderToSave);

            // Save order
            Integer orderId = upsertOrder(conn, orderToSave);

            // Update dish orders
            updateDishOrders(conn, orderId, orderToSave.getDishOrders());

            // Deduct stock
            deductStockForOrder(conn, orderToSave.getDishOrders());

            conn.commit();
            return findOrderById(orderId);
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // Ignore rollback exception
            }
            throw new RuntimeException("Failed to save order", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private Integer upsertOrder(Connection conn, Order order) throws SQLException {
        String sql = """
                INSERT INTO "order" (id, reference, creation_datetime)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    reference = EXCLUDED.reference,
                    creation_datetime = EXCLUDED.creation_datetime
                RETURNING id""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Integer orderId = order.getId();
            if (orderId == null || orderId <= 0) {
                orderId = nextId(conn, "order");
            }

            String reference = order.getReference();
            if (reference == null || reference.trim().isEmpty()) {
                reference = generateOrderReference(conn);
            }

            Instant creationDatetime = order.getCreationDatetime();
            if (creationDatetime == null) {
                creationDatetime = Instant.now();
            }

            ps.setInt(1, orderId);
            ps.setString(2, reference);
            ps.setTimestamp(3, Timestamp.from(creationDatetime));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Failed to upsert order");
                }
            }
        }
    }

    private void updateDishOrders(Connection conn, Integer orderId, List<DishOrder> dishOrders)
            throws SQLException {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        // Delete existing dish orders
        String deleteSql = "DELETE FROM dish_order WHERE id_order = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }

        // Insert new dish orders if provided
        if (dishOrders != null && !dishOrders.isEmpty()) {
            String insertSql = """
                    INSERT INTO dish_order (id, id_order, id_dish, quantity)
                    VALUES (?, ?, ?, ?)""";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int nextId = nextId(conn, "dish_order");

                for (DishOrder dishOrder : dishOrders) {
                    ps.setInt(1, nextId++);
                    ps.setInt(2, orderId);
                    ps.setInt(3, dishOrder.getDish().getId());
                    ps.setInt(4, dishOrder.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private void checkStockAvailability(Order order) {
        if (order.getDishOrders() == null || order.getDishOrders().isEmpty()) {
            return;
        }

        for (DishOrder dishOrder : order.getDishOrders()) {
            Dish dish = findDishById(dishOrder.getDish().getId());

            if (dish.getDishIngredients() != null) {
                for (DishIngredient dishIngredient : dish.getDishIngredients()) {
                    double requiredQuantity = dishIngredient.getQuantityRequired() * dishOrder.getQuantity();
                    double availableQuantity = calculateAvailableStock(dishIngredient.getIngredient().getId());

                    if (availableQuantity < requiredQuantity) {
                        throw new IllegalArgumentException(
                                String.format("Insufficient stock for ingredient '%s'. Required: %.2f, Available: %.2f",
                                        dishIngredient.getIngredient().getName(),
                                        requiredQuantity,
                                        availableQuantity)
                        );
                    }
                }
            }
        }
    }

    private double calculateAvailableStock(Integer ingredientId) {
        List<StockMovement> movements = getIngredientStockMovements(ingredientId);
        double total = 0.0;

        for (StockMovement movement : movements) {
            if (movement.getType() == MovementTypeEnum.IN) {
                total += movement.getValue().getQuantity();
            } else if (movement.getType() == MovementTypeEnum.OUT) {
                total -= movement.getValue().getQuantity();
            }
        }

        return total;
    }

    private void deductStockForOrder(Connection conn, List<DishOrder> dishOrders) throws SQLException {
        if (dishOrders == null || dishOrders.isEmpty()) {
            return;
        }

        String insertSql = """
                INSERT INTO stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime)
                VALUES (?, ?, ?, ?::unit_type, ?::movement_type, ?)""";

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            int nextMovementId = nextId(conn, "stock_movement");

            for (DishOrder dishOrder : dishOrders) {
                Dish dish = findDishById(dishOrder.getDish().getId());

                if (dish.getDishIngredients() != null) {
                    for (DishIngredient dishIngredient : dish.getDishIngredients()) {
                        double quantityToDeduct = dishIngredient.getQuantityRequired() * dishOrder.getQuantity();

                        ps.setInt(1, nextMovementId++);
                        ps.setInt(2, dishIngredient.getIngredient().getId());
                        ps.setDouble(3, quantityToDeduct);
                        ps.setString(4, dishIngredient.getUnit().name());
                        ps.setString(5, MovementTypeEnum.OUT.name());
                        ps.setTimestamp(6, Timestamp.from(Instant.now()));
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
    }

    public Order findOrderById(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }

        String sql = """
                SELECT o.id, o.reference, o.creation_datetime,
                       d_o.id as dish_order_id, d_o.id_dish, d_o.quantity,
                       d.name as dish_name, d.dish_type, d.selling_price
                FROM "order" o
                LEFT JOIN dish_order d_o ON o.id = d_o.id_order
                LEFT JOIN dish d ON d_o.id_dish = d.id
                WHERE o.id = ?
                ORDER BY d_o.id""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();

            Order order = null;
            List<DishOrder> dishOrders = new ArrayList<>();

            while (rs.next()) {
                if (order == null) {
                    order = new Order(
                            rs.getInt("id"),
                            rs.getString("reference"),
                            rs.getTimestamp("creation_datetime").toInstant(),
                            dishOrders
                    );
                }

                if (rs.getObject("id_dish") != null) {
                    Dish dish = new Dish(
                            rs.getInt("id_dish"),
                            rs.getString("dish_name"),
                            DishTypeEnum.valueOf(rs.getString("dish_type")),
                            rs.getDouble("selling_price"),
                            null  // We don't load ingredients here
                    );

                    dishOrders.add(new DishOrder(
                            rs.getInt("dish_order_id"),
                            dish,
                            rs.getInt("quantity")
                    ));
                }
            }

            if (order == null) {
                throw new RuntimeException("Order not found with id: " + orderId);
            }

            return order;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding order by id", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Dish> findDishByIngredientsName(String ingredientsName) {
        if (ingredientsName == null || ingredientsName.trim().isEmpty()) {
            throw new IllegalArgumentException("ingredientsName cannot be null or empty");
        }

        String sql = """
                SELECT DISTINCT d.id as id_dish, d.name as dish_name, d.dish_type, d.selling_price
                FROM dish d
                JOIN dish_ingredient di ON d.id = di.id_dish
                JOIN ingredient i ON di.id_ingredient = i.id
                WHERE i.name ILIKE ?
                ORDER BY d.id""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + ingredientsName.trim() + "%");
            ResultSet rs = ps.executeQuery();

            List<Dish> dishes = new ArrayList<>();
            while (rs.next()) {
                dishes.add(extractDishFromResultSet(rs));
            }

            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding dishes by ingredient name", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum category,
                                                      String dishName, Integer page, Integer size) {
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("Page must be greater than 0");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT i.id as id_ingredient, i.name as ingredient_name, i.price, i.category
                FROM ingredient i
                LEFT JOIN dish_ingredient di ON i.id = di.id_ingredient
                LEFT JOIN dish d ON di.id_dish = d.id
                WHERE 1 = 1""");

        List<Object> parameters = new ArrayList<>();

        if (ingredientName != null && !ingredientName.trim().isEmpty()) {
            sql.append(" AND i.name ILIKE ?");
            parameters.add("%" + ingredientName.trim() + "%");
        }

        if (category != null) {
            sql.append(" AND i.category = ?::ingredient_category");
            parameters.add(category.name());
        }

        if (dishName != null && !dishName.trim().isEmpty()) {
            sql.append(" AND d.name ILIKE ?");
            parameters.add("%" + dishName.trim() + "%");
        }

        sql.append(" ORDER BY i.id");

        if (page != null && size != null) {
            sql.append(" LIMIT ? OFFSET ?");
            parameters.add(size);
            parameters.add((page - 1) * size);
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql.toString());

            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = ps.executeQuery();

            List<Ingredient> ingredients = new ArrayList<>();
            while (rs.next()) {
                ingredients.add(extractIngredientFromResultSet(rs));
            }

            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredients by criteria", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Ingredient saveIngredient(Ingredient ingredientToSave) {
        if (ingredientToSave == null) {
            throw new IllegalArgumentException("Ingredient cannot be null");
        }

        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);

            Ingredient savedIngredient = saveIngredientInternal(conn, ingredientToSave);

            // Save stock movements if provided
            if (ingredientToSave.getStockMovementList() != null &&
                !ingredientToSave.getStockMovementList().isEmpty()) {
                saveStockMovements(conn, savedIngredient.getId(), ingredientToSave.getStockMovementList());
            }

            conn.commit();
            return findIngredientById(savedIngredient.getId());
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                // Ignore rollback exception
            }
            throw new RuntimeException("Error saving ingredient", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private Ingredient saveIngredientInternal(Connection conn, Ingredient ingredient) throws SQLException {
        String sql = """
                INSERT INTO ingredient (id, name, price, category)
                VALUES (?, ?, ?, ?::ingredient_category)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    price = EXCLUDED.price,
                    category = EXCLUDED.category::ingredient_category
                RETURNING id, name, price, category""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Integer ingredientId = ingredient.getId();
            if (ingredientId == null || ingredientId <= 0) {
                ingredientId = nextId(conn, "ingredient");
            }

            ps.setInt(1, ingredientId);
            ps.setString(2, ingredient.getName());
            ps.setDouble(3, ingredient.getPrice());
            ps.setString(4, ingredient.getCategory().name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Ingredient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            CategoryEnum.valueOf(rs.getString("category"))
                    );
                } else {
                    throw new SQLException("Failed to save ingredient");
                }
            }
        }
    }

    private void saveStockMovements(Connection conn, Integer ingredientId, List<StockMovement> stockMovements)
            throws SQLException {
        if (stockMovements == null || stockMovements.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime)
                VALUES (?, ?, ?, ?::unit_type, ?::movement_type, ?)""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int nextId = nextId(conn, "stock_movement");

            for (StockMovement movement : stockMovements) {
                ps.setInt(1, nextId++);
                ps.setInt(2, ingredientId);
                ps.setDouble(3, movement.getValue().getQuantity());
                ps.setString(4, movement.getValue().getUnit().name());
                ps.setString(5, movement.getType().name());
                ps.setTimestamp(6, Timestamp.from(movement.getCreationDatetime()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<StockMovement> getIngredientStockMovements(Integer ingredientId) {
        if (ingredientId == null || ingredientId <= 0) {
            throw new IllegalArgumentException("Ingredient ID must be positive");
        }

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
            throw new RuntimeException("Error getting ingredient stock movements", e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private Ingredient findIngredientById(Integer ingredientId) {
        if (ingredientId == null || ingredientId <= 0) {
            throw new IllegalArgumentException("Ingredient ID must be positive");
        }

        String sql = """
                SELECT id, name, price, category
                FROM ingredient
                WHERE id = ?""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return extractIngredientFromResultSet(rs);
            } else {
                throw new RuntimeException("Ingredient not found with id: " + ingredientId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredient by id", e);
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
                null  // Ingredients are loaded separately
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