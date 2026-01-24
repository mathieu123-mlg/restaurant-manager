package td.restaurantmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        String sql = """
                SELECT di.id, di.id_dish, d.name as dish_name, d.dish_type, d.selling_price,
                       di.id_ingredient, i.name as ingredient_name, i.price, i.category, di.quantity_required, di.unit
                FROM dish d
                JOIN dish_ingredient di ON di.id_dish = d.id
                JOIN ingredient i ON di.id_ingredient = i.id
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
                dishIngredients.add(getDishIngredientFromDb(rs, dish));
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

    public List<Ingredient> findIngredients(int page, int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Page and size must be greater than 0");
        }

        String sql = """
                SELECT i.id, i.name, i.price, i.category,
                       di.quantity_required, di.unit, di.id_dish
                FROM dish_ingredient di
                right JOIN ingredient i on di.id_ingredient = i.id
                order by i.id LIMIT ? OFFSET ?;""";

        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
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

    //
//    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
//        if (newIngredients == null || newIngredients.isEmpty()) {
//            return List.of();
//        }
//
//        List<Ingredient> savedIngredients = new ArrayList<>();
//        Connection conn = dbConnection.getDBConnection();
//        try {
//            conn.setAutoCommit(false);
//            String insertSql =
//                    """
//                            INSERT INTO ingredient (id, name, price, category)
//                            VALUES (?, ?, ?, ?::ingredient_category)
//                            RETURNING id;""";
//
//            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
//                for (Ingredient newIngredient : newIngredients) {
//                    buildIngredientsCreationStatement(newIngredient, ps);
//
//                    try (ResultSet rs = ps.executeQuery()) {
//                        rs.next();
//                        savedIngredients.add(
//                                new Ingredient(
//                                        rs.getInt("id"),
//                                        newIngredient.getName(),
//                                        newIngredient.getPrice(),
//                                        newIngredient.getCategory()
//                                )
//                        );
//                    }
//                }
//
//                conn.commit();
//                return savedIngredients;
//            } catch (SQLException e) {
//                conn.rollback();
//                throw new RuntimeException(e);
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
//    public Dish saveDish(Dish dishToSave) {
//        String upsertDishSql =
//                """
//                        INSERT INTO dish (id, selling_price, name, dish_type)
//                        VALUES (?, ?, ?, ?::dish_type)
//                        ON CONFLICT (id) DO UPDATE
//                        SET name = EXCLUDED.name,
//                            selling_price = EXCLUDED.selling_price,
//                            dish_type = EXCLUDED.dish_type
//                        RETURNING id""";
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
//                    ps.setInt(1, next_id());
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
//            List<Ingredient> newIngredients = dishToSave.getIngredients();
//            detachIngredients(conn, dishId, newIngredients);
//            attachIngredients(conn, dishId, newIngredients);
//
//            conn.commit();
//            return dishToSave;
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
//    public List<Dish> findDishByIngredientsName(String ingredientsName) {
//        if (ingredientsName == null || ingredientsName.trim().isEmpty()) {
//            throw new IllegalArgumentException("ingredientsName is null or empty");
//        }
//        String sql =
//                """
//                        select di.id_dish, dish.name, dish.dish_type, dish.selling_price
//                        from dish
//                        left join dish_ingredient di on di.id_dish = dish.id
//                        left join ingredient i on di.id_ingredient = i.id
//                        where i.name ilike ?
//                        order by dish.id;""";
//
//        Connection conn = dbConnection.getDBConnection();
//        try {
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ps.setString(1, "%" + ingredientsName + "%");
//            ResultSet rs = ps.executeQuery();
//
//            List<Dish> dishFromDatabase = new ArrayList<>();
//            while (rs.next()) {
//                Integer dishId = rs.getInt("id_dish");
//                Dish dish = new Dish(
//                        dishId,
//                        rs.getString("name"),
//                        DishTypeEnum.valueOf(rs.getString("dish_type")),
//                        rs.getObject("selling_price") == null ? null : rs.getDouble("selling_price"),
//                        findDishById(dishId).getDishIngredients()
//                );
//
//                dishFromDatabase.add(dish);
//            }
//
//            return dishFromDatabase;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
//    public List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum category, String dishName, int page, int size) {
//        if (page <= 0 || size <= 0) {
//            throw new IllegalArgumentException("page or size equal zero");
//        }
//        StringBuilder sql = new StringBuilder("""
//                select di.id_ingredient, i.name, i.price, i.category,
//                    di.id_dish, di.quantity_required, di.unit
//                from ingredient i
//                left join dish_ingredient di on i.id = di.id_ingredient
//                left join dish d on d.id = di.id_dish
//                where 1 = 1""");
//
//        Connection conn = dbConnection.getDBConnection();
//        try {
//            PreparedStatement ps = buildIngredientsSearchStatement(sql, ingredientName, category, dishName, conn, page, size);
//            ps.executeQuery();
//            ResultSet rs = ps.getResultSet();
//
//            return getIngredients(rs);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
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
//                    insert into stockmovement (id, id_ingredient, quantity, unit, creation_datetime)
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
                rs.getInt("id"),
                rs.getString("name"),
                getNullableDouble(rs, "price"),
                CategoryEnum.valueOf(rs.getString("category")),
                getNullableDouble(rs, "quantity_required"),
                rs.getString("unit") == null
                        ? UnitType.KG
                        : UnitType.valueOf(rs.getString("unit")));
    }
