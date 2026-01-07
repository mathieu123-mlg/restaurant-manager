package td.restaurantmanager;

public class Main {
    public static void main(String[] args) {
        DataRetriever data = new DataRetriever();

        Dish salade_fraiche = data.findDishById(1);

        System.out.println(salade_fraiche.getGrossMargin());

        Dish riz_au_legume = data.findDishById(3);
        System.out.println(riz_au_legume.getGrossMargin());

    }
}
