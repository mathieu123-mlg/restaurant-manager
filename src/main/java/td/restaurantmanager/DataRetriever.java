package td.restaurantmanager;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    private static String getSql(Integer page, Integer size, boolean hasPagination) {
        String sql = """
                SELECT di.id, i.id as id_ingredient, i.name as ingredient_name, i.price, i.category,
                       di.quantity_required, di.unit, di.id_dish
                FROM dish_ingredient di
                right JOIN ingredient i on di.id_ingredient = i.id
                order by i.id""";

        if (hasPagination) {
            if (page <= 0 || size <= 0) {
                throw new IllegalArgumentException("Page and size must be greater than 0");
            }
            sql += " LIMIT ? OFFSET ?";
        }
        return sql;
    }

    public Dish findDishById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        String sql = """
                SELECT di.id, d.id as id_dish, d.name as dish_name, d.dish_type, d.selling_price,
                       di.id_ingredient, i.name as ingredient_name, i.price, i.category, di.quantity_required, di.unit
                FROM dish d
                left JOIN dish_ingredient di ON di.id_dish = d.id
                left JOIN ingredient i ON di.id_ingredient = i.id
                WHERE d.id = ?""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            Dish dish = null;
            List<DishIngredient> dishIngredients = new ArrayList<>();

            while (rs.next()) {
                if (dish == null) {
                    dish = getDishFromDb(rs, dishIngredients);
                }
                if (rs.getInt("id_ingredient") > 0) {
                    dishIngredients.add(getDishIngredientFromDb(rs, dish));
                }
            }

            if (dish == null) {
                throw new RuntimeException("Dish not found: id=" + id);
            }

            return dish;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Ingredient> findIngredients(Integer page, Integer size) {
        boolean hasPagination = (page != null && size != null);

        String sql = getSql(page, size, hasPagination);
        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            if (hasPagination) {
                ps.setInt(1, size);
                ps.setInt(2, (page - 1) * size);
            }
            ResultSet rs = ps.executeQuery();

            List<Ingredient> ingredientsFromDB = new ArrayList<>();
            while (rs.next()) {
                ingredientsFromDB.add(getIngredientFromDB(rs));
            }

            return ingredientsFromDB;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }

        List<Ingredient> savedIngredients = new ArrayList<>();
        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                    INSERT INTO ingredient (id, name, price, category)
                    VALUES (?, ?, ?, ?::ingredient_category)
                    RETURNING id;""";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient newIngredient : newIngredients) {
                    buildIngredientStatement(newIngredient, ps);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        savedIngredients.add(
                                new Ingredient(
                                        rs.getInt("id"),
                                        newIngredient.getName(),
                                        newIngredient.getPrice(),
                                        newIngredient.getCategory()
                                )
                        );
                    }
                }

                conn.commit();
                return savedIngredients;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Dish saveDish(Dish dishToSave) {
        if (dishToSave == null) {
            throw new IllegalArgumentException("Dish cannot be null");
        }

        String upsertDishSql = """
                INSERT INTO dish (id, selling_price, name, dish_type)
                VALUES (?, ?, ?, ?::dish_type)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    selling_price = EXCLUDED.selling_price,
                    dish_type = EXCLUDED.dish_type
                RETURNING id""";

        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);
            Integer dishId;

            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (dishToSave.getId() != null) {
                    ps.setInt(1, dishToSave.getId());
                } else {
                    ps.setInt(1, next_id("dish"));
                }

                ps.setObject(2, dishToSave.getSellingPrice(), Types.DOUBLE);
                ps.setString(3, dishToSave.getName());
                ps.setString(4, dishToSave.getDishType().name());

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<DishIngredient> dishIngredients = dishToSave.getDishIngredients();

            manageDishIngredients(conn, dishId, dishIngredients);

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                e.addSuppressed(ex);
            }
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Dish> findDishByIngredientsName(String ingredientsName) {
        if (ingredientsName == null || ingredientsName.trim().isEmpty()) {
            throw new IllegalArgumentException("ingredientsName is null or empty");
        }
        String sql = """
                select di.id, dish.id as id_dish, dish.name as dish_name, dish.dish_type, dish.selling_price,
                       di.id_ingredient, i.name as ingredient_name, i.price, i.category, di.quantity_required, di.unit
                from dish
                left join dish_ingredient di on di.id_dish = dish.id
                left join ingredient i on di.id_ingredient = i.id
                where i.name ilike ?
                order by dish.id;""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + ingredientsName + "%");
            ResultSet rs = ps.executeQuery();

            List<Dish> dishFromDatabase = new ArrayList<>();
            List<DishIngredient> dishIngredients = new ArrayList<>();
            Dish dish = null;
            while (rs.next()) {
                if (dish == null) {
                    dish = getDishFromDb(rs, dishIngredients);
                }
                dishIngredients.add(getDishIngredientFromDb(rs, dish));
                dishFromDatabase.add(dish);
            }

            return dishFromDatabase;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum category, String dishName, int page, int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("page or size equal zero");
        }
        StringBuilder sql = new StringBuilder("""
                select i.id as id_ingredient, i.name as ingredient_name, i.price, i.category,
                    di.id_dish, di.quantity_required, di.unit
                from ingredient i
                left join dish_ingredient di on i.id = di.id_ingredient
                left join dish d on d.id = di.id_dish
                where 1 = 1""");

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = buildIngredientsSearchStatement(sql, ingredientName, category, dishName, conn, page, size);
            ResultSet rs = ps.executeQuery();

            List<Ingredient> ingredientsByCriteria = new ArrayList<>();
            while (rs.next()) {
                ingredientsByCriteria.add(getIngredientFromDB(rs));
            }

            return ingredientsByCriteria;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Ingredient saveIngredient(Ingredient ingredientToSave) {
        if (ingredientToSave == null) {
            throw new IllegalArgumentException("ingredientToSave is null");
        }

        String sql = """
                insert into ingredient (id, name, price, category)
                values (?, ? ,?, ?::ingredient_category)
                on conflict (id) do update
                set name = EXCLUDED.name,
                    price = EXCLUDED.price,
                    category = EXCLUDED.category
                returning id;""";
        Connection conn = dbConnection.getDBConnection();

        try {
            conn.setAutoCommit(false);
            Integer ingredientId;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, ingredientToSave.getId());
                ps.setString(2, ingredientToSave.getName());
                if (ingredientToSave.getPrice() == null) {
                    ps.setNull(3, Types.DOUBLE);
                } else {
                    ps.setDouble(3, ingredientToSave.getPrice());
                }
                if (ingredientToSave.getCategory() == null) {
                    ps.setNull(4, Types.VARCHAR);
                } else {
                    ps.setString(4, ingredientToSave.getCategory().name());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ingredientId = rs.getInt(1);
                }

                List<StockMovement> stockMovementList = ingredientToSave.getStockMovementList();

                if (stockMovementList != null && !stockMovementList.isEmpty()) {
                    saveStockMovements(conn, ingredientId, ingredientToSave.getStockMovementList());
                }

                conn.commit();
                return new Ingredient(
                        ingredientId,
                        ingredientToSave.getName(),
                        ingredientToSave.getPrice(),
                        ingredientToSave.getCategory(),
                        getIngredientStockMovements(ingredientId)
                );
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

//    public Dish saveDish(Dish dishToSave) {
//        if (dishToSave == null) {
//            throw new IllegalArgumentException("Dish cannot be null");
//        }
//        String upsertDishSql = """
//                INSERT INTO dish (id, selling_price, name, dish_type)
//                VALUES (?, ?, ?, ?::dish_type)
//                ON CONFLICT (id) DO UPDATE
//                SET name = EXCLUDED.name,
//                    selling_price = EXCLUDED.selling_price,
//                    dish_type = EXCLUDED.dish_type
//                RETURNING id""";
//
//        Connection conn = dbConnection.getDBConnection();
//        try {
//            conn.setAutoCommit(false);
//            Integer dishId;
//
//            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
//                if (dishToSave.getId() != null) {
//                    ps.setInt(1, dishToSave.getId());
//                } else {
//                    ps.setInt(1, next_id("dish"));
//                }
//                if (dishToSave.getSellingPrice() != null) {
//                    ps.setDouble(2, dishToSave.getSellingPrice());
//                } else {
//                    ps.setNull(2, Types.DOUBLE);
//                }
//
//                ps.setString(3, dishToSave.getName());
//                ps.setString(4, dishToSave.getDishType().name());
//
//                try (ResultSet rs = ps.executeQuery()) {
//                    rs.next();
//                    dishId = rs.getInt(1);
//                }
//            }
//
//            List<DishIngredient> newDishIngredients = dishToSave.getDishIngredients();
//
//            detachIngredients(conn, dishId, newDishIngredients);
//            attachIngredients(conn, dishId, newDishIngredients);
//
//            conn.commit();
//            return findDishById(dishId);
//        } catch (SQLException e) {
//            try {
//                conn.rollback();
//            } catch (SQLException ex) {
//                e.addSuppressed(ex);
//            }
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }

    //    public Ingredient saveIngredient(Ingredient ingredientTosave) {
