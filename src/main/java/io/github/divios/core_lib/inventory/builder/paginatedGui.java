package io.github.divios.core_lib.inventory.builder;

import com.google.common.base.Preconditions;
import io.github.divios.core_lib.Core_lib;
import io.github.divios.core_lib.inventory.InventoryGUI;
import io.github.divios.core_lib.inventory.ItemButton;
import io.github.divios.core_lib.inventory.inventoryUtils;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.Pair;
import io.github.divios.core_lib.scheduler.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class paginatedGui {

    private final BiFunction<Integer, Integer, String> title;
    private final Supplier<List<ItemButton>> items;
    private final Pair<ItemStack, Integer> backButton;
    private final Pair<ItemStack, Integer> nextButton;
    private final Pair<ItemButton, Integer> exitButton;
    private final BiConsumer<InventoryGUI, Integer> withButtons;
    private final PopulatorContentContext populator;

    private final List<InventoryGUI> invs = new ArrayList<>();

    private paginatedGui(
            BiFunction<Integer, Integer, String> title,
            Supplier<List<ItemButton>> items,
            Pair<ItemStack, Integer> backButton,
            Pair<ItemStack, Integer> nextButton,
            Pair<ItemButton, Integer> exitButton,
            BiConsumer<InventoryGUI, Integer> withButtons,
            PopulatorContentContext populator
    ) {
        this.title = title;
        this.items = items;
        this.backButton = backButton;
        this.nextButton = nextButton;
        this.exitButton = exitButton;
        this.withButtons = withButtons;
        this.populator = populator;

        init();
    }

    public void open(Player p) {
        invs.get(0).open(p);
    }

    public void open(Player p, int index) {
        if (index >= invs.size()) return;

        invs.get(index).open(p);
    }

    public List<InventoryGUI> getInvs() {
        return Collections.unmodifiableList(invs);
    }

    public InventoryGUI getInv(int index) {
        if (index >= invs.size()) return null;

        return invs.get(index);
    }

    private void init() {

        Inventory dummyInv = Bukkit.createInventory(null, 54, "");
        if (populator != null) populator.apply(dummyInv);
        dummyInv.setItem(backButton.get2(), backButton.get1());
        dummyInv.setItem(nextButton.get2(), nextButton.get1());

        List<ItemButton> itemButtons = items.get();

        int max = (int) Math.ceil((float) itemButtons.size() / inventoryUtils.getEmptySlots(dummyInv));
        if (max == 0) max = 1;

        Inventory skeletonInv = Bukkit.createInventory(null, 54);
        if (populator != null) populator.apply(skeletonInv);     // Apply populator

        Iterator<ItemButton> iteratorButton = itemButtons.iterator();

        for (int i = 0; i <= max; i++) {         // initial population

            InventoryGUI invGui = new InventoryGUI(Core_lib.PLUGIN, 54, title.apply(i + 1, max));
            invGui.getInventory().setContents(skeletonInv.getContents());

            if (i != 0) {
                int finalI = i;
                invGui.addButton(backButton.get2(), ItemButton.create(backButton.get1(),
                        e -> invs.get(finalI - 1).open((Player) e.getWhoClicked())));
            }

            if (i != max - 1) {
                int finalI = i;
                invGui.addButton(nextButton.get2(), ItemButton.create(nextButton.get1(),
                        e -> invs.get(finalI + 1).open((Player) e.getWhoClicked())));
            }

            invGui.addButton(exitButton.get2(), exitButton.get1());

            withButtons.accept(invGui, i);

            invGui.setDestroyOnClose(false);
            invs.add(invGui);

            Schedulers.sync().run(() -> {
                int slot;
                while (iteratorButton.hasNext()
                        && (slot = inventoryUtils.getFirstEmpty(invGui.getInventory())) != -1) {
                    invGui.addButton(slot, iteratorButton.next());
                }
            });
        }

    }

    public inventoryPopulatorState getPopulator() {
        return populator.toState();
    }

    public void destroy() {
        invs.forEach(InventoryGUI::destroy);
    }

    public static paginatedGuiBuilder Builder() {
        return new BuilderImpl();
    }

    protected static final class BuilderImpl implements paginatedGuiBuilder {

        private BiFunction<Integer, Integer, String> title;
        private Supplier<List<ItemButton>> items;
        private Pair<ItemStack, Integer> backButton;
        private Pair<ItemStack, Integer> nextButton;
        private Pair<ItemButton, Integer> exitButton;
        private BiConsumer<InventoryGUI, Integer> withButtons;
        private PopulatorContentContext populator;

        BuilderImpl() {
        }

        @Override
        public paginatedGuiBuilder withTitle(String title) {
            this.title = (current, max) -> title;
            return this;
        }

        @Override
        public paginatedGuiBuilder withTitle(BiFunction<Integer, Integer, String> title) {
            this.title = title;
            return this;
        }

        public paginatedGuiBuilder withItems(Supplier<List<ItemButton>> items) {
            this.items = items;
            return this;
        }

        @Override
        public paginatedGuiBuilder withBackButton(ItemStack item, int slot) {
            this.backButton = Pair.of(item, slot);
            return this;
        }

        @Override
        public paginatedGuiBuilder withNextButton(ItemStack item, int slot) {
            this.nextButton = Pair.of(item, slot);
            return this;
        }

        @Override
        public paginatedGuiBuilder withExitButton(ItemButton item, int slot) {
            this.exitButton = Pair.of(item, slot);
            return this;
        }

        @Override
        public paginatedGuiBuilder withExitButton(ItemStack item, Consumer<InventoryClickEvent> e, int slot) {
            return withExitButton(ItemButton.create(item, e), slot);
        }

        @Override
        public paginatedGuiBuilder withButtons(BiConsumer<InventoryGUI, Integer> withButtons) {
            this.withButtons = withButtons;
            return this;
        }

        @Override
        public paginatedGuiBuilder withPopulator(inventoryPopulatorState populator) {
            this.populator = populator.restore();
            return this;
        }

        @Override
        public paginatedGuiBuilder withPopulator(PopulatorContentContext populator) {
            this.populator = populator;
            return this;
        }

        @Override
        public paginatedGui build() {

            Preconditions.checkNotNull(items, "items null");
            Preconditions.checkNotNull(backButton, "backButton null");
            Preconditions.checkNotNull(nextButton, "nextButton null");

            Preconditions.checkArgument(!ItemUtils.isEmpty(backButton.get1()), "backbutton is empty");
            Preconditions.checkArgument(!ItemUtils.isEmpty(backButton.get1()), "nextbutton is empty");
            Preconditions.checkArgument(backButton.get2() >= 0 && backButton.get2() < 54,
                    "Backbutton slot out of bounds");
            Preconditions.checkArgument(nextButton.get2() >= 0 && nextButton.get2() < 54,
                    "nextButton slot out of bounds");
            Preconditions.checkArgument(exitButton.get2() >= 0 && exitButton.get2() < 54,
                    "nextButton slot out of bounds");

            if (withButtons == null) withButtons = (e, i) -> {
            };
            if (title == null) title = (current, max) -> "";

            return new paginatedGui(title, items, backButton, nextButton, exitButton, withButtons, populator);
        }

        @Override
        public CompletableFuture<paginatedGui> buildFuture() {
            return CompletableFuture.supplyAsync(this::build);
        }
    }
}
