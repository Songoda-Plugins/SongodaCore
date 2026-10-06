package com.songoda.core.world;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Defines a set of biome atmosphere overrides used by {@link SkyColorManager}.
 *
 * <p>Any field left {@code null} means "use whatever the closest vanilla biome provides
 * for that channel".  Only the non-null fields participate in the biome-matching
 * distance calculation, so you can override just the sky, just the fog, or all three.
 *
 * <p>Use the fluent {@link Builder} to construct instances:
 * <pre>
 *     BiomeEffects effects = BiomeEffects.builder()
 *             .skyColor(0x8B0000)
 *             .fogColor(0x550000)
 *             .waterFogColor(0x3B0000)
 *             .build();
 * </pre>
 */
@Getter
@EqualsAndHashCode
public class BiomeEffects {

    /**
     * Blood-red sky with dark crimson fog.
     */
    public static final BiomeEffects BLOOD_MOON = builder()
            .skyColor(0x8B0000).fogColor(0x550000).waterFogColor(0x3B0000).build();
    /**
     * Poison-green sky with dark green fog.
     */
    public static final BiomeEffects POISON_STORM = builder()
            .skyColor(0x2D6A2D).fogColor(0x1A3D1A).build();
    /**
     * Deep purple celestial sky.
     */
    public static final BiomeEffects CELESTIAL = builder()
            .skyColor(0x4B0082).fogColor(0x2A0055).build();
    /**
     * Standard overworld sky (use this to restore after an event).
     */
    public static final BiomeEffects NORMAL = builder()
            .skyColor(0x78A7FF).fogColor(0xC0D8FF).waterFogColor(0x050533).build();
    /**
     * Packed 0xRRGGBB sky color, or {@code null} to leave unchanged.
     */
    private final Integer skyColor;

    // ------------------------------------------------------------------
    // Builder
    /**
     * Packed 0xRRGGBB fog (horizon) color, or {@code null} to leave unchanged.
     */
    private final Integer fogColor;

    // ------------------------------------------------------------------
    // Presets
    /**
     * Packed 0xRRGGBB underwater fog color, or {@code null} to leave unchanged.
     */
    private final Integer waterFogColor;

    /**
     * Protected constructor used by the builder.
     */
    protected BiomeEffects(Integer skyColor, Integer fogColor, Integer waterFogColor) {
        this.skyColor = skyColor;
        this.fogColor = fogColor;
        this.waterFogColor = waterFogColor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Integer skyColor;
        private Integer fogColor;
        private Integer waterFogColor;

        private Builder() {
        }

        public Builder skyColor(int packed) {
            this.skyColor = packed;
            return this;
        }

        public Builder skyColor(int r, int g, int b) {
            this.skyColor = (r << 16) | (g << 8) | b;
            return this;
        }

        public Builder fogColor(int packed) {
            this.fogColor = packed;
            return this;
        }

        public Builder fogColor(int r, int g, int b) {
            this.fogColor = (r << 16) | (g << 8) | b;
            return this;
        }

        public Builder waterFogColor(int packed) {
            this.waterFogColor = packed;
            return this;
        }

        public Builder waterFogColor(int r, int g, int b) {
            this.waterFogColor = (r << 16) | (g << 8) | b;
            return this;
        }

        public BiomeEffects build() {
            return new BiomeEffects(skyColor, fogColor, waterFogColor);
        }
    }
}
