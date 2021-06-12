package me.jaackson.etched.bridge;

import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Jackson
 */
public final class RegistryBridge {

    @ExpectPlatform
    public static <T extends SoundEvent> Supplier<T> registerSound(String name, Supplier<T> object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <B extends BlockEntity, T extends BlockEntityType.Builder<B>> Supplier<BlockEntityType<B>> registerBlockEntity(String name, Supplier<T> object) {
        return Platform.safeAssertionError();
    }

    @ExpectPlatform
    public static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String name, RegistryBridge.MenuFactory<T> object) {
        return Platform.safeAssertionError();
    }

    @SafeVarargs
    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static void registerItemColor(ItemColor color, Supplier<Item>... items) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static void registerItemOverride(Item item, ResourceLocation resourceLocation, ItemPropertyFunction itemPropertyFunction) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static void registerBlockRenderType(Block block, RenderType type) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreenFactory(MenuType<M> type, ScreenFactory<M, S> object) {
        Platform.safeAssertionError();
    }

    @ExpectPlatform
    @Environment(EnvType.CLIENT)
    public static void registerSprite(ResourceLocation sprite, ResourceLocation atlas) {
        Platform.safeAssertionError();
    }

    public static Supplier<Block> registerBlock(String name, Supplier<Block> block, Item.Properties properties) {
        return registerBlock(name, block, blockSupplier -> new BlockItem(blockSupplier.get(), properties));
    }

    public static Supplier<Block> registerBlock(String name, Supplier<Block> block, Function<Supplier<Block>, Item> item) {
        Supplier<Block> register = registerBlock(name, block);
        registerItem(name, () -> item.apply(register));
        return register;
    }

    @FunctionalInterface
    public interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory);
    }

    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
        S create(M menu, Inventory inventory, Component title);
    }
}