//        Connection conn = dbConnection.getDBConnection()
//        try {
//            PreparedStatement ps = conn.prepareStatement("""
//                    INSERT INTO ingredient (id, name, price, category)
//                    VALUES (?, ?, ?, ?)
//                    ON conflict do nothing""");
//
//            ps.setInt(1, ingredientTosave.getId());
//            ps.setString(2, ingredientTosave.getName());
//            ps.setDouble(3, ingredientTosave.getPrice());
//            ps.setString(4, ingredientTosave.getCategory().toString());
//            ResultSet rs = ps.executeQuery();
//
//            Ingredient ingredient = getIngredients(rs).get(0);
//            insertIntoStockMovement(ingredient);
//            return ingredient;
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
//    private StockMovement insertIntoStockMovement(Ingredient ingredient) {
//        Connection conn = dbConnection.getDBConnection();
//        try {
//            PreparedStatement ps = conn.prepareStatement("""
//                    insert into stock_movement (id, id_ingredient, quantity, unit, creation_datetime)
//                    values (?, ?, ?::movement_type, ?::unit_type, ?)
//                    on conflict (id) do update
//                    set id_ingredient = excluded.id_ingredient,
//                        quantity = excluded.quantity,
//                        unit = excluded.unit,
//                        creation_datetime = excluded.creation_datetime
//                    returning id, id_ingredient, quantity, unit, creation_datetime;""");
//
//            ResultSet rs = ps.executeQuery();
//            if (rs.next()) {
//                return new StockMovement(
//                        rs.getInt("id"),
//                        ingredient,
//                        UnitType.valueOf(rs.getString("type")),
//                        rs.getObject("quantity") == null ? null : rs.getDouble("quantity"),
//                        Instant.parse(rs.getString("creation_datetime"))
//                );
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
//    public StockValue getStockValueAt(Instant instant) {
//        return null;
//    }
//

    private Ingredient getIngredientFromDB(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("id_ingredient"),
                rs.getString("ingredient_name"),
                getNullableDouble(rs, "price"),
                CategoryEnum.valueOf(rs.getString("category")));
    }

    private PreparedStatement buildIngredientsSearchStatement(
            StringBuilder sql,
            String ingredientName,
            CategoryEnum category,
            String dishName,
            Connection conn,
            int page,
            int size) throws SQLException {

        List<Object> params = new ArrayList<>();

        if (ingredientName != null) {
            sql.append(" AND i.name ILIKE ?");
            params.add("%" + ingredientName.trim() + "%");
        }

        if (category != null) {
            sql.append(" AND i.category::varchar(20) ILIKE ?");
            params.add("%" + category.name() + "%");
        }

        if (dishName != null) {
            sql.append(" AND d.name ILIKE ?");
            params.add("%" + dishName.trim() + "%");
        }

        sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");
        PreparedStatement ps = conn.prepareStatement(sql.toString());

        int index = 1;
        for (Object param : params) {
            ps.setObject(index++, param);
        }

        ps.setInt(index++, size);
        ps.setInt(index, (page - 1) * size);

        return ps;
    }

