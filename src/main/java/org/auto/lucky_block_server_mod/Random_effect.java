package org.auto.lucky_block_server_mod;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.auto.lucky_block_server_mod.cache.RandomEffectConfigData;
import org.auto.lucky_block_server_mod.cache.RandomEffectConfigManager;

import java.util.Random;
public class Random_effect {
    private static final Random RANDOM = new Random();

    public static void applyRandomEffect(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        RandomEffectConfigData cfg = RandomEffectConfigManager.getConfig();
        if (cfg == null || cfg.effects == null) return;

        // 依據原始邏輯隨機 0~2 (權重暫採原始隨機，若需要亦可從 JSON 權重計算)
        int effectType = RANDOM.nextInt(3);

        switch (effectType) {
            case 0:
                giveRandomItem(serverPlayer, cfg.effects.item);
                serverPlayer.sendMessage(Text.literal(cfg.effects.item.message), false);
                break;
            case 1:
                giveRandomPotionEffect(serverPlayer, cfg.effects.potion);
                serverPlayer.sendMessage(Text.literal(cfg.effects.potion.message), false);
                break;
            case 2:
                modifyMaxHealth(serverPlayer, cfg.effects.health);
                serverPlayer.sendMessage(Text.literal(cfg.effects.health.message), false);
                break;
        }
    }

    private static void giveRandomItem(ServerPlayerEntity player, RandomEffectConfigData.ItemEffect itemCfg) {
        int category = RANDOM.nextInt(4);
        ItemStack reward = null;

        switch (category) {
            case 0 -> {
                var normalList = itemCfg.categories.normal_items;
                var data = normalList.get(RANDOM.nextInt(normalList.size()));
                Item item = Registries.ITEM.get(Identifier.of(data.id));
                reward = new ItemStack(item, data.count);
            }
            case 1 -> reward = getRandomEquip(player, itemCfg.categories.weapons);
            case 2 -> reward = getRandomEquip(player, itemCfg.categories.tools);
            case 3 -> reward = getRandomEquip(player, itemCfg.categories.armors);
        }

        if (reward != null && !reward.isEmpty()) {
            player.getInventory().offerOrDrop(reward);
            // 這裡保留原版的 getName().getString()
            player.sendMessage(Text.literal("§7獲得: §f" + reward.getItem().getName().getString()), false);
        }
    }

    // 將武器、工具、防具的重複邏輯整合
    private static ItemStack getRandomEquip(ServerPlayerEntity player, RandomEffectConfigData.WeaponToolArmorConfig config) {
        String randomId = config.pool.get(RANDOM.nextInt(config.pool.size()));
        Item item = Registries.ITEM.get(Identifier.of(randomId));
        ItemStack stack = new ItemStack(item);

        // 檢查是否被排除附魔 (例如 Elytra)
        boolean isExcluded = config.exclude_enchant != null && config.exclude_enchant.contains(randomId);

        if (RANDOM.nextDouble() < config.enchant_chance && !isExcluded) {
            // 1.21+ 安全獲取附魔的 RegistryEntry 方式
            Identifier enchantId = Identifier.of(config.enchantment);
            RegistryKey<Enchantment> key = RegistryKey.of(RegistryKeys.ENCHANTMENT, enchantId);

            player.getWorld().getRegistryManager()
                    .getOptional(RegistryKeys.ENCHANTMENT)
                    .flatMap(r -> r.getOptional(key))
                    .ifPresent(entry -> {
                        int lvl = RANDOM.nextInt((config.max_level - config.min_level) + 1) + config.min_level;
                        stack.addEnchantment(entry, lvl);
                    });
        }
        return stack;
    }

    private static void giveRandomPotionEffect(ServerPlayerEntity player, RandomEffectConfigData.PotionEffect potionCfg) {
        boolean isGood = RANDOM.nextDouble() < potionCfg.good_effect_chance;
        var effectList = isGood ? potionCfg.good_effects : potionCfg.bad_effects;

        RandomEffectConfigData.PotionData data = effectList.get(RANDOM.nextInt(effectList.size()));
        Identifier effectId = Identifier.of(data.id);

        // 從註冊表撈取狀態效果
        StatusEffect statusEffect = Registries.STATUS_EFFECT.get(effectId);
        if (statusEffect != null) {
            // 1.21 需傳入 RegistryEntry
            RegistryEntry<StatusEffect> entry = Registries.STATUS_EFFECT.getEntry(statusEffect);
            StatusEffectInstance instance = new StatusEffectInstance(entry, data.duration_ticks, data.amplifier);
            player.addStatusEffect(instance);

            String msgPrefix = isGood ? "§a正面效果: " : "§c負面效果: ";
            player.sendMessage(Text.literal(msgPrefix + statusEffect.getName().getString()), false);
        }
    }

