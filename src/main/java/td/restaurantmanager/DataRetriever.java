package td.restaurantmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        String inClause = newIngredients.stream()
                .map(x -> "(?, ?, ?, ?::ingredient_category)")
                .collect(Collectors.joining(", "));
        String sql = """
                insert into ingredient (id, name, price, category)
                values %s""".formatted(inClause);

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            int idx = next_id(conn, "ingredient");
            List<Ingredient> createdIngredients = new ArrayList<>();
            for (Ingredient ing : newIngredients) {
                if (ing.getId() == null) {
                    ps.setInt(index++, idx);
                    ing = new Ingredient(idx++, ing.getName(), ing.getPrice(), ing.getCategory());
                } else {
                    ps.setInt(index++, ing.getId());
                }
                ps.setString(index++, ing.getName());
                ps.setDouble(index++, ing.getPrice());
                ps.setObject(index++, ing.getCategory().name());
                createdIngredients.add(ing);
            }
            ps.executeUpdate();
            return createdIngredients;
        } catch (SQLException e) {
            duplicationError(e);
            throw new RuntimeException(e);
        }
    }

    private Integer next_id(Connection conn, String table) {
        String sql = "select coalesce(max(id), 0) + 1 from %s limit 1"
                .formatted("\"" + table + "\"");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void duplicationError(SQLException e) {
        String message = e.getMessage();
        String duplicateValue = extractDuplicateValue(message);

        if (message.contains("ingredient_name_key") || message.contains("(name)=")) {
            throw new RuntimeException(duplicateValue + " already exist");
        } else if (message.contains("ingredient_pkey") || message.contains("(id)=")) {
            throw new RuntimeException(duplicateValue + " already exist");
        }
    }

    private String extractDuplicateValue(String detail) {
        // Find pattern : (column)=(value)
        Pattern p = Pattern.compile("\\(([^)]+)\\)\\s*=\\s*\\(([^)]+)\\)");
        Matcher m = p.matcher(detail);

        if (m.find()) {
            return m.group().trim();
        }
        return detail;
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