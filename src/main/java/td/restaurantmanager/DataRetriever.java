package td.restaurantmanager;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer idDish) {
        String sql = """
                select d.id as dish_id, d.name as dish_name, d.dish_type, d.price as dish_price,
                d_i.id as di_id, d_i.quantity_required, d_i.unit,
                i.id as ingredient_id, i.name as ingredient_name, i.price as ingredient_price, i.category
                from dish d
                left join dish_ingredient d_i on d_i.id_dish = d.id
                left join ingredient i on d_i.id_ingredient = i.id
                where d.id = ?""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return getSearchDishById(idDish, ps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish findDishById(Connection conn, Integer idDish) {
        String sql = """
                select d.id as dish_id, d.name as dish_name, d.dish_type as dish_type, d.price as dish_price,
                d_i.id as di_id, d_i.id_ingredient, d_i.quantity_required, d_i.unit,
                i.id as ingredient_id, i.name as ingredient_name, i.price as ingredient_price, i.category
                from dish d
                left join dish_ingredient d_i on d_i.id_dish = d.id
                left join ingredient i on d_i.id_ingredient = i.id
                where d.id = ?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return getSearchDishById(idDish, ps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Dish getSearchDishById(Integer idDish, PreparedStatement ps) throws SQLException {
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
        } else if (message.contains("unique_dish_order") || message.contains("(id_order, id_dish)=")) {
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
            return getDishSaving(conn, dishToSave, ps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish saveDish(Connection conn, Dish dishToSave) {
        String sql = """
                insert into dish (id, name, dish_type, price) values (?, ?, ?::dish_type, ?)
                on conflict (id) do update set name = excluded.name, dish_type = excluded.dish_type, price = excluded.price
                returning id""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return getDishSaving(conn, dishToSave, ps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Dish getDishSaving(Connection conn, Dish dishToSave, PreparedStatement ps) throws SQLException {
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

    public List<Dish> findDishByIngredientName(String ingredientName) {
        String sql = """
                select d.id from dish d
                join dish_ingredient d_i on d.id = d_i.id_dish
                join ingredient i on i.id = d_i.id_ingredient
                where i.name ilike ? order by d.id""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + ingredientName + "%");
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
        String sql = """
                select i.id, i.name, i.price, i.category
                from ingredient i
                join dish_ingredient on i.id = dish_ingredient.id_ingredient
                join dish on dish.id = dish_ingredient.id_dish
                where 1=1 %s
                order by i.id limit ? offset ?;"""
                .formatted(buildSQLQuery(ingredientName, categoryName, dishName));

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            buildCriteriaParams(ingredientName, categoryName, dishName, page, size, ps);
            ResultSet rs = ps.executeQuery();
            List<Ingredient> ingredients = new ArrayList<>();
            Set<Integer> ingredientIds = new HashSet<>();
            while (rs.next()) {
                Integer idIngredient = rs.getInt("id");
                Ingredient ingredient = new Ingredient(
                        idIngredient,
                        rs.getString("name"),
                        rs.getDouble("price"),
                        CategoryEnum.valueOf(rs.getString("category"))
                );
                ingredientIds.add(idIngredient);
                ingredients.add(ingredient);
            }
            var stockMovements = fetchStockMovementUsingExistingIds(ingredientIds);
            for (Ingredient ingredient : ingredients) {
                ingredient.setStockMovementList(stockMovements.get(ingredient.getId()));
            }
            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void buildCriteriaParams(String ingredientName, CategoryEnum categoryName, String dishName, int page, int size, PreparedStatement ps) throws SQLException {
        int index = 1;
        if (ingredientName != null) {
            ps.setString(index++, "%" + ingredientName + "%");
        }
        if (categoryName != null) {
            ps.setString(index++, categoryName.name());
        }
        if (dishName != null) {
            ps.setString(index++, "%" + dishName + "%");
        }
        ps.setInt(index++, size);
        ps.setInt(index, (page - 1) * size);
    }

    private String buildSQLQuery(String ingredientName, CategoryEnum categoryName, String dishName) {
        String sql = "";
        if (ingredientName != null) {
            sql += " and i.name ilike ?";
        }
        if (categoryName != null) {
            sql += " and i.category = ?::ingredient_category";
        }
        if (dishName != null) {
            sql += " and dish.name ilike ?";
        }
        return sql;
    }

    public Ingredient saveIngredient(Ingredient ingredientToSave) {
        String sql = """
                insert into ingredient (id, name, price, category)
                values (?, ?, ?, ?::ingredient_category)
                on conflict (id) do update set name = excluded.name, price = excluded.price, category = excluded.category
                returning id""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            if (ingredientToSave.getId() == null) {
                ps.setInt(1, next_id(conn, "ingredient"));
            } else {
                ps.setInt(1, ingredientToSave.getId());
            }
            ps.setString(2, ingredientToSave.getName());
            ps.setDouble(3, ingredientToSave.getPrice());
            ps.setObject(4, ingredientToSave.getCategory().name());

            Integer idIngredient;
            try (ResultSet rs = ps.executeQuery()) {
                Ingredient savedIngredient = null;
                rs.next();
                idIngredient = rs.getInt("id");
                savedIngredient = new Ingredient(
                        idIngredient,
                        ingredientToSave.getName(),
                        ingredientToSave.getPrice(),
                        ingredientToSave.getCategory()
                );
                Map<Integer, List<StockMovement>> stockMovements = null;
                if (!ingredientToSave.getStockMovementList().isEmpty()) {
                    attachStockMovement(conn, idIngredient, ingredientToSave.getStockMovementList());
                    stockMovements = fetchStockMovementUsingExistingIds(Collections.singleton(ingredientToSave.getId()));
                    savedIngredient.setStockMovementList(stockMovements.get(idIngredient));
                }
                conn.commit();
                return savedIngredient;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachStockMovement(Connection conn, Integer idIngredient, List<StockMovement> stockMovementList) {
        String sql = """
                insert into stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime)
                values (?, ?, ?, ?::unit_type, ?::movement_type, ?)
                on conflict do nothing""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (StockMovement stockMovement : stockMovementList) {
                if (stockMovement.getId() == null) {
                    ps.setInt(1, next_id(conn, "stock_movement"));
                } else {
                    ps.setInt(1, stockMovement.getId());
                }
                ps.setInt(2, idIngredient);
                ps.setDouble(3, stockMovement.getValue().getQuantity());
                ps.setObject(4, stockMovement.getValue().getUnit().name());
                ps.setObject(5, stockMovement.getType().name());
                ps.setTimestamp(6, Timestamp.from(stockMovement.getCreationDatetime()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order saveOrder(Order orderToSave) {
        String sql = """
                insert into "order" (id, reference, creation_datetime)
                values (?, ?, ?) on conflict (id) do update set reference = excluded.reference, creation_datetime = excluded.creation_datetime
                returning id""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            Integer idOrder;
            Order savedOrder;
            if (orderToSave.getId() == null) {
                idOrder = next_id(conn, "order");
            } else {
                idOrder = orderToSave.getId();
            }
            ps.setInt(1, idOrder);
            if (orderToSave.getReference() == null) {
                ps.setString(2, getReferenceSequenceOrder());
            } else {
                ps.setString(2, orderToSave.getReference());
            }
            ps.setTimestamp(3, Timestamp.from(orderToSave.getCreationDatetime()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                savedOrder = new Order(rs.getInt("id"), orderToSave.getReference(), orderToSave.getCreationDatetime(), orderToSave.getDishOrders());
                detachDishOrder(conn, idOrder);
                List<DishOrder> dishOrders = new ArrayList<>();
                if (!orderToSave.getDishOrders().isEmpty()) {
                    for (Dish dish : orderToSave.getDishOrders().stream().map(DishOrder::getDish).toList()) {
                        Dish dish_ = saveDish(conn, dish);
                        for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                            dishOrders.add(new DishOrder(dishOrder.getId(), dish_, dishOrder.getQuantity()));
                        }
                    }
                    attachDishOrder(conn, idOrder, dishOrders);
                    savedOrder.setDishOrder(dishOrders);
                }
                conn.commit();
                return savedOrder;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachDishOrder(Connection conn, Integer idOrder, List<DishOrder> dishOrders) {
        String sql = """
                insert into dish_order (id, id_order, id_dish, quantity)
                values (?, ?, ?, ?) on conflict (id_order, id_dish)
                    do update set quantity = excluded.quantity;""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = next_id(conn, "dish_order");
            for (DishOrder dishOrder : dishOrders) {
                ps.setInt(1, idx++);
                ps.setInt(2, idOrder);
                ps.setInt(3, dishOrder.getDish().getId());
                ps.setDouble(4, dishOrder.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            duplicationError(e);
            throw new RuntimeException(e);
        }
    }

    private void detachDishOrder(Connection conn, Integer idOrder) {
        String sql = """
                delete from dish_order where id_order = ?;""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOrder);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getReferenceSequenceOrder() {
        String sql = """
                SELECT 'ORD' || LPAD(
                        CAST(COALESCE(MAX(num), 0) + 1 AS TEXT), 5, '0') AS next_reference
                FROM (SELECT CAST(SUBSTRING(reference FROM 4) AS INTEGER) AS num
                      FROM "order"
                      WHERE reference ~ '^ORD\\d{5}$') t limit 1;""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getString("next_reference");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order findOrderByReference(String reference) {
        String sql = """
                select "order".id as order_id, "order".reference, "order".creation_datetime,
                        d_o.id as d_o_id, d_o.id_dish, d_o.quantity
                from "order"
                left join dish_order d_o on d_o.id_order = "order".id
                where reference like ?;""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();
            Order order = null;
            List<DishOrder> dishOrders = new ArrayList<>();
            while (rs.next()) {
                if (order == null) {
                    order = new Order(
                            rs.getInt("order_id"),
                            rs.getString("reference"),
                            rs.getTimestamp("creation_datetime").toInstant(),
                            null
                    );
                }
                int idOrder = rs.getInt("d_o_id");
                if (idOrder > 0) {
                    DishOrder dishOrder = new DishOrder(
                            idOrder,
                            findDishById(conn, rs.getInt("id_dish")),
                            rs.getInt("quantity")
                    );
                    dishOrders.add(dishOrder);
                }
            }
            if (order == null) {
                throw new RuntimeException("Order(reference=" + reference + ") not found");
            }
            order.setDishOrder(dishOrders);
            return order;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public StockValue getStockValueAt(Instant t, Integer ingredientIdentifier) {
        String sql = """
                SELECT unit, SUM(
                    CASE
                        WHEN type = 'IN'  THEN quantity
                        WHEN type = 'OUT' THEN -quantity
                        ELSE 0
                    END
                ) AS actual_quantity
                FROM stock_movement
                WHERE id_ingredient = ? and creation_datetime <= ?
                GROUP BY (unit, id_ingredient);""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientIdentifier);
            ps.setTimestamp(2, Timestamp.from(t));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new StockValue(
                        rs.getDouble("actual_quantity"),
                        UnitType.valueOf(rs.getString("unit"))
                );
            }
            return new StockValue(0.0, UnitType.KG);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Double getDishCost(Integer dishId) {
        String sql = """
                select sum(
                    case
                        when ingredient.price = 0 then 0
                        else ingredient.price * d_i.quantity_required
                    end) as total
                from dish_ingredient d_i
                join ingredient on d_i.id_ingredient = ingredient.id
                where d_i.id_dish = ?;""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
            throw new RuntimeException("Dish(" + dishId + ") not found");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Double getGrossMargin(Integer dishId) {
        String sql = """
                SELECT d.price - SUM(i.price * di.quantity_required) AS gross_margin
                FROM dish d
                LEFT JOIN dish_ingredient di ON di.id_dish = d.id
                LEFT JOIN ingredient i ON i.id = di.id_ingredient
                WHERE d.id = ?
                GROUP BY d.id, d.price;""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("gross_margin");
            }
            throw new RuntimeException("Dish(" + dishId + ") not found");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, List<StatsFormat>> getStatisticOfStock(Periodicity periodicity, Instant intervalMin, Instant intervalMax) {
        String sql = """
                SELECT i.name,
                       json_agg(
                           jsonb_build_array(
                               to_char(gs.periodicity, 'YYYY-MM-DD'),
                               COALESCE(
                                   (SELECT SUM(CASE WHEN stock.type = 'IN' THEN stock.quantity ELSE -stock.quantity END)
                                    FROM stock_movement stock
                                    WHERE stock.id_ingredient = i.id AND stock.creation_datetime <=
                                          (gs.periodicity + ?::interval - INTERVAL '1 microsecond')),
                                   0)::numeric(12,3),
                               COALESCE(
                                   (SELECT stock.unit::text FROM stock_movement stock
                                    WHERE stock.id_ingredient = i.id
                                    ORDER BY stock.creation_datetime DESC LIMIT 1),
                                   'KG')::unit_type
                           ) ORDER BY gs.periodicity
                       ) AS stock_statistic
                FROM ingredient i
                CROSS JOIN LATERAL generate_series(?::date, ?::date, ?::interval) gs(periodicity)
                GROUP BY i.id, i.name
                ORDER BY i.name;""";

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String period = "1 " + periodicity.name().toLowerCase();
            ps.setString(1, period);
            ps.setDate(2, Date.valueOf(intervalMin.atZone(ZoneId.systemDefault()).toLocalDate()));
            ps.setDate(3, Date.valueOf(intervalMax.atZone(ZoneId.systemDefault()).toLocalDate()));
            ps.setString(4, period);

            ResultSet rs = ps.executeQuery();
            HashMap<String, List<StatsFormat>> result = new HashMap<>();
            while (rs.next()) {
                String name = rs.getString("name");
                String json = rs.getString("stock_statistic");

                if (json == null) {
                    result.put(name, List.of());
                    continue;
                }

                JSONArray arr = new JSONArray(json);
                List<StatsFormat> stats = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONArray item = arr.getJSONArray(i);
                    stats.add(new StatsFormat(
                            Instant.parse(item.getString(0)),
                            new StockValue(
                                    item.getDouble(1),
                                    UnitType.valueOf(item.getString(2))
                            )
                    ));
                }
                result.put(name, stats);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dish> findDishByIngredientsName(String eur) {
        String sql = """
                select d.id as dish_id
                from dish d
                join dish_ingredient d_i on d_i.id_dish = d.id
                join ingredient i on d_i.id_ingredient = i.id
                where i.name ilike ?""";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + eur + "%");
            ResultSet rs = ps.executeQuery();
            List<Dish> dishes = new ArrayList<>();
            while (rs.next()) {
                dishes.add(findDishById(conn, rs.getInt("dish_id")));
            }
            if (dishes.isEmpty()) {
                return null;
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetData() {
        String sql = """
                delete from dish cascade;
                delete from ingredient cascade;
                delete from "order" cascade;
                """;
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialiseData(DataRetriever dataRetriever) {
        dataRetriever.resetData();
        Dotenv dotenv = Dotenv.load();
        String sqlFilePath = "E:/HEI/L2/TD/exo jdbc/restaurant-manager/src/main/resources/sql/data.sql";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "psql",
                    "-U", dotenv.get("USER"),
                    "-d", "mini_dish_db",
                    "-h", "localhost",
                    "-p", "5432",
                    "-f", sqlFilePath
            );

            pb.environment().put("PGPASSWORD", dotenv.get("PASSWORD"));
            Process process = pb.start();

            Thread outReader = new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Thread errReader = new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("[psql ERR] " + line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            outReader.start();
            errReader.start();
            int exitCode = process.waitFor();
            outReader.join(1500);
            errReader.join(1500);

            if (exitCode != 0) {
                throw new RuntimeException("Échec psql → code " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}