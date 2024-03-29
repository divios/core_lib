package io.github.divios.core_lib.inventory;

import com.cryptomorin.xseries.XMaterial;
import io.github.divios.core_lib.Core_lib;
import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.ChatPrompt;
import io.github.divios.core_lib.misc.EventListener;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.core_lib.scheduler.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.IntStream;

public class dynamicGui implements InventoryHolder, Listener {

    private final Plugin plugin;
    private final Player p;
    private final contentX contentX;
    private final Function<Integer, String> title;
    private final Consumer<Player> back;
    private final Integer rows2fill;
    private final Function<InventoryClickEvent, Response> contentAction;
    private final BiFunction<Integer, Player, Response> nonContentAction;
    private final BiConsumer<Inventory, Integer> addItems;

    private final boolean searchOn;
    private final boolean isSearch;
    private final Integer page;
    private AtomicBoolean preventCloseB;

    private EventListener<InventoryCloseEvent> preventClose;

    private final List<Inventory> invsList = new ArrayList<>();

    private dynamicGui(
            Plugin plugin,
            Player p,
            contentX contentX,
            Function<Integer, String> title,
            Consumer<Player> back,
            Integer rows2fill,
            Function<InventoryClickEvent, Response> contentAction,
            BiFunction<Integer, Player, Response> nonContentAction,
            BiConsumer<Inventory, Integer> addItems,
            boolean searchOn,
            boolean isSearch,
            Integer page,
            boolean preventClose

    ) {
        this.plugin = plugin;
        this.p = p;
        this.contentX = contentX;
        this.title = title;
        this.back = back;
        this.rows2fill = rows2fill;
        this.contentAction = contentAction;
        this.nonContentAction = nonContentAction;
        this.addItems = addItems;
        this.searchOn = searchOn;
        this.isSearch = isSearch;
        this.page = page;
        this.preventCloseB = new AtomicBoolean(preventClose);

        this.preventClose = new EventListener<>(
                InventoryCloseEvent.class, (l, e) -> {
            if (e.getInventory().getHolder() != this)
                return;

            if (preventCloseB.get()) {
                l.unregister();
                unregister();
                Schedulers.sync().runLater( () -> new dynamicGui(plugin, p, contentX, title, back, rows2fill, contentAction,
                                nonContentAction, addItems, searchOn, false, page, preventCloseB.get()),
                        1L);
            }
        });


        Bukkit.getPluginManager().registerEvents(this, plugin);

        createStructure();
        p.openInventory(invsList.get(page));

    }

    private static class contentX {
        Supplier<List<ItemStack>> update;
        public List<ItemStack> contentS;
        public List<ItemStack> searchContent;

        public contentX(Supplier<List<ItemStack>> contentS) {
            this.update = contentS;
            this.contentS = contentS.get();
        }

        public contentX update() {
            this.contentS = update.get();
            return this;
        }
    }

    private void createStructure() {

        List<ItemStack> content;
        if (!isSearch) content = contentX.contentS;
        else content = contentX.searchContent;

        double nD = content.size() / Double.valueOf(rows2fill);
        int n = (int) Math.ceil(nD);

        IntStream.range(0, n).forEach(i -> {
            if (i + 1 == n) {
                invsList.add(createSingleInv(i + 1, 2));
            } else if (i == 0) invsList.add(createSingleInv(i + 1, 0));
            else invsList.add(createSingleInv(i + 1, 1));
        });

        if (invsList.isEmpty()) {
            Inventory firstInv = Bukkit.createInventory(this, 54, FormatUtils.color(title.apply(0)));
            setDefaultItems(firstInv);
            invsList.add(firstInv);
        }
    }

    private Inventory createSingleInv(int page, int pos) {

        final int[] slot = {0};
        Inventory returnGui = Bukkit.createInventory(this, 54, FormatUtils.color(title.apply(page)));


        List<ItemStack> content;
        if (!isSearch) content = contentX.contentS;
        else content = contentX.searchContent;

        setDefaultItems(returnGui);
        if (pos == 0 && content.size() > 45) setNextItem(returnGui);
        if (pos == 1) {
            setNextItem(returnGui);
            setPreviousItem(returnGui);
        }
        if (pos == 2 && content.size() > 45) {
            setPreviousItem(returnGui);
        }

        for (ItemStack item : content) {
            if (slot[0] == rows2fill * page) break;
            if (slot[0] >= (page - 1) * rows2fill) returnGui.setItem(slot[0] - (page - 1) * rows2fill, item);

            slot[0]++;
        }


        return returnGui;
    }

    private Inventory processNextGui(Inventory inv, int dir) {
        return invsList.get(invsList.indexOf(inv) + dir);
    }

    private void setDefaultItems(Inventory inv) {

        ItemStack backItem = new ItemBuilder(XMaterial.OAK_DOOR)  //back button
                .setName("&c&lReturn").setLore("&7Click to go back");
        inv.setItem(49, backItem);

        if (addItems != null) addItems.accept(inv, page);

        if (!searchOn) return;

        ItemStack search = null;
        if (!isSearch) {
            search = new ItemBuilder(XMaterial.COMPASS)
                    .setName("&b&lSearch").setLore("&7Click to search item");
        } else {
            search = new ItemBuilder(XMaterial.REDSTONE_BLOCK)
                    .setName("&c&lCancel search").setLore("&7Click to cancel search");
        }
        inv.setItem(52, search);

    }

    private void setNextItem(Inventory inv) {
        ItemStack next = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setName("&6&lNext").applyTexture("19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf");
        inv.setItem(51, next);
    }

    private void setPreviousItem(Inventory inv) {
        ItemStack previous = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setName("&6&lPrevious").applyTexture("bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9");
        inv.setItem(47, previous);
    }

