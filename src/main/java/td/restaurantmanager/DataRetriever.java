package td.restaurantmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public List<Ingredient> findIngredientsOfDishById(Integer id) {
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

            if (ingredientFromDB.isEmpty()) {
                throw new RuntimeException("Ingredient is empty on this dish");
            } else {
                return ingredientFromDB;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection();
        }
    }

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
                        SELECT 
                            i.id, i.name, i.price, i.category, i.id_dish
                        FROM ingredient i
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

    public List<Ingredient> createIncgredients(List<Ingredient> newIngredients) {
        return null;
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
}
