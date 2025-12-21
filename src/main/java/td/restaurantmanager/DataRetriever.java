package td.restaurantmanager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is required");
        } else if (id <= 0) {
            throw new IllegalArgumentException("Id must be greater than 0");
        }

        String sql = "SELECT id, name, dish_type from dish where id = ?;";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                throw new RuntimeException("Dish with id " + id + " not found");
            }

            Integer id_dish = (Integer) resultSet.getInt("id");
            String name = resultSet.getString("name");
            DishTypeEnum dish_type = DishTypeEnum.valueOf(resultSet.getString("dish_type"));
            List<Ingredient> ingredients_list = findIngredientsOfDishById(id);

            Dish dishFromDatabase = new Dish(
                    id_dish,
                    name,
                    dish_type,
                    ingredients_list
            );

            dishFromDatabase.setIngredients(ingredients_list);
            return dishFromDatabase;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
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
                        LIMIT ? OFFSET ? ;""";
        Connection databaseConnection = dbConnection.getDBConnection();
        List<Ingredient> ingredientsFromDB = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Integer id = (Integer) resultSet.getInt("id");
                String name = resultSet.getString("name");
                Double price = resultSet.getDouble("price");
                CategoryEnum category = CategoryEnum.valueOf(resultSet.getString("category"));
                Integer id_dish = (Integer) resultSet.getInt("id_dish");

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
            throw new IllegalArgumentException("New ingredient is required");
        }

        String sql =
                """
                        INSERT INTO ingredient (id, name, price, category, id_dish) 
                        VALUES (?, ?, ?, ?, ?);""";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            databaseConnection.setAutoCommit(false);
            List<Ingredient> allIngredients = getListIngredients();

            for (Ingredient newIngredient : newIngredients) {

                if (allIngredients.contains(newIngredient)) {
                    throw new RuntimeException("Ingredient named: '" + newIngredient.getName() + "' already exists with ID: " + newIngredient.getId());
                }
                SQLBuildParams(newIngredient, preparedStatement);
                preparedStatement.executeUpdate();
            }

            databaseConnection.commit();
            databaseConnection.setAutoCommit(true);
            return newIngredients;

        } catch (Exception e) {
            try {
                databaseConnection.rollback();
                throw new RuntimeException(e);
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } finally {
            dbConnection.closeDBConnection();
        }
    }

    public Dish SaveDish(Dish dishToSave) {
        return null;
    }

    public List<Dish> findDishByIngredientsName(String ingredientsName) {
        return null;
    }

    public List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum category, String dishName, int page, int size) {
        return null;
    }

    private List<Ingredient> findIngredientsOfDishById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is required");
        } else if (id <= 0) {
            throw new IllegalArgumentException("Id must be greater than 0");
        }

        String sql =
                """
                        select i.id as ingredient_id, i.name as ingredient_name, i.price, i.category
                        from dish
                                 left join ingredient i on i.id_dish = dish.id
                        where id_dish = ? ;""";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            List<Ingredient> ingredientFromDB = new ArrayList<>();

            while (resultSet.next()) {
                Integer ingredient_id = (Integer) resultSet.getInt("ingredient_id");
                String name = resultSet.getString("ingredient_name");
                Double price = resultSet.getDouble("price");
                CategoryEnum category = CategoryEnum.valueOf(resultSet.getString("category"));

                ingredientFromDB.add(
                        new Ingredient(
                                ingredient_id,
                                name,
                                price,
                                category
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

    private void SQLBuildParams(Ingredient newIngredient, PreparedStatement preparedStatement) {
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

    private List<Ingredient> getListIngredients() {
        String sql = "SELECT id, name, price, category, id_dish FROM ingredient;";
        Connection databaseConnection = dbConnection.getDBConnection();
        try {
            PreparedStatement preparedStatement = databaseConnection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<Ingredient> ingredientFromDB = new ArrayList<>();

            while (resultSet.next()) {
                Integer id = (Integer) resultSet.getInt("id");
                String name = resultSet.getString("name");
                Double price = resultSet.getDouble("price");
                CategoryEnum category = CategoryEnum.valueOf(resultSet.getString("category"));

                Ingredient ingredient = new Ingredient(id, name, price, category);
                ingredientFromDB.add(ingredient);
            }

            return ingredientFromDB;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
