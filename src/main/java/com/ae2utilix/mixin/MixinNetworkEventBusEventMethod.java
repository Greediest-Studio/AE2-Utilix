package com.ae2utilix.mixin;

import appeng.api.networking.events.MENetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "appeng.me.NetworkEventBus$EventMethod", remap = false)
public abstract class MixinNetworkEventBusEventMethod {

    @Shadow(remap = false)
    private Method objMethod;

    @Inject(method = "invoke", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void ae2utilix$skipIncompatibleMachine(Object target, MENetworkEvent event, CallbackInfo ci) {
        if (this.objMethod != null && !this.objMethod.getDeclaringClass().isInstance(target)) {
            ci.cancel();
        }
    }
}