//    private void detachIngredients(Connection conn, Integer dishId, List<DishIngredient> ingredientsToSave)
//            throws SQLException {
//
//        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {
//            return;
//        }
//
//        List<Ingredient> ingredients = ingredientsToSave
//                .stream()
//                .map(DishIngredient::getIngredient)
//                .toList();
//
//        String baseSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
//
//        if (!ingredientsToSave.isEmpty()) {
//            baseSql += " AND id_ingredient NOT IN (%s) ";
//
//            String inClause = ingredients.stream()
//                    .map(x -> "?")
//                    .collect(Collectors.joining(","));
//
//            baseSql = String.format(baseSql, inClause);
//        }
//
//        try (PreparedStatement ps = conn.prepareStatement(baseSql)) {
//            ps.setInt(1, dishId);
//            int paramIndex = 2;
//
//            if (!ingredientsToSave.isEmpty()) {
//                for (Ingredient ingredient : ingredients) {
//                    ps.setInt(paramIndex++, ingredient.getId());
//                }
//            }
//            ps.executeUpdate();
//        }
//    }

//    private void attachIngredients(Connection conn, Integer dishId, List<DishIngredient> ingredientsToSave)
//            throws SQLException {
//
//        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {
//            return;
//        }
//
//        List<Ingredient> ingredientsListFromDB = findIngredients(null, null);
//        List<Ingredient> newIngredientsToSave = new ArrayList<>();
//
//        List<Ingredient> ingredients = ingredientsToSave
//                .stream()
//                .map(DishIngredient::getIngredient)
//                .toList();
//
//        ingredients
//                .forEach(ingredient -> {
//                    if (!ingredientsListFromDB.contains(ingredient)) {
//                        newIngredientsToSave.add(ingredient);
//                    }
//                });
//
//        if (!newIngredientsToSave.isEmpty()) {
//            createIngredients(newIngredientsToSave);
//        }
//
//        if (newIngredientsToSave.size() < ingredientsToSave.size()) {
//            insertDishIngredient(dishId, ingredientsToSave, conn);
//            updateDishIngredient(dishId, ingredientsToSave, conn);
//        }
//    }