//
//    private void buildIngredientsCreationStatement(Ingredient newIngredient, PreparedStatement ps) {
//        try {
//            ps.setInt(1, newIngredient.getId());
//            ps.setString(2, newIngredient.getName());
//            ps.setDouble(3, newIngredient.getPrice());
//            ps.setString(4, newIngredient.getCategory().toString());
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private PreparedStatement buildIngredientsSearchStatement(
//            StringBuilder sql,
//            String ingredientName,
//            CategoryEnum category,
//            String dishName,
//            Connection conn,
//            int page,
//            int size) throws SQLException {
//
//        List<Object> params = new ArrayList<>();
//
//        if (ingredientName != null) {
//            sql.append(" AND i.name ILIKE ?");
//            params.add("%" + ingredientName.trim() + "%");
//        }
//
//        if (category != null) {
//            sql.append(" AND i.category::varchar(20) ILIKE ?");
//            params.add("%" + category.name() + "%");
//        }
//
//        if (dishName != null) {
//            sql.append(" AND d.name ILIKE ?");
//            params.add("%" + dishName.trim() + "%");
//        }
//
//        sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");
//        PreparedStatement ps = conn.prepareStatement(sql.toString());
//
//        int index = 1;
//        for (Object param : params) {
//            ps.setObject(index++, param);
//        }
//
//        ps.setInt(index++, size);
//        ps.setInt(index, (page - 1) * size);
//
//        return ps;
//    }
//
//    private Integer next_id() {
//        String sql = "select id from dish order by id desc limit 1";
//        Connection conn = dbConnection.getDBConnection();
//        try {
//            Statement stm = conn.createStatement();
//            ResultSet rs = stm.executeQuery(sql);
//
//            if (rs.next()) {
//                return rs.getInt(1) + 1;
//            } else {
//                return 1;
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            dbConnection.closeDBConnection(conn);
//        }
//    }
//
//    private void detachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredientsToSave)
//            throws SQLException {
//
//        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {
//
//            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_ingredient WHERE id_dish = ?")) {
//                ps.setInt(1, dishId);
//                ps.executeUpdate();
//                System.out.println("sql1 ?: DELETE FROM dish_ingredient WHERE id_dish = " + dishId);
//            }
//            return;
//        }
//
//        String baseSql = """
//                DELETE from dish_ingredient
//                WHERE id_dish = ? AND id NOT IN (%s)""";
//
//        String inClause = ingredientsToSave.stream()
//                .map(i -> "?")
//                .collect(Collectors.joining(","));
//
//        String sql = String.format(baseSql, inClause);
//
//        try (PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setInt(1, dishId);
//            int index = 2;
//            for (Ingredient ingredient : ingredientsToSave) {
//                ps.setInt(index++, ingredient.getId());
//            }
//            ps.executeUpdate();
//            System.out.println("baseSql ?: " + baseSql);
//        }
//    }
//
//    private void attachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredientsToSave)
//            throws SQLException {
//        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {
//            return;
//        }
//
//        String attachSql = """
//                    UPDATE dish_ingredient
//                    SET id_dish = ?
//                    WHERE id = ?
//                """;
//
//        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
//            for (Ingredient ingredient : ingredientsToSave) {
//                ps.setInt(1, dishId);
//                ps.setInt(2, ingredient.getId());
//                ps.addBatch();
//            }
//            ps.executeBatch();
//        }
//    }
//
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
}
