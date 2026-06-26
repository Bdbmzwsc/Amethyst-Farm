package com.zsj.amethystfarm.fakes;

import com.zsj.amethystfarm.farm.AmethystFarmBinding;
import com.zsj.amethystfarm.farm.AmethystFarmProfile;
import com.zsj.amethystfarm.farm.AmethystScanner;

public interface AmethystFarmBotAccess {
    AmethystFarmProfile amethystfarm$getProfile();

    void amethystfarm$setPreviewBinding(AmethystFarmBinding binding, AmethystScanner.ScanResult result);

    AmethystFarmBinding amethystfarm$getPreviewBinding();

    AmethystScanner.ScanResult amethystfarm$getPreviewResult();

    boolean amethystfarm$hasActivePreview();
}