    private void searchAction(ItemStack item) {
        if (item.getType() == XMaterial.REDSTONE_BLOCK.parseMaterial()) {
            preventClose.unregister();
            new dynamicGui(plugin, p, contentX, title, back, rows2fill, contentAction,
                    nonContentAction, addItems, searchOn, false, page, preventCloseB.get());
        } else {
            final List<ItemStack> lists = new ArrayList<>();

            ChatPrompt.builder()
                    .withPlayer(p)
                    .withResponse(s -> {

                        for (ItemStack i : contentX.contentS) {
                            String name = FormatUtils.stripColor(i.getItemMeta().getDisplayName());
                            if (name.toLowerCase().startsWith(s.toLowerCase())) {
                                lists.add(i);
                            }
                        }
                        contentX.searchContent = lists;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Core_lib.PLUGIN, ()-> {
                            preventClose.unregister();
                            new dynamicGui(plugin, p, contentX, title, back, rows2fill, contentAction,
                                    nonContentAction, addItems, searchOn, true, page, preventCloseB.get());
                        }, 1L);
                    })
                    .withCancel(cancelReason -> {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Core_lib.PLUGIN, ()-> {
                            preventClose.unregister();
                            new dynamicGui(plugin, p, contentX, title, back, rows2fill, contentAction,
                                    nonContentAction, addItems, searchOn, true, page, preventCloseB.get());
                        }, 1L);
                    })
                    .withTitle("Insert text to search")
                    .prompt();

        }
    }

    public List<Inventory> getinvs() {
        return invsList;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);

        if (e.getSlot() != e.getRawSlot()) return;

        int slot = e.getSlot();
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Inventory inv = e.getView().getTopInventory();
        int pos = invsList.indexOf(inv);

        if (slot == 49) {
            unregister();
            preventClose.unregister();
            back.accept(p);
        } else if (slot == 51 && pos != invsList.size() - 1) {
            boolean aux = preventCloseB.get();
            preventCloseB.set(false);
            p.openInventory(processNextGui(inv, 1));
            preventCloseB.set(aux);
        } else if (slot == 47 && pos != 0) {
            boolean aux = preventCloseB.get();
            preventCloseB.set(false);
            p.openInventory(processNextGui(inv, -1));
            preventCloseB.set(aux);
        } else if (e.getSlot() == 52 && searchOn) searchAction(item);                             /* Search button */

        else {
            Response response;
            if (slot >= 0 && slot < rows2fill && !ItemUtils.isEmpty(item)) {
                response = contentAction.apply(e);
            } else {
                response = nonContentAction.apply(slot, p);
            }

            if (response == null) return;

            if (response.getResponse() == ResponseX.CLOSE) {
                preventClose.unregister();
                unregister();
                p.closeInventory();
            } else if (response.getResponse() == ResponseX.UPDATE) {
                preventClose.unregister();
                new dynamicGui(plugin, p, contentX.update(), title, back, rows2fill, contentAction,
                        nonContentAction, addItems, searchOn, isSearch, page, preventCloseB.get());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
    }

    public void unregister() {
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
    }


    public static class Builder {

        private Plugin plugin;
        private Player p;
        private contentX content = null;
        private Function<Integer, String> title = integer -> "";
        private Consumer<Player> back = player -> {
        };
        private Integer rows2fill = 45;
        private Function<InventoryClickEvent, Response> contentAction = InventoryClickEvent -> {
            return null;
        };
        private BiFunction<Integer, Player, Response> nonContentAction = (integer, Player) -> {
            return null;
        };
        private BiConsumer<Inventory, Integer> addItems = (itemStacks, integer) -> {
        };

        private boolean searchOn = true;
        private boolean isSearch = false;
        private boolean preventClose = false;
        private Integer page = 0;

        public Builder contents(Supplier<List<ItemStack>> content) {
            this.content = new contentX(content);
            return this;
        }

        public Builder title(Function<Integer, String> title) {
            this.title = title;
            return this;
        }

        public Builder back(Consumer<Player> back) {
            this.back = back;
            return this;
        }

        public Builder rows(int rows) {
            this.rows2fill = rows;
            return this;
        }

        public Builder contentAction(Function<InventoryClickEvent, Response> contentAction) {
            this.contentAction = contentAction;
            return this;
        }

        public Builder nonContentAction(BiFunction<Integer, Player, Response> nonContentAction) {
            this.nonContentAction = nonContentAction;
            return this;
        }

        public Builder addItems(BiConsumer<Inventory, Integer> addItems) {
            this.addItems = addItems;
            return this;
        }

        public Builder setSearch(boolean status) {
            searchOn = status;
            return this;
        }

        public Builder isSearch(boolean isSearch) {
            this.isSearch = isSearch;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder preventClose() {
            this.preventClose = true;
            return this;
        }

        public Builder setPreventClose(boolean preventClose) {
            this.preventClose = preventClose;
            return this;
        }

        public Builder plugin(Plugin plugin) {
            this.plugin = plugin;
            return this;
        }

        public dynamicGui open(Player p) {
            this.p = p;

            return new dynamicGui(plugin, p, content, title, back, rows2fill, contentAction,
                    nonContentAction, addItems, searchOn, isSearch, page, preventClose);
        }
    }

    public static class Response {

        private final ResponseX response;

        private Response(ResponseX response) {
            this.response = response;
        }

        public ResponseX getResponse() {
            return this.response;
        }

        public static Response close() {
            return new Response(ResponseX.CLOSE);
        }

        public static Response update() {
            return new Response(ResponseX.UPDATE);
        }

        public static Response nu() {
            return new Response(ResponseX.NU);
        }
    }

    private enum ResponseX {
        NU,
        UPDATE,
        CLOSE
    }

}