    private static void modifyMaxHealth(ServerPlayerEntity player, RandomEffectConfigData.HealthEffect healthCfg) {
        boolean isIncrease = RANDOM.nextBoolean();

        var attributeInstance = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attributeInstance == null) return;

        double currentMax = player.getMaxHealth();
        // 根據 JSON 設定的範圍隨機改變量
        double changeAmount = RANDOM.nextInt((healthCfg.max_change - healthCfg.min_change)) + healthCfg.min_change;

        if (isIncrease) {
            double newMax = currentMax + changeAmount;
            attributeInstance.setBaseValue(newMax);
            // 使用 String.format 將原本的 %s 替換成心心數量
            player.sendMessage(Text.literal(String.format(healthCfg.increase_msg, (changeAmount / 2))), false);
        } else {
            double newMax = Math.max(healthCfg.min_health_limit, currentMax - changeAmount);
            attributeInstance.setBaseValue(newMax);
            player.sendMessage(Text.literal(String.format(healthCfg.decrease_msg, (changeAmount / 2))), false);
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
//public class Random_effect {
//    private static final Random RANDOM = new Random();
//
//    public static void applyRandomEffect(PlayerEntity player) {
//        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
//
//        int effectType = RANDOM.nextInt(3);
//
//        switch (effectType) {
//            case 0:
//                giveRandomItem(serverPlayer);
//                serverPlayer.sendMessage(Text.literal("§a✦ 你獲得了隨機物品！ ✦"), false);
//                break;
//            case 1:
//                giveRandomPotionEffect(serverPlayer);
//                serverPlayer.sendMessage(Text.literal("§b✦ 你獲得了藥水效果！ ✦"), false);
//                break;
//            case 2:
//                modifyMaxHealth(serverPlayer);
//                serverPlayer.sendMessage(Text.literal("§d✦ 你的血量上限改變了！ ✦"), false);
//                break;
//        }
//    }
//
//    private static void giveRandomItem(ServerPlayerEntity player) {
//        int category = RANDOM.nextInt(4);
//        ItemStack reward = null;
//
//        switch (category) {
//            case 0 -> reward = getRandomItem();
//            case 1 -> reward = getRandomWeapon(player);
//            case 2 -> reward = getRandomTool(player);
//            case 3 -> reward = getRandomArmor(player);
//        }
//
//        if (reward != null) {
//            player.getInventory().offerOrDrop(reward);
//            player.sendMessage(Text.literal("§7獲得: §f" + reward.getItem().getName().getString()), false);
//        }
//    }
//
//    private static ItemStack getRandomItem() {
//        ItemStack[] items = {
//                new ItemStack(Items.DIAMOND, 5),
//                new ItemStack(Items.NETHERITE_INGOT, 1),
//                new ItemStack(Items.EMERALD, 10),
//                new ItemStack(Items.GOLD_INGOT, 16),
//                new ItemStack(Items.IRON_INGOT, 32),
//                new ItemStack(Items.TOTEM_OF_UNDYING, 1)
//        };
//        return items[RANDOM.nextInt(items.length)];
//    }
//
//    private static ItemStack getRandomWeapon(ServerPlayerEntity player) {
//        ItemStack[] weapons = {
//                new ItemStack(Items.DIAMOND_SWORD),
//                new ItemStack(Items.NETHERITE_SWORD),
//                new ItemStack(Items.DIAMOND_AXE),
//                new ItemStack(Items.BOW),
//                new ItemStack(Items.CROSSBOW)
//        };
//        ItemStack weapon = weapons[RANDOM.nextInt(weapons.length)];
//        if (RANDOM.nextBoolean()) {
//            RegistryEntry<Enchantment> sharpness = player.getWorld().getRegistryManager()
//                    .getOrThrow(RegistryKeys.ENCHANTMENT)
//                    .getOrThrow(Enchantments.SHARPNESS);
//            weapon.addEnchantment(sharpness, RANDOM.nextInt(3) + 1);
//        }
//        return weapon;
//    }
//
//    private static ItemStack getRandomTool(ServerPlayerEntity player) {
//        ItemStack[] tools = {
//                new ItemStack(Items.DIAMOND_PICKAXE),
//                new ItemStack(Items.NETHERITE_PICKAXE),
//                new ItemStack(Items.DIAMOND_SHOVEL),
//                new ItemStack(Items.ELYTRA)
//        };
//        ItemStack tool = tools[RANDOM.nextInt(tools.length)];
//        if (RANDOM.nextBoolean() && !tool.getItem().equals(Items.ELYTRA)) {
//            RegistryEntry<Enchantment> efficiency = player.getWorld().getRegistryManager()
//                    .getOrThrow(RegistryKeys.ENCHANTMENT)
//                    .getOrThrow(Enchantments.EFFICIENCY);
//            tool.addEnchantment(efficiency, RANDOM.nextInt(4) + 1);
//        }
//        return tool;
//    }
//
//    private static ItemStack getRandomArmor(ServerPlayerEntity player) {
//        ItemStack[] armors = {
//                new ItemStack(Items.DIAMOND_HELMET),
//                new ItemStack(Items.DIAMOND_CHESTPLATE),
//                new ItemStack(Items.NETHERITE_HELMET),
//                new ItemStack(Items.NETHERITE_CHESTPLATE),
//                new ItemStack(Items.LEATHER_HELMET),
//                new ItemStack(Items.LEATHER_CHESTPLATE)
//        };
//        ItemStack armor = armors[RANDOM.nextInt(armors.length)];
//        if (RANDOM.nextBoolean()) {
//            RegistryEntry<Enchantment> protection = player.getWorld().getRegistryManager()
//                    .getOrThrow(RegistryKeys.ENCHANTMENT)
//                    .getOrThrow(Enchantments.PROTECTION);
//            armor.addEnchantment(protection, RANDOM.nextInt(3) + 1);
//        }
//        return armor;
//    }
//
//    private static void giveRandomPotionEffect(ServerPlayerEntity player) {
//        boolean isGood = RANDOM.nextInt(100) < 70;
//
//        if (!isGood) {
//            StatusEffectInstance[] badEffects = {
//                    new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 1),
//                    new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 15, 0),
//                    new StatusEffectInstance(StatusEffects.POISON, 20 * 8, 1),
//                    new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * 5, 0),
//                    new StatusEffectInstance(StatusEffects.HUNGER, 20 * 20, 1)
//            };
//            StatusEffectInstance effect = badEffects[RANDOM.nextInt(badEffects.length)];
//            player.addStatusEffect(effect);
//            player.sendMessage(Text.literal("§c負面效果: " + effect.getEffectType().value().getName().getString()), false);
//        } else {
//            StatusEffectInstance[] goodEffects = {
//                    new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 1),
//                    new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20 * 45, 1),
//                    new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 30, 0),
//                    new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 20, 1),
//                    new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 40, 0),
//                    new StatusEffectInstance(StatusEffects.NIGHT_VISION, 20 * 120, 0),
//                    new StatusEffectInstance(StatusEffects.HASTE, 20 * 60, 1)
//            };
//            StatusEffectInstance effect = goodEffects[RANDOM.nextInt(goodEffects.length)];
//            player.addStatusEffect(effect);
//            player.sendMessage(Text.literal("§a正面效果: " + effect.getEffectType().value().getName().getString()), false);
//        }
//    }
//
//    private static void modifyMaxHealth(ServerPlayerEntity player) {
//        boolean isIncrease = RANDOM.nextBoolean();
//        double currentMax = player.getMaxHealth();
//        double changeAmount = (RANDOM.nextInt(10) + 2);
//
//        if (isIncrease) {
//            double newMax = currentMax + changeAmount;
//            player.getAttributeInstance(EntityAttributes.MAX_HEALTH)
//                    .setBaseValue(newMax);
//            player.sendMessage(Text.literal("§a❤ 血量上限增加了 " + (changeAmount / 2) + " 顆心！"), false);
//        } else {
//            double newMax = Math.max(2, currentMax - changeAmount);
//            player.getAttributeInstance(EntityAttributes.MAX_HEALTH)
//                    .setBaseValue(newMax);
//            player.sendMessage(Text.literal("§c❤ 血量上限減少了 " + (changeAmount / 2) + " 顆心！"), false);
//        }
//
//        if (player.getHealth() > player.getMaxHealth()) {
//            player.setHealth(player.getMaxHealth());
//        }
//    }
//}