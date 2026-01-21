package td.restaurantmanager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Id must be positive integer");
        }

        String sql = """
                SELECT d.id, d.name, d.dish_type, d.price,
                       di.id_ingredient, di.quantity_required, di.unit,
                       i.name AS ing_name, i.price AS ing_price, i.category
                FROM dish d
                LEFT JOIN dish_ingredient di ON di.id_dish = d.id
                LEFT JOIN ingredient i ON i.id = di.id_ingredient
                WHERE d.id = ?""";
        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                Dish dish = null;
                List<Ingredient> ingredients = new ArrayList<>();

                while (rs.next()) {
                    if (dish == null) {
                        dish = new Dish(
                                rs.getInt("id"),
                                rs.getString("name"),
                                DishTypeEnum.valueOf(rs.getString("dish_type")),
                                rs.getObject("price") == null ? null : rs.getDouble("price"),
                                ingredients
                        );
                    }

                    if (rs.getObject("id_ingredient") != null) {
                        ingredients.add(new Ingredient(
                                rs.getInt("id_ingredient"),
                                rs.getString("ing_name"),
                                rs.getDouble("ing_price"),
                                CategoryEnum.valueOf(rs.getString("category")),
                                rs.getDouble("quantity_required"),
                                UnitType.valueOf(rs.getString("unit"))
                        ));
                    }
                }

                if (dish == null) {
                    throw new RuntimeException("Dish not found: id=" + id);
                }

                return dish;
            }
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

        String sql =
                """
                        SELECT id_ingredient, i.name, i.price, i.category, 
                               di.quantity_required, di.unit, di.id_dish
                        FROM dish_ingredient di
                        LEFT JOIN ingredient i on di.id_ingredient = i.id
                        order by i.id LIMIT ? OFFSET ? ;""";
        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            ResultSet rs = ps.executeQuery();

            return getIngredients(rs);
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
            String insertSql =
                    """
                            INSERT INTO ingredient (id, name, price, category) 
                            VALUES (?, ?, ?, ?::category)
                            RETURNING id;""";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient newIngredient : newIngredients) {
                    buildIngredientsCreationStatement(newIngredient, ps);

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
        String upsertDishSql =
                """
                        INSERT INTO dish (id, price, name, dish_type)
                        VALUES (?, ?, ?, ?::dish_type)
                        ON CONFLICT (id) DO UPDATE
                        SET name = EXCLUDED.name,
                            price = EXCLUDED.price,
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
                    ps.setInt(1, next_id());
                }
                if (dishToSave.getPrice() != null) {
                    ps.setDouble(2, dishToSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }

                ps.setString(3, dishToSave.getName());
                ps.setString(4, dishToSave.getDishType().name());

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<Ingredient> newIngredients = dishToSave.getIngredients();
            List<Ingredient> older_dish_ingredient = findDishById(dishToSave.getId()).getIngredients();
//
            detachIngredients(conn, dishId, older_dish_ingredient, newIngredients);
//            attachIngredients(conn, dishId, older_dish_ingredient, newIngredients);
//            conn.commit();
            return dishToSave;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public List<Dish> findDishByIngredientsName(String ingredientsName) {
        if (ingredientsName == null || ingredientsName.trim().isEmpty()) {
            throw new IllegalArgumentException("ingredientsName is null or empty");
        }
        String sql =
                """
                        select di.id_dish, dish.name, dish.dish_type, dish.price
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
            while (rs.next()) {
                Integer dishId = rs.getInt("id_dish");
                Dish dish = new Dish(
                        dishId,
                        rs.getString("name"),
                        DishTypeEnum.valueOf(rs.getString("dish_type")),
                        rs.getObject("price") == null ? null : rs.getDouble("price"),
                        findDishById(dishId).getIngredients()
                );

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
        StringBuilder sql = new StringBuilder(
                """
                        select di.id_ingredient, i.name, i.price, i.category,
                            di.id_dish, di.quantity_required, di.unit
                        from ingredient i
                        left join dish_ingredient di on i.id = di.id_ingredient
                        left join dish d on d.id = di.id_dish
                        where 1 = 1"""
        );
        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = buildIngredientsSearchStatement(
                    sql,
                    ingredientName,
                    category,
                    dishName,
                    conn,
                    page,
                    size
            );

            ps.executeQuery();
            ResultSet rs = ps.getResultSet();
            return getIngredients(rs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private List<Ingredient> getIngredients(ResultSet rs) throws SQLException {
        List<Ingredient> ingredientsFromDB = new ArrayList<>();
        while (rs.next()) {
            ingredientsFromDB.add(new Ingredient(
                    rs.getInt("id_ingredient"),
                    rs.getString("name"),
                    rs.getObject("price") == null ? null : rs.getDouble("price"),
                    CategoryEnum.valueOf(rs.getString("category")),
                    rs.getObject("quantity_required") == null ? null : rs.getDouble("quantity_required"),
                    UnitType.valueOf(rs.getString("unit"))
            ));
        }

        return ingredientsFromDB;
    }

    private void buildIngredientsCreationStatement(Ingredient newIngredient, PreparedStatement ps) {
        try {
            ps.setInt(1, newIngredient.getId());
            ps.setString(2, newIngredient.getName());
            ps.setDouble(3, newIngredient.getPrice());
            ps.setString(4, newIngredient.getCategory().toString());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
            sql.append(" AND i.category = ?");
            params.add(category.name());
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

    private Integer next_id() {
        String sql = "select id from dish order by id desc limit 1";
        Connection conn = dbConnection.getDBConnection();
        try {
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1) + 1;
            } else {
                return 1;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private List<Integer> notRattachedIngredientsID(List<Ingredient> ingredients, List<Ingredient> ingredientToSave) {
        if (ingredients == null || ingredientToSave == null || ingredientToSave.isEmpty() || ingredients.isEmpty()) {
            return List.of();
        }

        return ingredients.stream()
                .filter(i -> !ingredientToSave.contains(i))
                .filter(Objects::nonNull)
                .map(Ingredient::getId)
                .collect(Collectors.toList());
    }

    private List<Integer> rattachedIngredientsId(List<Ingredient> ingredients, List<Ingredient> ingredientToSave) {
        if (ingredients == null || ingredientToSave == null || ingredientToSave.isEmpty() || ingredients.isEmpty()) {
            return List.of();
        }

        return ingredients.stream()
                .filter(ingredientToSave::contains)
                .filter(Objects::nonNull)
                .map(i -> i.getId())
                .collect(Collectors.toList());
    }

    private void detachIngredients(Connection conn, Integer dishId, List<Ingredient> dish_ingredient, List<Ingredient> ingredientsToSave)
            throws SQLException {

        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_ingredient WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
                System.out.println("sql1 ?: DELETE FROM dish_ingredient WHERE id_dish = "+dishId);
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE ingredient SET id_dish = NULL WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
                System.out.println("sql2 ?: UPDATE ingredient SET id_dish = NULL WHERE id_dish = "+dishId);
            }

            return;
        }

        String delete_dish_ingredient = """
                DELETE FROM dish_ingredient WHERE id_dish = ? and id_ingredient = (%s);""";

        try {
            List<Integer> detached_ingredient_id = notRattachedIngredientsID(dish_ingredient, ingredientsToSave);

            String inClause = detached_ingredient_id.stream()
                    .map(i -> "?")
                    .collect(Collectors.joining(","));

            delete_dish_ingredient = String.format(
                    delete_dish_ingredient,
                    String.join(", ", inClause)
            );
            PreparedStatement ps = conn.prepareStatement(delete_dish_ingredient);
            ps.setInt(1, dishId);
            ps.executeUpdate();
            System.out.println("delete_dish_ingredient ?: " + delete_dish_ingredient);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String baseSql = """
                    UPDATE ingredient
                    SET id_dish = NULL
                    WHERE id_dish = ? AND id NOT IN (%s)
                """;

        String inClause = ingredientsToSave.stream()
                .map(i -> "?")
                .collect(Collectors.joining(","));

        String sql = String.format(baseSql, inClause);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            int index = 2;
            for (Ingredient ingredient : ingredientsToSave) {
                ps.setInt(index++, ingredient.getId());
            }
            ps.executeUpdate();
            System.out.println("baseSql ?: " + baseSql);
        }
    }

    private void attachIngredients(Connection conn, Integer dishId, List<Ingredient> dish_ingredient, List<Ingredient> ingredientsToSave)
            throws SQLException {
        if (ingredientsToSave == null || ingredientsToSave.isEmpty()) {
            return;
        }

        String attachSql = """
                    UPDATE ingredient
                    SET id_dish = ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (Ingredient ingredient : ingredientsToSave) {
                ps.setInt(1, dishId);
                ps.setInt(2, ingredient.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    protected void resetData() {
        String deleteIngredient = "DELETE FROM ingredient;";
        String deleteDish = "DELETE FROM dish;";

        String resetSequenceDish = "ALTER SEQUENCE dish_id_seq RESTART WITH 1;";
        String resetSequenceIngredient = "ALTER SEQUENCE ingredient_id_seq RESTART WITH 1;";

        String initialDish = """
                INSERT INTO dish (id, name, dish_type)
                VALUES (1, 'Salade fraîche', 'STARTER'),
                       (2, 'Poulet grillé', 'MAIN'),
                       (3, 'Riz au légume', 'MAIN'),
                       (4, 'Gâteau aux chocolat', 'DESSERT'),
                       (5, 'Salade de fruits', 'DESSERT');
                """;

        String initialIngredient = """
                INSERT INTO ingredient (id, name, price, category, id_dish)
                VALUES (1, 'Laitue', 800.00, 'VEGETABLE', 1),
                       (2, 'Tomate', 600.00, 'VEGETABLE', 1),
                       (3, 'Poulet', 4500.00, 'ANIMAL', 2),
                       (4, 'Chocolat', 3000.00, 'OTHER', 4),
                       (5, 'Beurre', 2500.00, 'DAIRY', 4);
                """;
        Connection conn = dbConnection.getDBConnection();

        try {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(deleteIngredient);
            stmt.executeUpdate(deleteDish);

            stmt.executeUpdate(resetSequenceDish);
            stmt.executeUpdate(resetSequenceIngredient);

            stmt.executeUpdate(initialDish);
            stmt.executeUpdate(initialIngredient);

            conn.commit();

        } catch (SQLException e) {
            try {
                conn.rollback();
                throw new RuntimeException("Erreur lors de la réinitialisation des données", e);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException _) {
            }
            dbConnection.closeDBConnection(conn);
        }
    }
}