//    private void insertDishIngredient(Integer dishId, List<DishIngredient> newIngredientsToSave, Connection conn)
//            throws SQLException {
//
//        String insertSql = """
//                INSERT INTO dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
//                values (?, ?, ?, ?, ?::unit_type)
//                on conflict (id_dish, id_ingredient) do nothing;""";
//
//        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
//
//            for (DishIngredient ingredient : newIngredientsToSave) {
//                ps.setInt(1, next_id("dish_ingredient"));
//                ps.setInt(2, dishId);
//                ps.setInt(3, ingredient.getIngredient().getId());
//
//                if (ingredient.getQuantityRequired() != null) {
//                    ps.setDouble(4, ingredient.getQuantityRequired());
//                } else {
//                    ps.setNull(4, Types.DOUBLE);
//                }
//
//                ps.setString(5, ingredient.getUnit().name());
//                ps.addBatch();
//            }
//            ps.executeBatch();
//        }
//    }

//    private void updateDishIngredient(Integer dishId, List<DishIngredient> ingredientsToSave, Connection conn)
//            throws SQLException {
//
//        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {
//            return;
//        }
//
//        String upsert = """
//                UPDATE dish_ingredient
//                SET quantity_required = ?,
//                    unit = ?::unit_type
//                WHERE id_dish = ? and id_ingredient = ?;""";
//
//        try (PreparedStatement ps = conn.prepareStatement(upsert)) {
//            for (DishIngredient ingredient : ingredientsToSave) {
//                if (ingredient.getQuantityRequired() != null) {
//                    ps.setDouble(1, ingredient.getQuantityRequired());
//                } else {
//                    ps.setNull(1, Types.DOUBLE);
//                }
//                ps.setString(2, ingredient.getUnit().name());
//                ps.setInt(3, dishId);
//                ps.setInt(4, ingredient.getId());
//
//                ps.addBatch();
//            }
//            ps.executeBatch();
//        }
//    }

