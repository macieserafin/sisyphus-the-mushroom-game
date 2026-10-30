package main;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class Shop {
    public static class Offer {
        public final String name;
        public final String description;
        private final IntSupplier price;
        private final IntSupplier bought;
        private final int limit;
        private final Runnable purchase;

        public Offer(String name, String description, IntSupplier price, IntSupplier bought, int limit, Runnable purchase) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.bought = bought;
            this.limit = limit;
            this.purchase = purchase;
        }

        public static Offer once(String name, String description, int price, BooleanSupplier owned, Runnable purchase) {
            return new Offer(name, description, () -> price, () -> owned.getAsBoolean() ? 1 : 0, 1, purchase);
        }

        public int price() {
            return price.getAsInt();
        }

        public int bought() {
            return bought.getAsInt();
        }

        public int limit() {
            return limit;
        }

        public boolean soldOut() {
            return bought() >= limit;
        }

        void buy() {
            purchase.run();
        }
    }

    public final String greeting;
    public final List<Offer> offers;

    public Shop(String greeting, Offer... offers) {
        this.greeting = greeting;
        this.offers = Arrays.asList(offers);
    }


    public static Shop merchant(GamePanel gp) {
        Progression p = gp.progression;
        return new Shop("Have a look. Everything here costs mushrooms.",
                new Offer("Heart Cap", "Max health +1. Permanent.",
                        () -> 40 * (p.healthUpgrades() + 1), p::healthUpgrades, Progression.MAX_HEALTH_UPGRADES,
                        () -> p.buyHealth(gp.player)),
                new Offer("Spore Shell", "Max shield +1. Permanent.",
                        () -> 60 * (p.shieldUpgrades() + 1), p::shieldUpgrades, Progression.MAX_SHIELD_UPGRADES,
                        () -> p.buyShield(gp.player)));
    }

    public static Shop witch(GamePanel gp) {
        Progression p = gp.progression;
        return new Shop("Brewed this morning. Mostly safe.",
                new Offer("Swiftcap Elixir", "Speed +1. It stays with you, not just for a run.",
                        () -> 100 * (p.speedElixirs() + 1), p::speedElixirs, Progression.MAX_SPEED_ELIXIRS,
                        () -> p.buySpeedElixir(gp.player)));
    }

    public static Shop sage(GamePanel gp) {
        Progression p = gp.progression;
        return new Shop("Iron from your world. The last one who carried it no longer needs it.",
                Offer.once("Shotgun", "Three pellets, short range, strong push. Second gun slot.",
                        120, p::ownsShotgun, () -> p.buyShotgun(gp.player)));
    }

    public static Shop smith(GamePanel gp) {
        Progression p = gp.progression;
        return new Shop("Forged it for someone who never came back. It will fit you.",
                Offer.once("Heavy Sword", "Slow, wide and brutal. Second blade slot.",
                        120, p::ownsHeavySword, () -> p.buyHeavySword(gp.player)));
    }
}
