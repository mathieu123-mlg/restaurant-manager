package td.restaurantmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            return getCreationIngredients(newIngredients, conn, ps);
        } catch (SQLException e) {
            duplicationError(e);
            throw new RuntimeException(e);
        }
    }

    private List<Ingredient> getCreationIngredients(List<Ingredient> newIngredients, Connection conn, PreparedStatement ps) throws SQLException {
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
    }

    public List<Ingredient> createIngredients(Connection conn, List<Ingredient> newIngredients) {
        String inClause = newIngredients.stream()
                .map(x -> "(?, ?, ?, ?::ingredient_category)")
                .collect(Collectors.joining(", "));
        String sql = """
                insert into ingredient (id, name, price, category)
                values %s on conflict (id) do nothing""".formatted(inClause);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return getCreationIngredients(newIngredients, conn, ps);
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
        } else if (message.contains("unique_dish_ingredient") || message.contains("(id_dish, id_ingredient)=")) {
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
        String sql = """
                insert into dish (id, name, dish_type, price) values (?, ?, ?::dish_type, ?)
                on conflict (id) do update set name = excluded.name, dish_type = excluded.dish_type, price = excluded.price
                returning id""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            Integer idDish = dishToSave.getId() == null ? next_id(conn, "dish") : dishToSave.getId();
            ps.setInt(1, idDish);
            ps.setString(2, dishToSave.getName());
            ps.setObject(3, dishToSave.getDishType().name());
            ps.setDouble(4, dishToSave.getPrice());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Dish dish = new Dish(idDish, dishToSave.getName(), dishToSave.getDishType(), dishToSave.getPrice(),
                        dishToSave.getDishIngredients());

                List<DishIngredient> dishIngredients = dishToSave.getDishIngredients();

                detachDishIngredient(conn, idDish);
                if (!dishIngredients.isEmpty()) {
                    List<Ingredient> ingredients = createIngredients(conn, dishIngredients.stream()
                            .map(DishIngredient::getIngredient).toList());
                    List<DishIngredient> tempDishIngredients = new ArrayList<>();
                    for (int i = 0; i < ingredients.size(); i++) {
                        tempDishIngredients.add(new DishIngredient(ingredients.get(i), dishIngredients.get(i).getQuantityRequired(), dishIngredients.get(i).getUnit()));
                    }
                    dishIngredients = tempDishIngredients;
                    attachDishIngredient(conn, idDish, dishIngredients);
                    var stockMovements = fetchStockMovementUsingExistingIds(dishIngredients.stream()
                            .map(x -> x.getIngredient().getId()).collect(Collectors.toSet()));
                    dish.setDishIngredients(dishIngredients, stockMovements);
                }
                conn.commit();
                return dish;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachDishIngredient(Connection conn, Integer idDish, List<DishIngredient> dishIngredients) {
        String inClause = dishIngredients.stream().map(_ -> "(?, ?, ?, ?, ?::unit_type)")
                .collect(Collectors.joining(", "));
        String sql = """
                insert into dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
                values %s""".formatted(inClause);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            int idx = next_id(conn, "dish_ingredient");
            for (DishIngredient dishIngredient : dishIngredients) {
                ps.setInt(index++, idx++);
                ps.setInt(index++, idDish);
                ps.setInt(index++, dishIngredient.getIngredient().getId());
                ps.setDouble(index++, dishIngredient.getQuantityRequired());
                ps.setObject(index++, dishIngredient.getUnit().name());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            duplicationError(e);
            throw new RuntimeException(e);
        }
    }

    private void detachDishIngredient(Connection conn, Integer idDish) {
        String sql = """
                delete from dish_ingredient where id_dish = ?;""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDish);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        DataRetriever data = new DataRetriever(); /*
        for (int i = 1; i < 6; i++) {
            System.out.println(data.findDishById(i));
        }

        System.out.println();
        System.out.println(data.findIngredients(1, 5));
        System.out.println(data.findIngredients(2, 2));
        System.out.println(data.findIngredients(2, 7));
        List<Ingredient> ingredientList = List.of(
                new Ingredient(6, "Citrouille", 4_200.0, CategoryEnum.VEGETABLE),
                new Ingredient(7, "Porc", 200.0, CategoryEnum.ANIMAL),
                new Ingredient(8, "Poivre", 1_000.0, CategoryEnum.VEGETABLE)
        );
        System.out.println(data.createIngredients(ingredientList));
*//*
        Dish dishToSave1 = new Dish(
                null,
                "Vary @ anana",
                DishTypeEnum.MAIN,
                1_000.0,
                List.of(new DishIngredient(new Ingredient(null, "Anana", 500.0, CategoryEnum.VEGETABLE), 4.0, UnitType.PCS),
                        new DishIngredient(new Ingredient(null, "Vary", 500.0, CategoryEnum.VEGETABLE), 0.4, UnitType.KG))
        );
        Ingredient laitue = data.findIngredientById(1);
        Ingredient poulet = data.findIngredientById(3);
        Dish dishToSave2 = new Dish(
                6,
                "Vary",
                DishTypeEnum.STARTER,
                3_500.0,
                List.of(new DishIngredient(laitue, 0.15, UnitType.KG),
                        new DishIngredient(poulet, 5.0, UnitType.KG))
        );
        Dish dishToSave3 = new Dish(
                7,
                "Riz",
                DishTypeEnum.MAIN,
                3_500.0,
                null
        );
        System.out.println("dishToSave ---- 111111111");
        System.out.println(data.saveDish(dishToSave1));
        System.out.println("dishToSave ---- 222222222");
        System.out.println(data.saveDish(dishToSave2));
        System.out.println("dishToSave ---- 333333333");
        System.out.println(data.saveDish(dishToSave3));
*/
        System.out.println(data.findDishByIngredientName("o"));
    }

    public List<Dish> findDishByIngredientName(String ingredientName) {
        String sql = """
                select d.id from dish d
                join dish_ingredient d_i on d.id = d_i.id_dish
                join ingredient i on i.id = d_i.id_ingredient
                where i.name ilike ? order by d.id""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%"+ingredientName+"%");
            ResultSet rs = ps.executeQuery();
            List<Dish> dishes = new ArrayList<>();
            while (rs.next()) {
                Integer idDish = rs.getInt("dish_id");
                dishes.add(findDishById(idDish));
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public Ingredient findIngredientById(int idIngredient) {
        String sql = """
                select id, name, price, category from ingredient where id = ?""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIngredient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Ingredient(
                        idIngredient,
                        rs.getString("name"),
                        rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        fetchStockMovementUsingExistingIds(Collections.singleton(idIngredient)).getOrDefault(idIngredient, new ArrayList<>())
                );
            }
            throw new RuntimeException("Ingredient(" + idIngredient + ") not found");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}