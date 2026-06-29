//? if =1.21.6 {
package com.zsj.amethystfarm.mixin;

import com.zsj.amethystfarm.AmethystFarmMod;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Map;

@Mixin(value = RegistrySyncManager.class, remap = false)
public abstract class RegistrySyncManagerMixin {

    private static final String VANILLA_NS = ResourceLocation.DEFAULT_NAMESPACE; // "minecraft"

    @Inject(method = "createAndPopulateRegistryMap", at = @At("RETURN"), cancellable = true)
    private static void amethystfarm$stripServerOnlyEntries(
        CallbackInfoReturnable<Map<ResourceLocation, Object2IntMap<ResourceLocation>>> cir
    ) {
        Map<ResourceLocation, Object2IntMap<ResourceLocation>> map = cir.getReturnValue();

        if (map == null || map.isEmpty()) {
            return;
        }

        boolean changed = false;

        // Step 1: strip amethystfarm entries from every registry
        for (Object2IntMap<ResourceLocation> idMap : map.values()) {
            Iterator<ResourceLocation> iterator = idMap.keySet().iterator();
            while (iterator.hasNext()) {
                if (AmethystFarmMod.MOD_ID.equals(iterator.next().getNamespace())) {
                    iterator.remove();
                    changed = true;
                }
            }
        }

        if (!changed) {
            return;
        }

        // Step 2: remove registries that are now empty or contain only vanilla entries.
        // A registry that only has vanilla ("minecraft") entries after our strip was
        // solely marked MODDED because of amethystfarm — syncing it to a client without
        // this mod (especially ViaFabric) would be unnecessary and can cause a disconnect
        // when the cross-version client cannot handle the 1.21.6 sync format.
        map.entrySet().removeIf(entry -> {
            Object2IntMap<ResourceLocation> idMap = entry.getValue();
            if (idMap.isEmpty()) {
                return true;
            }
            boolean allVanilla = idMap.keySet().stream()
                .allMatch(id -> VANILLA_NS.equals(id.getNamespace()));
            return allVanilla;
        });

        if (map.isEmpty()) {
            cir.setReturnValue(null);
        }
    }
}
//?}