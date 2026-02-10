package td.restaurantmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer idDish) {
        String sql = """
                select d.id as dish_id, d.name as dish_name, d.dish_type as dish_type, d.price as dish_price,
                d_i.id as di_id, d_i.id_ingredient, d_i.quantity_required, d_i.unit,
                i.id as ingredient_id, i.name as ingredient_name, i.price as ingredient_price, i.category
                from dish d
                left join dish_ingredient d_i on d_i.id_dish = d.id
                left join ingredient i on d_i.id_ingredient = i.id
                where d.id = ?""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDish);
            ResultSet rs = ps.executeQuery();
            Dish dish = null;
            List<DishIngredient> dishIngredients = new ArrayList<>();
            while (rs.next()) {
                if (dish == null) {
                    dish = new Dish(
                            rs.getInt("dish_id"),
                            rs.getString("dish_name"),
                            DishTypeEnum.valueOf(rs.getString("dish_type")),
                            rs.getDouble("dish_price"),
                            new ArrayList<>()
                    );
                }
                if (rs.getInt("di_id") > 0) {
                    Integer ing_id = rs.getInt("ingredient_id");
                    Ingredient ingredient = new Ingredient(
                            ing_id,
                            rs.getString("ingredient_name"),
                            rs.getDouble("ingredient_price"),
                            CategoryEnum.valueOf(rs.getString("category"))
                    );
                    DishIngredient dishIngredient = new DishIngredient(
                            ingredient,
                            rs.getDouble("quantity_required"),
                            UnitType.valueOf(rs.getString("unit"))
                    );
                    dishIngredients.add(dishIngredient);
                }
            }
            if (dish == null) {
                throw new RuntimeException("Dish(id=" + idDish + ") not found");
            }
            Set<Integer> ingredientIds = dishIngredients.stream()
                    .map(d_i -> d_i.getIngredient().getId())
                    .collect(Collectors.toSet());
            var stockMovementList = fetchStockMovementUsingExistingIds(ingredientIds);
            dish.setDishIngredients(dishIngredients, stockMovementList);
            return dish;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, List<StockMovement>> fetchStockMovementUsingExistingIds(Set<Integer> ingredientIds) {
        if (ingredientIds.isEmpty()) {
            return new HashMap<>();
        }
        String sql = """
                select id, id_ingredient, quantity, type, unit, creation_datetime
                from stock_movement where id_ingredient in (%s)""";
        String inClause = ingredientIds.stream()
                .map(_ -> "?")
                .collect(Collectors.joining(","));
        sql = String.format(sql, inClause);

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : ingredientIds) {
                ps.setInt(index++, id);
            }
            ResultSet rs = ps.executeQuery();
            Map<Integer, List<StockMovement>> stockMovements = new HashMap<>();
            for (Integer id : ingredientIds) {
                stockMovements.put(id, new ArrayList<>());
            }
            while (rs.next()) {
                StockMovement stock = new StockMovement(
                        rs.getInt("id"),
                        new StockValue(
                                rs.getDouble("quantity"),
                                UnitType.valueOf(rs.getString("unit"))
                        ),
                        MovementTypeEnum.valueOf(rs.getString("type")),
                        rs.getTimestamp("creation_datetime").toInstant()
                );
                Integer id_ingredient = rs.getInt("id_ingredient");
                List<StockMovement> stocks = stockMovements.get(id_ingredient);
                stocks.add(stock);
                stockMovements.put(id_ingredient, stocks);
            }
            return stockMovements;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ingredient> findIngredients(int page, int size) {
        String sql = """
                select id, id, name, price, category
                from ingredient limit ? offset ?""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            ResultSet rs = ps.executeQuery();
            List<Ingredient> ingredients = new ArrayList<>();
            while (rs.next()) {
                Ingredient ingredient = new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category"))
                );
                ingredients.add(ingredient);
            }
            Set<Integer> ingredientIds = ingredients.stream()
                    .map(Ingredient::getId)
                    .collect(Collectors.toSet());
            var stockMovement = fetchStockMovementUsingExistingIds(ingredientIds);
            for (Ingredient ingredient : ingredients) {
                ingredient.setStockMovementList(stockMovement.get(ingredient.getId()));
            }
            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        DataRetriever data = new DataRetriever();
/*
        for (int i = 1; i < 6; i++) {
            System.out.println(data.findDishById(i));
        }

        System.out.println();
        System.out.println(data.findIngredients(1, 5));
        System.out.println(data.findIngredients(2, 2));
        System.out.println(data.findIngredients(2, 7));
*/

        System.out.println(data.createIngredients(data.findIngredients(1, 5)));
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        String inClause = newIngredients.stream()
                .map(x -> "(?, ?, ?, ?)")
                .collect(Collectors.joining(", "));
        String sql = """
                insert into ingredient (id, name, price, category)
                values %s""".formatted(inClause);

        try (Connection conn = dbConnection.getDBConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int index = 1;
            for (Ingredient ingredient : newIngredients) {
                ps.setInt(index++, ingredient.getId());
                ps.setString(index++, ingredient.getName());
                ps.setDouble(index++, ingredient.getPrice());
                ps.setObject(index++, ingredient.getCategory().name());
            }
            return newIngredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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