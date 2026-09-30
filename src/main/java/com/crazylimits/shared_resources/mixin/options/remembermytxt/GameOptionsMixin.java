package com.crazylimits.shared_resources.mixin.options.remembermytxt;

import com.crazylimits.shared_resources.SharedResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.nbt.CompoundTag;

@Mixin(Options.class)
public abstract class GameOptionsMixin {
    /*
     * Most of this code is copied from RememberMyTxt with permission from DuncanRuns.
     * See https://github.com/DuncanRuns/RememberMyTxt
     *
     * This is the 1.19 version of the mixin.
     */

    @Shadow
    protected abstract void processOptions(Options.FieldAccess visitor);

    @Unique
    private CompoundTag sharedresources$loadedData;
    @Unique
    private Map<String, String> sharedresources$unacceptedOptions = new HashMap<>();

    @Inject(method = "dataFix", at = @At("HEAD"))
    private void sharedresources$getKeys(CompoundTag nbtCompound, CallbackInfoReturnable<CompoundTag> cir) {
        sharedresources$loadedData = nbtCompound;
    }

    @Inject(
            method = "load",
            at = @At("TAIL")
    )
    private void sharedresources$endLoad(CallbackInfo info) {
        //? if >=1.21.5 {
        Set<String> unacceptedKeys = sharedresources$loadedData.keySet();
        //?} else
        //Set<String> unacceptedKeys = sharedresources$loadedData.getAllKeys();
        processOptions(new Options.FieldAccess() {
            @Override
            public <T> void process(String key, OptionInstance<T> option) {
                unacceptedKeys.remove(key);
            }

            @Override
            public int process(String key, int current) {
                unacceptedKeys.remove(key);
                return current;
            }

            @Override
            public boolean process(String key, boolean current) {
                unacceptedKeys.remove(key);
                return current;
            }

            @Override
            public String process(String key, String current) {
                unacceptedKeys.remove(key);
                return current;
            }

            @Override
            public float process(String key, float current) {
                unacceptedKeys.remove(key);
                return current;
            }

            @Override
            public <T> T process(String key, T current, Function<String, T> decoder, Function<T, String> encoder) {
                unacceptedKeys.remove(key);
                return current;
            }
        });
        unacceptedKeys.remove("version");
        sharedresources$unacceptedOptions = new HashMap<>();
        for (String key : unacceptedKeys.toArray(new String[0])) {
            SharedResources.LOGGER.info(
                    "Unaccepted options.txt Key: \"" + key + "\" with value: " + sharedresources$loadedData.get(key) +
                    ". Storing seperately to ensure it is not lost."
            );
            //? if >=1.21.5 {
            sharedresources$unacceptedOptions.put(key, sharedresources$loadedData.getStringOr(key, ""));
            //?} else
            //sharedresources$unacceptedOptions.put(key, sharedresources$loadedData.getString(key));
        }
    }

    @Inject(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;processOptions(Lnet/minecraft/client/Options$FieldAccess;)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void sharedresources$writeUnaccepted(CallbackInfo info, PrintWriter printWriter) {
        // Unaccepted variables will be placed at the top in case they weren't accepted by the visitor during reading.
        // This probably means that they will be written a second time later in the file, and for duplicate keys, the
        // lowest one in the file is the one which will be loaded.
        for (Map.Entry<String, String> entry : sharedresources$unacceptedOptions.entrySet()) {
            printWriter.println(entry.getKey() + ":" + entry.getValue());
        }
    }
}
