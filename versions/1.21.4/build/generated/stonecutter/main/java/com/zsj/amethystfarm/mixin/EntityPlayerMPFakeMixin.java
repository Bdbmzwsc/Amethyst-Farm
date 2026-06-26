package com.zsj.amethystfarm.mixin;

import carpet.patches.EntityPlayerMPFake;
import com.zsj.amethystfarm.config.AmethystFarmSettings;
import com.zsj.amethystfarm.data.AmethystFarmDataManager;
import com.zsj.amethystfarm.fakes.AmethystFarmBotAccess;
import com.zsj.amethystfarm.farm.AmethystFarmBinding;
import com.zsj.amethystfarm.farm.AmethystFarmProfile;
import com.zsj.amethystfarm.farm.AmethystHarvester;
import com.zsj.amethystfarm.farm.AmethystScanner;
import com.zsj.amethystfarm.farm.MiningClaimRegistry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityPlayerMPFake.class, remap = false)
public abstract class EntityPlayerMPFakeMixin implements AmethystFarmBotAccess {
    @Unique
    private AmethystFarmProfile amethystfarm$profile;

    @Unique
    private AmethystFarmBinding amethystfarm$previewBinding;

    @Unique
    private AmethystScanner.ScanResult amethystfarm$previewResult;

    @Unique
    private int amethystfarm$previewExpiry;

    @Override
    public AmethystFarmProfile amethystfarm$getProfile() {
        if (amethystfarm$profile == null) {
            amethystfarm$profile = AmethystFarmDataManager.getOrCreate((EntityPlayerMPFake) (Object) this);
        }
        return amethystfarm$profile;
    }

    @Override
    public void amethystfarm$setPreviewBinding(AmethystFarmBinding binding, AmethystScanner.ScanResult result) {
        amethystfarm$previewBinding = binding;
        amethystfarm$previewResult = result;
        amethystfarm$previewExpiry = 40;
    }

    @Override
    public AmethystFarmBinding amethystfarm$getPreviewBinding() {
        return amethystfarm$previewBinding;
    }

    @Override
    public AmethystScanner.ScanResult amethystfarm$getPreviewResult() {
        return amethystfarm$previewResult;
    }

    @Override
    public boolean amethystfarm$hasActivePreview() {
        return amethystfarm$previewExpiry > 0;
    }

    @Inject(
        method = "tick",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;tick()V",
            shift = At.Shift.AFTER
        )
    )
    private void amethystfarm$onTick(CallbackInfo ci) {
        if (!AmethystFarmSettings.enabled || !AmethystFarmDataManager.isFarmActive((EntityPlayerMPFake) (Object) this)) {
            return;
        }
        EntityPlayerMPFake self = (EntityPlayerMPFake) (Object) this;
        AmethystHarvester.tickFakePlayer(self, amethystfarm$getProfile());
        if (amethystfarm$previewExpiry > 0) {
            amethystfarm$previewExpiry--;
        }
    }

    @Inject(method = "kill(Lnet/minecraft/network/chat/Component;)V", remap = true, at = @At("HEAD"))
    private void amethystfarm$onKill(Component reason, CallbackInfo ci) {
        MiningClaimRegistry.releaseAllForBot(((EntityPlayerMPFake) (Object) this).getUUID());
    }
}
