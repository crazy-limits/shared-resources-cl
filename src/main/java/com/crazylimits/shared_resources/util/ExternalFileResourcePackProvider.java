package com.crazylimits.shared_resources.util;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

public class ExternalFileResourcePackProvider extends FolderRepositorySource {
    protected final Supplier<Path> pathSupplier;

    public ExternalFileResourcePackProvider(Supplier<Path> pathSupplier) {
        super(null, PackType.CLIENT_RESOURCES, PackSource.DEFAULT/*? if >=1.20.2 {*/, Minecraft.getInstance().directoryValidator()/*?}*/);
        this.pathSupplier = pathSupplier;
    }

    @Override
    public void loadPacks(Consumer<Pack> profileAdder) {
        FileResourcepackProviderProxy thiz = (FileResourcepackProviderProxy) this;

        Path path = pathSupplier.get();
        if (path == null) return;
        thiz.sharedresources$setPacksFolder(path);

        super.loadPacks(profileAdder);
    }
}
