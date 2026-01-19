package td.restaurantmanager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is required");
        } else if (id <= 0) {
            throw new IllegalArgumentException("Id must be greater than 0");
        }

        String sql =
                """
                        SELECT dish.id, dish.name, dish.dish_type, dish.price 
                        from dish
                        where dish.id = ?;""";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = databaseConnection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                List<Ingredient> ingredients_list = findIngredientsOfDishById(id);

                Dish dishFromDatabase = new Dish(
                        rs.getInt("id"),
                        rs.getString("name"),
                        DishTypeEnum.valueOf(rs.getString("dish_type")),
                        rs.getObject("price") == null
                                ? null
                                : rs.getDouble("price"),
                        ingredients_list
                );
                dishFromDatabase.setIngredients(ingredients_list);
                return dishFromDatabase;
            }
            throw new RuntimeException("Dish id=" + id + " not found");

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
        }
    }

    public Dish findDishIngredientByDishId(int dishId) {
        String sql =
                """
                        SELECT 
                            d.id, d.name AS dish_name, d.dish_type, d.price AS selling_price,
                            i.id AS id_ingredient, i.name AS ingredient_name, i.price, i.category,
                            di.quantity_required, di.unit
                        FROM dish_ingredient di
                        JOIN dish d ON di.id_dish = d.id
                        JOIN ingredient i ON di.id_ingredient = i.id
                        WHERE d.id = ?""";

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {

                Dish dish = null;
                List<Ingredient> ingredients = new ArrayList<>();

                while (rs.next()) {
                    if (dish == null) {
                        dish = new Dish(
                                rs.getInt("id"),
                                rs.getString("dish_name"),
                                DishTypeEnum.valueOf(rs.getString("dish_type")),
                                rs.getBigDecimal("selling_price").doubleValue(),
                                ingredients
                        );
                    }

                    ingredients.add(new Ingredient(
                            rs.getInt("id_ingredient"),
                            rs.getString("ingredient_name"),
                            rs.getBigDecimal("price").doubleValue(),
                            CategoryEnum.valueOf(rs.getString("category")),
                            rs.getBigDecimal("quantity_required").doubleValue(),
                            UnitType.valueOf(rs.getString("unit"))
                    ));
                }

                if (dish == null) {
                    throw new RuntimeException("Plat non trouvé : id=" + dishId);
                }

                dish.setIngredients(ingredients);
                return dish;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du plat #" + dishId, e);
        }
    }

    public List<Ingredient> findIngredients(int page, int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Page and size must be greater than 0");
        }

        String sql =
                """
                        SELECT id, name, price, category, id_dish
                        FROM ingredient
                        order by id LIMIT ? OFFSET ? ;""";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);
            ResultSet resultSet = preparedStatement.executeQuery();

            List<Ingredient> ingredientsFromDB = new ArrayList<>();
            while (resultSet.next()) {
                Integer id = (Integer) resultSet.getInt("id");
                String name = resultSet.getString("name");
                BigDecimal priceBigDecimal = resultSet.getBigDecimal("price");
                Double price = (priceBigDecimal != null) ? priceBigDecimal.doubleValue() : null;
                CategoryEnum category = CategoryEnum.valueOf(resultSet.getString("category"));
                Integer id_dish = resultSet.getInt("id_dish");

                Dish dishFromDB = findDishById(id_dish);

                ingredientsFromDB.add(new Ingredient(
                        id,
                        name,
                        price,
                        category,
                        dishFromDB
                ));
            }

            return ingredientsFromDB;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }

        List<Ingredient> savedIngredients = new ArrayList<>();
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            databaseConnection.setAutoCommit(false);
            String insertSql =
                    """
                            INSERT INTO ingredient (id, name, price, category, id_dish) 
                            VALUES (?, ?, ?, ?, ?)
                            RETURNING id;""";
            try (PreparedStatement ps = databaseConnection.prepareStatement(insertSql)) {
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

                databaseConnection.commit();
                return savedIngredients;
            } catch (SQLException e) {
                databaseConnection.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
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
        Connection databaseConnection = dbConnection.getDBConnection();
        String sql =
                """
                        select dish.id, dish.name, dish.dish_type, dish.price
                        from dish 
                        join ingredient on ingredient.id_dish = dish.id 
                        where ingredient.name ilike ? 
                        order by dish.id;""";
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            preparedStatement.setString(1, "%" + ingredientsName + "%");
            ResultSet resultSet = preparedStatement.executeQuery();

            List<Dish> dishFromDatabase = new ArrayList<>();
            while (resultSet.next()) {
                Integer id = (Integer) resultSet.getInt("id");
                String name = resultSet.getString("name");
                DishTypeEnum dish_type = DishTypeEnum.valueOf(resultSet.getString("dish_type"));
                BigDecimal priceBigDecimal = resultSet.getBigDecimal("price");
                Double price = (priceBigDecimal != null) ? priceBigDecimal.doubleValue() : null;
                List<Ingredient> ingredients_list = findIngredientsOfDishById(id);

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
            dbConnection.closeDBConnection();
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
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement preparedStatement = SQLBuildParams_findIngredientsByCriteria(
                    sql,
                    ingredientName,
                    category,
                    dishName,
                    databaseConnection,
                    page,
                    size
            );

            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getResultSet();
            List<Ingredient> ingredientsFromDB = new ArrayList<>();
            while (resultSet.next()) {
                Integer id = (Integer) resultSet.getInt("id");
                String name = resultSet.getString("name");
                BigDecimal priceBigDecimal = resultSet.getBigDecimal("price");
                Double price = (priceBigDecimal != null) ? priceBigDecimal.doubleValue() : null;
                CategoryEnum category_type = CategoryEnum.valueOf(resultSet.getString("category"));
                Integer id_dish = resultSet.getInt("id_dish");

                Ingredient ingredient = new Ingredient(id, name, price, category_type);
                ingredient.setDish(findDishById(id_dish));
                ingredientsFromDB.add(ingredient);
            }
            return ingredientsFromDB;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
        }
    }

    private List<Ingredient> findIngredientsOfDishById(Integer id) {
        String sql =
                """
                        select id_ingredient as id, quantity_required, unit, i.name, i.price, i.category
                        from dish_ingredient
                        left join ingredient i on i.id = dish_ingredient.id_ingredient
                        where dish_ingredient.id_dish = ? ;""";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement ps = databaseConnection.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            List<Ingredient> ingredientFromDB = new ArrayList<>();

            while (rs.next()) {
                ingredientFromDB.add(
                        new Ingredient(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getObject("price") == null
                                        ? null
                                        : rs.getDouble("price"),
                                CategoryEnum.valueOf(rs.getString("category")),
                                rs.getObject("quantity_required") == null
                                        ? null
                                        : rs.getDouble("quantity_required"),
                                UnitType.valueOf(rs.getString("unit"))
                        )
                );
            }

            return ingredientFromDB;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
        }
    }

    private void SQLBuildParams_createIngredients(Ingredient newIngredient, PreparedStatement preparedStatement) {
        try {
            preparedStatement.setInt(1, newIngredient.getId());
            preparedStatement.setString(2, newIngredient.getName());
            preparedStatement.setDouble(3, newIngredient.getPrice());
            preparedStatement.setString(4, newIngredient.getCategory().toString());

            if (newIngredient.getDishName() == null) {
                preparedStatement.setNull(5, Types.INTEGER);
            } else {
                preparedStatement.setInt(5, newIngredient.getDish().getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PreparedStatement SQLBuildParams_findIngredientsByCriteria(StringBuilder sql, String ingredientName, CategoryEnum category, String dishName, Connection databaseConnection, int page, int size) {
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

            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql.toString());
            int i = 0;
            if (ingredientName != null) {
                preparedStatement.setString(i + 1, "%" + ingredientName + "%");
                i += 1;
            }
            if (category != null) {
                preparedStatement.setString(i + 1, "%" + category.toString() + "%");
                i += 1;
            }
            if (dishName != null) {
                preparedStatement.setString(i + 1, "%" + dishName + "%");
                i += 1;
            }
            preparedStatement.setInt(i + 1, size);
            i += 1;
            preparedStatement.setInt(i + 1, (page - 1) * size);

            return preparedStatement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer next_id() {
        String sql = "select id from dish order by id desc limit 1";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            Statement stm = databaseConnection.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1) + 1;
            } else {
                return 1;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
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
        Connection databaseConnection = dbConnection.getDBConnection();

        try {
            databaseConnection.setAutoCommit(false);
            Statement stmt = databaseConnection.createStatement();
            stmt.executeUpdate(deleteIngredient);
            stmt.executeUpdate(deleteDish);

            stmt.executeUpdate(resetSequenceDish);
            stmt.executeUpdate(resetSequenceIngredient);

            stmt.executeUpdate(initialDish);
            stmt.executeUpdate(initialIngredient);

            databaseConnection.commit();

        } catch (SQLException e) {
            try {
                databaseConnection.rollback();
                throw new RuntimeException("Erreur lors de la réinitialisation des données", e);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            try {
                databaseConnection.setAutoCommit(true);
            } catch (SQLException _) {
            }
            dbConnection.closeDBConnection();
        }
    }
}
