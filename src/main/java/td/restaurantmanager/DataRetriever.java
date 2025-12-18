package td.restaurantmanager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private final DBConnection connection = new DBConnection();

    public List<Ingredient> findIngredientsById(Integer id) {
        String query =
                """
                        select i.id as ingredient_id, i.name as ingredient_name, i.price, i.category
                        from dish
                                 left join ingredient i on i.id_dish = dish.id
                        where id_dish = ?;""";
        try (
                PreparedStatement preparedStatement = connection.getDBConnection().prepareStatement(query);
        ) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet == null) {
                throw new IllegalArgumentException("Ingredient not found");
            }

            List<Ingredient> ingredient = new ArrayList<>();

            while (resultSet.next()) {
                Integer ingredient_id = (Integer) resultSet.getInt("ingredient_id");
                String name = resultSet.getString("ingredient_name");
                Double price = resultSet.getDouble("price");
                CategoryEnum category = CategoryEnum.valueOf(resultSet.getString("category"));

                ingredient.add(new Ingredient(
                        ingredient_id,
                        name,
                        price,
                        category
                ));
            }

            if (ingredient.isEmpty()) {
                throw new IllegalArgumentException("Ingredient not found");
            } else {
                return ingredient;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connection.closeDBConnection();
        }
    }

    public Dish findDishById(Integer id) {
        String query = "SELECT id, name, dish_type from dish where id = ?;";
        try (
                PreparedStatement preparedStatement = connection.getDBConnection().prepareStatement(query);
        ) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet == null) {
                throw new IllegalArgumentException("Dish not found");
            }

            Dish dish = null;

            while (resultSet.next()) {
                Integer dish_id = (Integer) resultSet.getInt("id");
                String name = resultSet.getString("name");
                DishTypeEnum dish_type = DishTypeEnum.valueOf(resultSet.getString("dish_type"));
                List<Ingredient> ingredients_list = findIngredientsById(id);

                dish = new Dish(
                        dish_id,
                        name,
                        dish_type,
                        ingredients_list
                );

                dish.setIngredients(ingredients_list);
            }

            if (dish == null) {
                throw new IllegalArgumentException("Dish not found");
            } else {
                return dish;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connection.closeDBConnection();
        }
    }

    public List<Ingredient> findIngredients(int page, int size) {
        return null;
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