//    protected void resetData() {
//        String deleteIngredient = "DELETE FROM ingredient;";
//        String deleteDish = "DELETE FROM dish;";
//
//        String resetSequenceDish = "ALTER SEQUENCE dish_id_seq RESTART WITH 1;";
//        String resetSequenceIngredient = "ALTER SEQUENCE ingredient_id_seq RESTART WITH 1;";
//
//        String initialDish = """
//                insert into dish (id, name, dish_type, selling_price)
//                VALUES (1, 'Salade fraîche', 'STARTER', 3500.00),
//                       (2, 'Poulet grillé', 'MAIN', 12000.00),
//                       (3, 'Riz au légume', 'MAIN', null),
//                       (4, 'Gâteau aux chocolat', 'DESSERT', 8000.00),
//                       (5, 'Salade de fruits', 'DESSERT', null);
//                """;
//
//        String initialIngredient = """
//                INSERT INTO ingredient (id, name, price, category)
//                VALUES (1, 'Laitue', 800.00, 'VEGETABLE'),
//                       (2, 'Tomate', 600.00, 'VEGETABLE'),
//                       (3, 'Poulet', 4500.00, 'ANIMAL'),
//                       (4, 'Chocolat', 3000.00, 'OTHER'),
//                       (5, 'Beurre', 2500.00, 'DAIRY');
//                """;
//        Connection conn = dbConnection.getDBConnection();
//
//        try {
//            conn.setAutoCommit(false);
//            Statement stmt = conn.createStatement();
//            stmt.executeUpdate(deleteIngredient);
//            stmt.executeUpdate(deleteDish);
//
//            stmt.executeUpdate(resetSequenceDish);
//            stmt.executeUpdate(resetSequenceIngredient);
//
//            stmt.executeUpdate(initialDish);
//            stmt.executeUpdate(initialIngredient);
//
//            conn.commit();
//
//        } catch (SQLException e) {
//            try {
//                conn.rollback();
//                throw new RuntimeException("Erreur lors de la réinitialisation des données", e);
//            } catch (SQLException ex) {
//                throw new RuntimeException(ex);
//            }
//        } finally {
//            try {
//                conn.setAutoCommit(true);
//            } catch (SQLException _) {
//            }
//            dbConnection.closeDBConnection(conn);
//        }
//    }

    private Integer next_id(String table) {
        Connection conn = dbConnection.getDBConnection();
        try {
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery("""
                    SELECT COALESCE(MAX(id), 0) + 1 FROM %s""".formatted(table));

            return rs.next() ? (rs.getInt(1) + 1) : 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private Dish getDishFromDb(ResultSet rs, List<DishIngredient> dishIngredients) throws SQLException {
        return new Dish(
                rs.getInt("id_dish"),
                rs.getString("dish_name"),
                DishTypeEnum.valueOf(rs.getString("dish_type")),
                getNullableDouble(rs, "selling_price"),
                dishIngredients
        );
    }

    private DishIngredient getDishIngredientFromDb(ResultSet rs, Dish dish) throws SQLException {
        return new DishIngredient(
                rs.getInt("id"),
                dish,
                getIngredientFromDB(rs),
                getNullableDouble(rs, "quantity_required"),
                UnitType.valueOf(rs.getString("unit"))
        );
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column) == null ? null : rs.getDouble(column);
    }

    private void buildIngredientStatement(Ingredient ingredient, PreparedStatement preparedStatement)
            throws SQLException {
        preparedStatement.setInt(1, ingredient.getId());
        preparedStatement.setString(2, ingredient.getName());
        preparedStatement.setDouble(3, ingredient.getPrice());
        preparedStatement.setString(4, ingredient.getCategory().toString());
    }

    private void manageDishIngredients(Connection conn, Integer dishId, List<DishIngredient> dishIngredients)
            throws SQLException {

        if (dishIngredients == null || dishIngredients.isEmpty()) {
            deleteAllDishIngredients(conn, dishId);
            return;
        }

        List<Ingredient> existingIngredients = findIngredients(null, null);
        List<Ingredient> newIngredients = new ArrayList<>();
        Map<Integer, DishIngredient> ingredientsToKeep = new HashMap<>();

        for (DishIngredient di : dishIngredients) {
            Ingredient ingredient = di.getIngredient();

            if (!existingIngredients.contains(ingredient)) {
                newIngredients.add(ingredient);
            }
            ingredientsToKeep.put(ingredient.getId(), di);
        }

        if (!newIngredients.isEmpty()) {
            createNewIngredients(conn, newIngredients);
        }

        deleteRemovedIngredients(conn, dishId, ingredientsToKeep.keySet());
        upsertDishIngredients(conn, dishId, dishIngredients);
    }

    private void deleteAllDishIngredients(Connection conn, Integer dishId) throws SQLException {
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }
    }

    private void deleteRemovedIngredients(Connection conn, Integer dishId, Set<Integer> ingredientIdsToKeep)
            throws SQLException {

        StringBuilder deleteSql = new StringBuilder("DELETE FROM dish_ingredient WHERE id_dish = ?");

        if (!ingredientIdsToKeep.isEmpty()) {
            String placeholders = ingredientIdsToKeep.stream()
                    .map(x -> "?")
                    .collect(Collectors.joining(","));
            deleteSql.append(" AND id_ingredient NOT IN (").append(placeholders).append(")");
        }

        try (PreparedStatement ps = conn.prepareStatement(deleteSql.toString())) {
            ps.setInt(1, dishId);

            int paramIndex = 2;
            for (Integer id : ingredientIdsToKeep) {
                ps.setInt(paramIndex++, id);
            }

            ps.executeUpdate();
        }
    }

    private void createNewIngredients(Connection conn, List<Ingredient> newIngredients) throws SQLException {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return;
        }

        String insertSql = """
                INSERT INTO ingredient (id, name, price, category)
                VALUES (?, ?, ?, ?::ingredient_category)
                ON CONFLICT (id) DO NOTHING""";

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Ingredient ingredient : newIngredients) {
                ps.setInt(1, ingredient.getId());
                ps.setString(2, ingredient.getName());
                ps.setDouble(3, ingredient.getPrice());
                ps.setString(4, ingredient.getCategory().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void upsertDishIngredients(Connection conn, Integer dishId, List<DishIngredient> dishIngredients)
            throws SQLException {

        String upsertSql = """
                INSERT INTO dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
                VALUES (?, ?, ?, ?, ?::unit_type)
                ON CONFLICT (id_dish, id_ingredient) DO UPDATE
                SET quantity_required = EXCLUDED.quantity_required,
                    unit = EXCLUDED.unit""";

        try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
            int nextId = next_id("dish_ingredient");

            for (DishIngredient di : dishIngredients) {
                ps.setInt(1, nextId++);
                ps.setInt(2, dishId);
                ps.setInt(3, di.getIngredient().getId());
                ps.setObject(4, di.getQuantityRequired(), Types.DOUBLE);
                ps.setString(5, di.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveStockMovements(Connection conn, Integer ingredientId, List<StockMovement> stockMovements)
            throws SQLException {

        if (stockMovements == null || stockMovements.isEmpty()) {
            return;
        }

        String insertMovementSql = """
                INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
                VALUES (?, ?, ?, ?::movement_type, ?::unit_type, ?)
                ON CONFLICT (id) DO nothing""";

        try (PreparedStatement ps = conn.prepareStatement(insertMovementSql)) {
            for (StockMovement movement : stockMovements) {
                if (movement.getId() == null) {
                    ps.setInt(1, next_id("stock_movement"));
                } else {
                    ps.setInt(1, movement.getId());
                }

                ps.setInt(2, ingredientId);

                if (movement.getValue() != null && movement.getValue().getQuantity() != null) {
                    ps.setDouble(3, movement.getValue().getQuantity());
                } else {
                    ps.setNull(3, Types.DOUBLE);
                }

                ps.setString(4, movement.getType().name());

                if (movement.getValue() != null) {
                    ps.setString(5, movement.getValue().getUnit().name());
                } else {
                    ps.setString(5, UnitType.KG.name());
                }

                Timestamp timestamp = movement.getCreationDatetime() != null
                        ? Timestamp.from(movement.getCreationDatetime())
                        : Timestamp.from(Instant.now());
                ps.setTimestamp(6, timestamp);
                ps.executeUpdate();
            }
        }
    }

    public List<StockMovement> getIngredientStockMovements(Integer ingredientId) {
        if (ingredientId == null) {
            throw new IllegalArgumentException("ingredientId cannot be null");
        }

        String sql = """
                SELECT id, quantity, type, unit, creation_datetime
                FROM stock_movement
                WHERE id_ingredient = ?
                ORDER BY creation_datetime""";

        Connection conn = dbConnection.getDBConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);

            List<StockMovement> movements = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement movement = new StockMovement(
                            rs.getInt("id"),
                            new StockValue(
                                    rs.getDouble("quantity"),
                                    UnitType.valueOf(rs.getString("unit"))
                            ),
                            MouvementTypeEnum.valueOf(rs.getString("type")),
                            rs.getTimestamp("creation_datetime").toInstant()
                    );
                    movements.add(movement);
                }
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }
}
