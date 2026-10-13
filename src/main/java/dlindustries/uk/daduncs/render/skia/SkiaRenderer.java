/*
 * This file is part of System Client (https://github.com/daduncs/system)
 * Website: https://download.daduncs.cloud/
 *
 * Copyright (c) 2024 - 2026 DLindustries and project contributors.
 *
 * System Client is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * System Client is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with System Client. If not, see <https://www.gnu.org/licenses/>.
 */

package dlindustries.uk.daduncs.render.skia;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.GLBackendState;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Picture;
import io.github.humbleui.skija.PictureRecorder;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.types.Rect;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

/**
 * Skija render backend.
 *
 * <p>The client's custom UI (HUD, ClickGui, modules) draws through
 * {@link dlindustries.uk.daduncs.render.Draw} / {@link dlindustries.uk.daduncs.render.Render2D}
 * with <b>unchanged call sites</b>; when {@link #ENABLED} is true those methods
 * delegate here instead of the custom GLSL pipeline.
 *
 * <p>Unlike the earlier implementation, Skia never touches the GL context while
 * the game is rendering the frame. Every draw call made during the HUD/screen
 * pass is recorded into a CPU display list ({@link Picture}) through
 * {@link #canvas()}. Only right before the frame is presented - inside
 * {@link #flushToFramebuffer()} - is the picture replayed, directly onto
 * Minecraft's main-framebuffer colour texture, and flushed in a single
 * state-protected burst. This is the approach used by the Skija template
 * (Lyzev/Skija): draw straight into the live frame, with <b>no</b> offscreen
 * surface, snapshot or composite step.
 *
 * <p>The previous offscreen-surface + {@code makeImageSnapshot} + composite
 * design could let Ganesh submit deferred GL commands while vanilla was
 * mid-frame (its state is only protected around the submit), corrupting
 * Blaze3D's or its own bindings. That showed up as flickering text, glyphs
 * rendering from the wrong atlas (parts "rotating"), and characters drawn
 * incorrectly.
 *
 * <p>Native library: Skija extracts {@code io/github/humbleui/skija/<os>/<arch>/skija.dll}
 * from the classpath at load time, which is why the platform artifacts are merged
 * into the final jar alongside {@code skija-shared}.
 */
public final class SkiaRenderer {

	/** Flip this to route the client's 2D drawing through Skija. */
	public static boolean ENABLED = true;

	/**
	 * When false, text falls back to the legacy glyph-atlas renderer while 2D
	 * shapes stay on the Skia path. Wired to the "Skia Text" client setting.
	 */
	public static boolean TEXT_ENABLED = true;

	public static boolean text() {
		return ENABLED && TEXT_ENABLED;
	}

	private static DirectContext context;

	// CPU display list recorded during the frame and replayed at present time.
	private static PictureRecorder recorder;
	private static Canvas recordCanvas;
	private static int recW = -1;
	private static int recH = -1;
	private static boolean dirty;

	// Persistent Skia surface wrapping Minecraft's main-framebuffer colour
	// texture. Recreated only when the size or the underlying GL texture changes.
	private static Surface surface;
	private static BackendRenderTarget surfaceTarget;
	private static int surfaceFbo;
	private static int surfaceDepth;
	private static int surfaceW = -1;
	private static int surfaceH = -1;
	private static int surfaceTexture = -1;

	// Per-frame texture wraps (GL texture id -> Skia image snapshot).
	private static final Map<Integer, Image> textureImages = new HashMap<>();

	// Snapshots of the main framebuffer used by blur / glass / bubble effects.
	private static Image sceneImage;
	private static Image behindImage;
	private static int sceneSW = -1;
	private static int sceneSH = -1;
	private static int behindW = -1;
	private static int behindH = -1;
	private static boolean sceneCopied;
	private static boolean behindReady;

	/** A Skia image plus the pixel size it was wrapped from. */
	public record SkiaTexture(Image image, int width, int height) {
	}

	private SkiaRenderer() {
	}

	// ---------------------------------------------------------------- lifecycle

	public static DirectContext context() {
		if (context == null || context.isClosed()) {
			context = DirectContext.makeGL();
		}
		return context;
	}

