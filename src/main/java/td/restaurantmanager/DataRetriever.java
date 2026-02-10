package td.restaurantmanager;

import java.util.List;

public class DataRetriever {
    public Dish findDishById(Integer idDish) {
        throw new RuntimeException("Not Implemented");
    }

    public List<Ingredient> findIngredients(int page, int size) {
        throw new RuntimeException("Not Implemented");
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        throw new RuntimeException("Not Implemented");
    }

    public Dish saveDish(Dish dishToSave) {
        throw new RuntimeException("Not Implemented");
    }

    public List<Dish> findDishByIngredientName(String ingredientName) {
        throw new RuntimeException("Not Implemented");
    }

    public List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum categoryName, String dishName, int page, int size) {
        throw new RuntimeException("Not Implemented");
    }

    public Ingredient saveIngredient(Ingredient newIngredient) {
        throw new RuntimeException("Not Implemented");
    }

    public Order saveOrder(Order orderToSave) {
        throw new RuntimeException("Not Implemented");
    }

    public Order findOrderByReference(String reference) {
        throw new RuntimeException("Not Implemented");
    }
}