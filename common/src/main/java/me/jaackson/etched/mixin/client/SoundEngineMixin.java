package me.jaackson.etched.mixin.client;

import com.mojang.blaze3d.audio.OggAudioStream;
import me.jaackson.etched.client.sound.AbstractOnlineSoundInstance;
import me.jaackson.etched.client.sound.SoundStopListener;
import me.jaackson.etched.client.sound.download.*;
import me.jaackson.etched.common.item.EtchedMusicDiscItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Unique
    private Sound sound;

    @Inject(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onSoundRemoved(CallbackInfo ci, Iterator<?> iterator, Map.Entry<?, ?> entry, ChannelAccess.ChannelHandle channelHandle2, SoundInstance soundInstance) {
        if (soundInstance instanceof SoundStopListener)
            ((SoundStopListener) soundInstance).onStop();
    }

    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    public void captureSound(SoundInstance soundInstance, CallbackInfo ci, WeighedSoundEvents weighedSoundEvents, ResourceLocation resourceLocation, Sound sound) {
        this.sound = sound;
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/ResourceLocation;Z)Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<AudioStream> redirectSoundStream(SoundBufferLibrary soundBufferLibrary, ResourceLocation resourceLocation, boolean loop) {
        if (!(this.sound instanceof AbstractOnlineSoundInstance.OnlineSound))
            return soundBufferLibrary.getStream(resourceLocation, loop);

        AbstractOnlineSoundInstance.OnlineSound onlineSound = (AbstractOnlineSoundInstance.OnlineSound) this.sound;
        if (EtchedMusicDiscItem.isLocalSound(onlineSound.getURL())) {
            WeighedSoundEvents weighedSoundEvents = Minecraft.getInstance().getSoundManager().getSoundEvent(new ResourceLocation(onlineSound.getURL()));
            if (weighedSoundEvents == null)
                throw new CompletionException(new FileNotFoundException("Unable to play unknown soundEvent: " + resourceLocation));

            return soundBufferLibrary.getStream(weighedSoundEvents.getSound().getPath(), loop).thenApplyAsync(MonoWrapper::new, Util.backgroundExecutor()).handleAsync((stream, e) -> {
                if (e != null) {
                    e.printStackTrace();
                    return EmptyAudioStream.INSTANCE;
                }
                onlineSound.getProgressListener().onSuccess();
                return stream;
            }, Util.backgroundExecutor());
        }

        return SoundCache.getAudioStream(onlineSound.getURL(), onlineSound.getProgressListener()).<AudioStream>thenApplyAsync(path -> {
            FileInputStream is = null;

            // Try loading as OGG
            try {
                is = new FileInputStream(path.toFile());
                return new MonoWrapper(loop ? new LoopingAudioStream(OggAudioStream::new, is) : new OggAudioStream(is));
            } catch (Exception e) {
                IOUtils.closeQuietly(is);
                LOGGER.debug("Failed to load as OGG", e);

                // Try loading as WAV
                try {
                    is = new FileInputStream(path.toFile());
                    AudioInputStream ais = WaveDataReader.getAudioInputStream(is);
                    AudioFormat format = ais.getFormat();
                    return new MonoWrapper(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), ais) : new RawAudioStream(format, ais));
                } catch (Exception e1) {
                    IOUtils.closeQuietly(is);
                    LOGGER.debug("Failed to load as WAV", e1);

                    // Try loading as MP3
                    try {
                        is = new FileInputStream(path.toFile());
                        fr.delthas.javamp3.Sound sound = new fr.delthas.javamp3.Sound(new ByteArrayInputStream(IOUtils.toByteArray(is)));
                        AudioFormat format = sound.getAudioFormat();
                        return new MonoWrapper(loop ? new LoopingAudioStream(input -> new RawAudioStream(format, input), sound) : new RawAudioStream(format, sound));
                    } catch (Exception e2) {
                        LOGGER.debug("Failed to load as MP3", e2);
                        throw new CompletionException(new UnsupportedAudioFileException("Could not load as OGG, WAV, OR MP3"));
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
            }
        }, Util.backgroundExecutor()).handleAsync((stream, e) -> {
            if (e != null) {
                e.printStackTrace();
                return EmptyAudioStream.INSTANCE;
            }
            onlineSound.getProgressListener().onSuccess();
            return stream;
        }, Util.backgroundExecutor());
    }
}