	/**
	 * Returns the canvas that records this frame's Skia drawing. The returned
	 * canvas is a pure CPU display-list recorder - it performs no GL work - so
	 * call sites may draw at any point during the frame. Returns {@code null}
	 * when the window is not ready.
	 */
	public static void markDirty() {
		dirty = true;
	}

	public static boolean isDirty() {
		return dirty;
	}

	/**
	 * Returns the Skia {@link Canvas} for the current frame, starting a recording
	 * if needed.
	 */
	public static Canvas canvas() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null) {
			return null;
		}
		Framebuffer fb = mc.getFramebuffer();
		int w = fb != null && fb.textureWidth > 0 ? fb.textureWidth : (mc.getWindow() != null ? mc.getWindow().getFramebufferWidth() : 0);
		int h = fb != null && fb.textureHeight > 0 ? fb.textureHeight : (mc.getWindow() != null ? mc.getWindow().getFramebufferHeight() : 0);
		if (w <= 0 || h <= 0) {
			return null;
		}
		ensureRecorder(w, h);
		return recordCanvas;
	}

	/**
	 * Replays the frame's recorded Skia display list directly onto Minecraft's
	 * main framebuffer colour texture. Called right before
	 * {@code Framebuffer.blitToScreen()}, so the text is part of the exact frame
	 * that is presented. Mirrors the Skija template's frame hook.
	 */
	public static void flushToFramebuffer() {
		if (!ENABLED) {
			return;
		}

		if (!dirty) {
			Picture stale = finishRecording();
			tryClose(stale);
			releaseFrameState();
			return;
		}
		dirty = false;

		Picture picture = finishRecording();
		if (picture == null) {
			releaseFrameState();
			return;
		}

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null) {
			tryClose(picture);
			releaseFrameState();
			return;
		}
		Framebuffer fb = mc.getFramebuffer();
		if (fb == null || fb.getColorAttachment() == null) {
			tryClose(picture);
			releaseFrameState();
			return;
		}
		GpuTexture tex = fb.getColorAttachment();
		if (!(tex instanceof GlTexture gl)) {
			tryClose(picture);
			releaseFrameState();
			return;
		}
		int colorTexId = gl.getGlId();
		int w = fb.textureWidth;
		int h = fb.textureHeight;
		if (w <= 0 || h <= 0) {
			tryClose(picture);
			releaseFrameState();
			return;
		}

		try {
			bindTarget(colorTexId, w, h);
			GlStateManager._viewport(0, 0, w, h);

			GL33.glBindSampler(0, 0);

			GL11.glDisable(GL11.GL_SCISSOR_TEST);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_STENCIL_TEST);
			GL11.glDisable(GL11.GL_CULL_FACE);

			GL11.glStencilMask(0xFF);
			GL11.glClearStencil(0);
			GL11.glDepthMask(true);
			GL11.glClearDepth(1.0);
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

			context().resetGLAll();
			Surface target = surfaceFor(w, h, colorTexId);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			Canvas c = target.getCanvas();
			c.save();
			c.drawPicture(picture);
			c.restore();
			target.flushAndSubmit(false);
		} finally {
			GlState.restoreToMinecraft(fb);
		}

		tryClose(picture);
		releaseFrameState();
	}

	/**
	 * The present path runs entirely in {@link #flushToFramebuffer()} before
	 * {@code blitToScreen()}; nothing needs to happen at swap time. Kept so the
	 * {@code Window} hook stays harmless.
	 */
	public static void flushToScreen() {
		// intentionally empty
	}

	/** Frees all GL/Skia resources. Safe to call from the client stop hook. */
	public static void close() {
		Picture picture = finishRecording();
		tryClose(picture);
		tryClose(recordCanvas);
		recordCanvas = null;

		tryClose(sceneImage);
		sceneImage = null;
		tryClose(behindImage);
		behindImage = null;
		for (Image img : textureImages.values()) {
			tryClose(img);
		}
		textureImages.clear();
		for (SkiaTexture st : idTextureImages.values()) {
			tryClose(st.image());
		}
		idTextureImages.clear();

		if (surface != null) {
			tryClose(surface);
			surface = null;
		}
		if (surfaceTarget != null) {
			tryClose(surfaceTarget);
			surfaceTarget = null;
		}
		if (surfaceDepth != 0) {
			GL30.glDeleteRenderbuffers(surfaceDepth);
			surfaceDepth = 0;
		}
		if (surfaceFbo != 0) {
			GlStateManager._glDeleteFramebuffers(surfaceFbo);
			surfaceFbo = 0;
		}
		if (auxFbo != 0) {
			GlStateManager._glDeleteFramebuffers(auxFbo);
			auxFbo = 0;
		}
		surfaceW = -1;
		surfaceH = -1;
		surfaceTexture = -1;

		if (context != null && !context.isClosed()) {
			context.close();
		}
		context = null;
	}

	// ------------------------------------------------------------------ scenes

	private static boolean sceneRequested = false;
	private static boolean behindRequested = false;

	/** Mirrors {@code Draw.captureScene()} for the Skia path - marks scene capture as needed. */
	public static void captureScene() {
		sceneRequested = true;
	}

	/** Mirrors {@code Draw.askBehind()} for the Skia path - marks behind capture as needed. */
	public static void askBehind() {
		behindRequested = true;
	}

	public static boolean sceneCopied() {
		if (sceneRequested && sceneImage == null) {
			ensureSceneSnapshot();
		}
		return sceneCopied;
	}

	public static Image sceneImage() {
		if (sceneRequested && sceneImage == null) {
			ensureSceneSnapshot();
		}
		return sceneImage;
	}

	private static void ensureSceneSnapshot() {
		if (sceneImage != null) {
			return;
		}
		SkiaTexture shot = snapshotMain();
		sceneImage = shot == null ? null : shot.image();
		sceneSW = shot == null ? -1 : shot.width();
		sceneSH = shot == null ? -1 : shot.height();
		sceneCopied = sceneImage != null;
	}

	public static int sceneWidth() {
		if (sceneRequested && sceneImage == null) {
			ensureSceneSnapshot();
		}
		return sceneSW;
	}

	public static int sceneHeight() {
		if (sceneRequested && sceneImage == null) {
			ensureSceneSnapshot();
		}
		return sceneSH;
	}

	public static Image behindImage() {
		if (behindRequested && behindImage == null) {
			ensureBehindSnapshot();
		}
		return behindImage;
	}

	public static int behindWidth() {
		if (behindRequested && behindImage == null) {
			ensureBehindSnapshot();
		}
		return behindW;
	}

	public static int behindHeight() {
		if (behindRequested && behindImage == null) {
			ensureBehindSnapshot();
		}
		return behindH;
	}

	private static void ensureBehindSnapshot() {
		if (behindImage != null) {
			return;
		}
		SkiaTexture shot = snapshotMain();
		behindImage = shot == null ? null : shot.image();
		behindW = shot == null ? -1 : shot.width();
		behindH = shot == null ? -1 : shot.height();
		behindReady = behindImage != null;
	}

	/** Mirrors {@code Draw.takeBehind()}: returns true when a behind snapshot is available. */
	public static boolean takeBehind() {
		if (behindRequested && behindImage == null) {
			ensureBehindSnapshot();
		}
		boolean had = behindReady;
		behindReady = false;
		behindRequested = false;
		return had && behindImage != null;
	}

	// ---------------------------------------------------------------- textures

	private static final Map<Identifier, SkiaTexture> idTextureImages = new ConcurrentHashMap<>();

	private static int auxFbo = 0;

	/** Wraps a Minecraft GL texture into a Skia image (cached per frame, keyed by GL id). */
	public static SkiaTexture textureImage(int glId, int width, int height) {
		if (glId <= 0 || width <= 0 || height <= 0) {
			return null;
		}
		Image cached = textureImages.get(glId);
		if (cached != null && !cached.isClosed()) {
			return new SkiaTexture(cached, width, height);
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		GlState.push();
		try {
			if (auxFbo == 0) {
				auxFbo = GlStateManager.glGenFramebuffers();
			}
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, auxFbo);
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
					GL11.GL_TEXTURE_2D, glId, 0);
			if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE) {
				BackendRenderTarget rt = BackendRenderTarget.makeGL(width, height, 0, 0, auxFbo, FramebufferFormat.GR_GL_RGBA8);
				Surface surf = Surface.wrapBackendRenderTarget(context(), rt, SurfaceOrigin.BOTTOM_LEFT,
						SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB());
				if (surf != null) {
					Image img = surf.makeImageSnapshot();
					surf.close();
					rt.close();
					if (img != null) {
						textureImages.put(glId, img);
						return new SkiaTexture(img, width, height);
					}
				} else if (rt != null) {
					rt.close();
				}
			}
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
					GL11.GL_TEXTURE_2D, 0, 0);
		} catch (Exception ignored) {
		} finally {
			GlState.pop();
		}
		return null;
	}

	/** Resolves a registered texture identifier and wraps it into a Skia image. */
	public static SkiaTexture textureImage(Identifier id) {
		if (id == null) {
			return null;
		}
		SkiaTexture cached = idTextureImages.get(id);
		if (cached != null && !cached.image().isClosed()) {
			return cached;
		}

		byte[] bytes = null;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc != null && mc.getResourceManager() != null) {
			try {
				var res = mc.getResourceManager().getResource(id);
				if (res.isPresent()) {
					try (InputStream in = res.get().getInputStream()) {
						bytes = in.readAllBytes();
					}
				}
			} catch (Exception ignored) {
			}
		}

		if (bytes == null) {
			String path = "/assets/" + id.getNamespace() + "/" + id.getPath();
			try (InputStream in = SkiaRenderer.class.getResourceAsStream(path)) {
				if (in != null) {
					bytes = in.readAllBytes();
				}
			} catch (Exception ignored) {
			}
		}

		if (bytes == null) {
			String path = "assets/" + id.getNamespace() + "/" + id.getPath();
			try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
				if (in != null) {
					bytes = in.readAllBytes();
				}
			} catch (Exception ignored) {
			}
		}

		if (bytes != null) {
			try {
				Image img = Image.makeDeferredFromEncodedBytes(bytes);
				if (img != null) {
					SkiaTexture tex = new SkiaTexture(img, img.getWidth(), img.getHeight());
					idTextureImages.put(id, tex);
					return tex;
				}
			} catch (Exception ignored) {
			}
		}

		// Fallback for runtime / dynamic textures (e.g. downloaded player skins)
		if (mc != null && mc.getTextureManager() != null) {
			try {
				AbstractTexture tex = mc.getTextureManager().getTexture(id);
				if (tex instanceof NativeImageBackedTexture nibt && nibt.getImage() != null) {
					NativeImage ni = nibt.getImage();
					int w = ni.getWidth();
					int h = ni.getHeight();
					int[] argb = ni.copyPixelsArgb();
					byte[] rgba = new byte[w * h * 4];
					for (int i = 0; i < argb.length; i++) {
						int c = argb[i];
						rgba[i * 4] = (byte) ((c >> 16) & 0xFF);     // R
						rgba[i * 4 + 1] = (byte) ((c >> 8) & 0xFF);  // G
						rgba[i * 4 + 2] = (byte) (c & 0xFF);         // B
						rgba[i * 4 + 3] = (byte) ((c >> 24) & 0xFF); // A
					}
					Image img = Image.makeRasterFromBytes(new ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL), rgba, w * 4L);
					if (img != null) {
						SkiaTexture st = new SkiaTexture(img, w, h);
						idTextureImages.put(id, st);
						return st;
					}
				}
				if (tex != null) {
					GpuTextureView view = tex.getGlTextureView();
					if (view != null && view.texture() instanceof GlTexture gl) {
						GpuTexture gt = view.texture();
						return textureImage(gl.getGlId(), gt.getWidth(0), gt.getHeight(0));
					}
				}
			} catch (Exception ignored) {
			}
		}

		return null;
	}

	/** Snapshot of the main framebuffer - the "behind the UI" content for blur effects. */
	private static SkiaTexture snapshotMain() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null) {
			return null;
		}
		Framebuffer fb = mc.getFramebuffer();
		if (fb == null || fb.getColorAttachment() == null) {
			return null;
		}
		GpuTexture tex = fb.getColorAttachment();
		if (!(tex instanceof GlTexture gl)) {
			return null;
		}
		int glId = gl.getGlId();
		int w = fb.textureWidth;
		int h = fb.textureHeight;
		if (w <= 0 || h <= 0) {
			return null;
		}
		try {
			GlState.push();
			bindTarget(glId, w, h);
			if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
				return null;
			}
			context().resetGLAll();
			Surface surf = surfaceFor(w, h, glId);
			if (surf != null) {
				Image img = surf.makeImageSnapshot();
				if (img != null) {
					return new SkiaTexture(img, w, h);
				}
			}
		} catch (Exception ignored) {
		} finally {
			GlState.pop();
		}
		return null;
	}

	// ------------------------------------------------------------------ helpers

	/** Starts a new display-list recording for the given framebuffer size. */
	private static void ensureRecorder(int w, int h) {
		if (recorder != null && !recorder.isClosed() && recW == w && recH == h && recordCanvas != null) {
			return;
		}
		Picture stale = finishRecording();
		tryClose(stale);
		recorder = new PictureRecorder();
		recW = w;
		recH = h;
		recordCanvas = recorder.beginRecording(Rect.makeWH(w, h));
	}

	private static Picture finishRecording() {
		if (recorder == null) {
			return null;
		}
		Picture picture = null;
		try {
			if (!recorder.isClosed()) {
				picture = recorder.finishRecordingAsPicture();
			}
		} catch (Throwable ignored) {
			picture = null;
		}
		tryClose(recorder);
		recorder = null;
		recordCanvas = null;
		return picture;
	}

	/**
	 * Creates the wrapper FBO that attaches Minecraft's main-framebuffer colour
	 * texture, keeping a {@code GL_DEPTH24_STENCIL8} renderbuffer attached for
	 * its lifetime. Skia's Ganesh draws against whatever the render-target
	 * descriptor promises, so the actual attachments must match the stencil bits
	 * passed to {@code BackendRenderTarget.makeGL}; a colour-only FBO paired with
	 * a 0-stencil descriptor triggers driver-side {@code GL_INVALID_OPERATION}
	 * ("the required buffer is missing").
	 */
	private static void bindTarget(int colorTexId, int w, int h) {
		if (surfaceFbo == 0) {
			surfaceFbo = GlStateManager.glGenFramebuffers();
		}
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, surfaceFbo);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
				GL11.GL_TEXTURE_2D, colorTexId, 0);

		if (surfaceDepth == 0 || surfaceW != w || surfaceH != h) {
			if (surfaceDepth != 0) {
				GL30.glDeleteRenderbuffers(surfaceDepth);
			}
			surfaceDepth = GL30.glGenRenderbuffers();
			GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, surfaceDepth);
			GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, w, h);
			GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
			GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT,
					GL30.GL_RENDERBUFFER, surfaceDepth);
			surfaceW = w;
			surfaceH = h;

			int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
			if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
				System.err.println("[System] Skia target FBO incomplete: 0x" + Integer.toHexString(status));
			}
		}
	}

	/** Wraps (and caches) a Skia surface over the main-framebuffer colour texture. */
	private static Surface surfaceFor(int w, int h, int colorTexId) {
		if (surface != null && !surface.isClosed()
				&& surface.getWidth() == w && surface.getHeight() == h
				&& surfaceTexture == colorTexId) {
			return surface;
		}

		if (surface != null) {
			tryClose(surface);
			surface = null;
		}
		if (surfaceTarget != null) {
			tryClose(surfaceTarget);
			surfaceTarget = null;
		}

		surfaceTarget = BackendRenderTarget.makeGL(w, h, 0, 8, surfaceFbo, FramebufferFormat.GR_GL_RGBA8);
		surface = Surface.wrapBackendRenderTarget(context(), surfaceTarget, SurfaceOrigin.BOTTOM_LEFT,
				SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB(), null);
		surfaceTexture = colorTexId;
		return surface;
	}

	private static void releaseFrameState() {
		for (Image img : textureImages.values()) {
			tryClose(img);
		}
		textureImages.clear();
		if (sceneImage != null) {
			tryClose(sceneImage);
			sceneImage = null;
		}
		if (behindImage != null) {
			tryClose(behindImage);
			behindImage = null;
		}
		sceneRequested = false;
		behindRequested = false;
		sceneSW = -1;
		sceneSH = -1;
		behindW = -1;
		behindH = -1;
		sceneCopied = false;
		behindReady = false;
	}

	private static void tryClose(AutoCloseable handle) {
		try {
			if (handle != null) {
				handle.close();
			}
		} catch (Exception ignored) {
		}
	}
}
