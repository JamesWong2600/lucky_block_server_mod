package org.auto.lucky_block_server_mod.cache;

import java.util.List;

public class RandomEffectConfigData {
    public Effects effects;

    public static class Effects {
        public ItemEffect item;
        public PotionEffect potion;
        public HealthEffect health;
    }

    public static class ItemEffect {
        public int weight;
        public String message;
        public ItemCategories categories;
    }

    public static class ItemCategories {
        public List<NormalItem> normal_items;
        public WeaponToolArmorConfig weapons;
        public WeaponToolArmorConfig tools;
        public WeaponToolArmorConfig armors;
    }

    public static class NormalItem {
        public String id;
        public int count;
    }

    public static class WeaponToolArmorConfig {
        public List<String> pool;
        public double enchant_chance;
        public String enchantment;
        public int min_level;
        public int max_level;
        public List<String> exclude_enchant; // 僅工具類別常用到
    }

    public static class PotionEffect {
        public int weight;
        public String message;
        public double good_effect_chance;
        public List<PotionData> bad_effects;
        public List<PotionData> good_effects;
    }

    public static class PotionData {
        public String id;
        public int duration_ticks;
        public int amplifier;
    }

    public static class HealthEffect {
        public int weight;
        public String message;
        public int min_change;
        public int max_change;
        public int min_health_limit;
        public String increase_msg;
        public String decrease_msg;
    }
}