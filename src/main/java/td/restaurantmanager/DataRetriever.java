package td.restaurantmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is required");
        };

        String sqlDish = """
                SELECT d.id AS dish_id, d.name AS dish_name, d.dish_type, d.price AS dish_price,
                       di.id AS di_id, di.quantity_required, di.unit AS di_unit,
                       i.id AS ingredient_id, i.name AS ingredient_name, i.price AS ingredient_price, i.category AS ingredient_category
                FROM dish d
                LEFT JOIN dish_ingredient di ON d.id = di.id_dish
                LEFT JOIN ingredient i ON i.id = di.id_ingredient
                WHERE d.id = ?
                ORDER BY i.id""";

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sqlDish)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                Dish dish = null;
                List<DishIngredient> dishIngredients = new ArrayList<>();
                Map<Integer, Ingredient> tempIngredients = new HashMap<>();
                Set<Integer> ingredientIds = new HashSet<>();

                while (rs.next()) {
                    if (dish == null) {
                        dish = buildDish(rs);
                    }
                    Integer ingId = getInteger(rs, "ingredient_id");
                    if (ingId != null && !tempIngredients.containsKey(ingId)) {
                        tempIngredients.put(ingId, buildTempIngredient(rs));
                        ingredientIds.add(ingId);
                    }
                    if (ingId != null) {
                        dishIngredients.add(buildDishIngredient(rs, dish, tempIngredients.get(ingId)));
                    }
                }
                if (dish == null) {
                    throw new RuntimeException("Dish(id="+id+") not found");
                };

                Map<Integer, List<StockMovement>> movementsByIng = fetchStockMovements(conn, ingredientIds);
                Map<Integer, Ingredient> finalIngredients = new HashMap<>();
                for (Map.Entry<Integer, Ingredient> entry : tempIngredients.entrySet()) {
                    Integer ingId = entry.getKey();
                    List<StockMovement> movements = movementsByIng.getOrDefault(ingId, new ArrayList<>());
                    finalIngredients.put(ingId, new Ingredient(ingId, entry.getValue().getName(),
                            entry.getValue().getPrice(), entry.getValue().getCategory(), movements));
                }

                List<DishIngredient> updatedDishIngredients = new ArrayList<>();
                for (DishIngredient di : dishIngredients) {
                    Ingredient updatedIng = finalIngredients.get(di.getIngredient().getId());
                    updatedDishIngredients.add(new DishIngredient(di.getId(), di.getDish(), updatedIng,
                            di.getQuantityRequired(), di.getUnit()));
                }

                return new Dish(dish.getId(), dish.getName(), dish.getDishType(), dish.getPrice(), updatedDishIngredients);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération plat id=" + id, e);
        }
    }

    private Map<Integer, List<StockMovement>> fetchStockMovements(Connection conn, Set<Integer> ingredientIds) throws SQLException {
        if (ingredientIds.isEmpty()) return Collections.emptyMap();

        String sqlMov = """
                        SELECT id, id_ingredient, quantity, type, unit, creation_datetime
                        FROM stock_movement WHERE id_ingredient IN (%s)
                        ORDER BY id_ingredient, creation_datetime""";
        sqlMov = String.format(sqlMov, placeholders(ingredientIds.size()));

        try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
            int idx = 1;
            for (Integer ingId : ingredientIds) {
                ps.setInt(idx++, ingId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, List<StockMovement>> map = new HashMap<>();
                while (rs.next()) {
                    Integer ingId = rs.getInt("id_ingredient");
                    map.computeIfAbsent(ingId, k -> new ArrayList<>()).add(buildStockMovement(rs));
                }
                return map;
            }
        }
    }

    private String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private Dish buildDish(ResultSet rs) throws SQLException {
        return new Dish(rs.getInt("dish_id"), rs.getString("dish_name"),
                enumValue(DishTypeEnum.class, rs.getString("dish_type")),
                getDouble(rs, "dish_price"), new ArrayList<>());
    }

    private Ingredient buildTempIngredient(ResultSet rs) throws SQLException {
        return new Ingredient(rs.getInt("ingredient_id"), rs.getString("ingredient_name"),
                getDouble(rs, "ingredient_price"),
                enumValue(CategoryEnum.class, rs.getString("ingredient_category")));
    }

    private DishIngredient buildDishIngredient(ResultSet rs, Dish dish, Ingredient ing) throws SQLException {
        return new DishIngredient(rs.getInt("di_id"), dish, ing,
                rs.getDouble("quantity_required"),
                enumValue(UnitType.class, rs.getString("di_unit")));
    }

    private StockMovement buildStockMovement(ResultSet rs) throws SQLException {
        return new StockMovement(rs.getInt("id"),
                new StockValue(rs.getDouble("quantity"), enumValue(UnitType.class, rs.getString("unit"))),
                enumValue(MovementTypeEnum.class, rs.getString("type")),
                rs.getTimestamp("creation_datetime").toInstant());
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumClass, String value) {
        return value != null ? Enum.valueOf(enumClass, value) : null;
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int val = rs.getInt(column);
        return rs.wasNull() ? null : val;
    }

    private Double getDouble(ResultSet rs, String col) throws SQLException {
        double val = rs.getDouble(col);
        return rs.wasNull() ? null : val;
    }
}