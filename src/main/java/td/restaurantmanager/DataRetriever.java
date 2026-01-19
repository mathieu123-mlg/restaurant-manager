package td.restaurantmanager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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

                dish.setIngredients(dish.getIngredients());
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

            List<Ingredient> ingredientsFromDB = new ArrayList<>();
            while (rs.next()) {
                Integer dishId = rs.getInt("id_dish");
                ingredientsFromDB.add(new Ingredient(
                        rs.getInt("id_ingredient"),
                        rs.getString("name"),
                        rs.getObject("price") == null ? null : rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        findDishById(dishId),
                        rs.getObject("quantity_required") == null ? null : rs.getDouble("quantity_required"),
                        UnitType.valueOf(rs.getString("unit"))
                ));
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
            String insertSql =
                    """
                            INSERT INTO ingredient (id, name, price, category, id_dish) 
                            VALUES (?, ?, ?, ?, ?)
                            RETURNING id;""";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient newIngredient : newIngredients) {
                    SQLBuildParams_createIngredients(newIngredient, ps);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        savedIngredients.add(
                                new Ingredient(
                                        rs.getInt("id"),
                                        newIngredient.getName(),
                                        newIngredient.getPrice(),
                                        newIngredient.getCategory(),
                                        newIngredient.getDish()
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
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE
                        SET name = EXCLUDED.name,
                            price = EXCLUDED.price,
                            dish_type = EXCLUDED.dish_type
                        RETURNING id""";

        try (Connection conn = dbConnection.getDBConnection()) {
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
            detachIngredients(conn, dishId, newIngredients);
            attachIngredients(conn, dishId, newIngredients);

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dish> findDishByIngredientsName(String ingredientsName) {
        if (ingredientsName == null || ingredientsName.trim().isEmpty()) {
            throw new IllegalArgumentException("ingredientsName is null or empty");
        }
        Connection conn = dbConnection.getDBConnection();
        String sql =
                """
                        select dish.id, dish.name, dish.dish_type, dish.price
                        from dish 
                        join ingredient on ingredient.id_dish = dish.id 
                        where ingredient.name ilike ? 
                        order by dish.id;""";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + ingredientsName + "%");
            ResultSet rs = ps.executeQuery();

            List<Dish> dishFromDatabase = new ArrayList<>();
            while (rs.next()) {
                Integer id = (Integer) rs.getInt("id");
                String name = rs.getString("name");
                DishTypeEnum dish_type = DishTypeEnum.valueOf(rs.getString("dish_type"));
                BigDecimal priceBigDecimal = rs.getBigDecimal("price");
                Double price = (priceBigDecimal != null) ? priceBigDecimal.doubleValue() : null;
                List<Ingredient> ingredients_list = findDishById(id).getIngredients();

                Dish dish = new Dish(
                        id,
                        name,
                        dish_type,
                        price,
                        ingredients_list
                );

                dish.setIngredients(dish.getIngredients());
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
            throw new IllegalArgumentException("page or size null");
        }
        StringBuilder sql = new StringBuilder(
                """
                        select 
                            ingredient.id, 
                            ingredient.name,
                            ingredient.price,
                            ingredient.category,
                            ingredient.id_dish
                        from ingredient
                        join dish on dish.id = ingredient.id_dish
                        where 1 = 1"""
        );
        Connection conn = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = SQLBuildParams_findIngredientsByCriteria(
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
            List<Ingredient> ingredientsFromDB = new ArrayList<>();
            while (rs.next()) {
                Integer id = (Integer) rs.getInt("id");
                String name = rs.getString("name");
                BigDecimal priceBigDecimal = rs.getBigDecimal("price");
                Double price = (priceBigDecimal != null) ? priceBigDecimal.doubleValue() : null;
                CategoryEnum category_type = CategoryEnum.valueOf(rs.getString("category"));
                Integer id_dish = rs.getInt("id_dish");

                Ingredient ingredient = new Ingredient(id, name, price, category_type);
                ingredient.setDish(findDishById(id_dish));
                ingredientsFromDB.add(ingredient);
            }
            return ingredientsFromDB;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    private void SQLBuildParams_createIngredients(Ingredient newIngredient, PreparedStatement ps) {
        try {
            ps.setInt(1, newIngredient.getId());
            ps.setString(2, newIngredient.getName());
            ps.setDouble(3, newIngredient.getPrice());
            ps.setString(4, newIngredient.getCategory().toString());

            if (newIngredient.getDishName() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, newIngredient.getDish().getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PreparedStatement SQLBuildParams_findIngredientsByCriteria(StringBuilder sql, String ingredientName, CategoryEnum category, String dishName, Connection conn, int page, int size) {
        try {
            if (ingredientName != null) {
                sql.append(" and ingredient.name ilike ? ");
            }
            if (category != null) {
                sql.append(" and ingredient.category ilike ? ");
            }
            if (dishName != null) {
                sql.append(" and dish.name ilike ? ");
            }
            sql.append(" order by ingredient.id");
            sql.append(" limit ? offset ? ;");

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int i = 0;
            if (ingredientName != null) {
                ps.setString(i + 1, "%" + ingredientName + "%");
                i += 1;
            }
            if (category != null) {
                ps.setString(i + 1, "%" + category.toString() + "%");
                i += 1;
            }
            if (dishName != null) {
                ps.setString(i + 1, "%" + dishName + "%");
                i += 1;
            }
            ps.setInt(i + 1, size);
            i += 1;
            ps.setInt(i + 1, (page - 1) * size);

            return ps;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    private void detachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
            throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ingredient SET id_dish = NULL WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }
            return;
        }

        String baseSql = """
                    UPDATE ingredient
                    SET id_dish = NULL
                    WHERE id_dish = ? AND id NOT IN (%s)
                """;

        String inClause = ingredients.stream()
                .map(i -> "?")
                .collect(Collectors.joining(","));

        String sql = String.format(baseSql, inClause);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            int index = 2;
            for (Ingredient ingredient : ingredients) {
                ps.setInt(index++, ingredient.getId());
            }
            ps.executeUpdate();
        }
    }

    private void attachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
            throws SQLException {

        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        String attachSql = """
                    UPDATE ingredient
                    SET id_dish = ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (Ingredient ingredient : ingredients) {
                ps.setInt(1, dishId);
                ps.setInt(2, ingredient.getId());
                ps.addBatch(); // Can be substitute ps.executeUpdate() but bad performance
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
