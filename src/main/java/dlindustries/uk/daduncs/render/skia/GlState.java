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
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Resets OpenGL state after Skia operations without any synchronous glGet*
 * driver queries, preventing CPU-GPU pipeline stalls.
 */
public final class GlState {

	private GlState() {
	}

	// Blaze3D's texture binding cache holds exactly 12 units - going beyond
	// throws ArrayIndexOutOfBounds inside GlStateManager._bindTexture.
	private static final int TEXTURE_UNITS = 12;

	private static int savedFbo;
	private static final IntBuffer savedViewport = intBuf();
	private static boolean savedScissorTest;
	private static final IntBuffer savedScissorBox = intBuf();
	private static boolean savedBlend;
	private static int savedBlendSrcRgb;
	private static int savedBlendDstRgb;
	private static int savedBlendSrcAlpha;
	private static int savedBlendDstAlpha;
	private static int savedBlendEqRgb;
	private static int savedBlendEqAlpha;
	private static boolean savedDepthTest;
	private static int savedDepthFunc;
	private static boolean savedDepthMask;
	private static boolean savedCull;
	private static boolean savedStencilTest;
	private static int savedStencilMask;
	private static final ByteBuffer savedColorMask = ByteBuffer.allocateDirect(4);
	private static int savedActiveUnit;
	private static final int[] savedTextures = new int[TEXTURE_UNITS];
	private static final int[] savedSamplers = new int[TEXTURE_UNITS];
	private static int savedProgram;
	private static int savedVao;
	private static int savedArrayBuffer;
	private static int savedUnpackBuffer;
	private static int savedUnpackAlignment;
	private static int savedUnpackRowLength;
	private static int savedUnpackSkipPixels;
	private static int savedUnpackSkipRows;

	private static IntBuffer intBuf() {
		IntBuffer buf = ByteBuffer.allocateDirect(16).asIntBuffer();
		return buf;
	}

	/**
	 * Saves the full GL state that Skia snapshotting touches, so mid-frame
	 * scene captures cannot leak foreign framebuffer bindings, viewport,
	 * blend, scissor or texture-unit state into Minecraft's rendering.
	 * Not re-entrant - snapshot paths never nest.
	 */
	public static void push() {
		savedFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
		savedScissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
		GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, savedScissorBox);
		savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
		savedBlendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		savedBlendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		savedBlendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		savedBlendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		savedBlendEqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
		savedBlendEqAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
		savedDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
		savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		savedStencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
		savedStencilMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
		GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, savedColorMask);
		savedActiveUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) - GL13.GL_TEXTURE0;
		for (int i = 0; i < TEXTURE_UNITS; i++) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
			savedTextures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			savedSamplers[i] = GL33.glGetInteger(GL33.GL_SAMPLER_BINDING);
		}
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + savedActiveUnit);
		savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		savedArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		savedUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
		savedUnpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
		savedUnpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
		savedUnpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
		savedUnpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
	}

	/**
	 * Restores the state saved by {@link #push()}. Uses Blaze3D setters where
	 * they exist so its state cache stays consistent, raw GL otherwise.
	 */
	public static void pop() {
		for (int i = 0; i < TEXTURE_UNITS; i++) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedTextures[i]);
			GlStateManager._bindTexture(savedTextures[i]);
			GL33.glBindSampler(i, savedSamplers[i]);
		}
		GlStateManager._activeTexture(GL13.GL_TEXTURE0 + savedActiveUnit);
		GlStateManager._glUseProgram(savedProgram);
		GlStateManager._glBindVertexArray(savedVao);
		GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, savedArrayBuffer);
		GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, savedUnpackBuffer);
		GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, savedUnpackAlignment);
		GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, savedUnpackRowLength);
		GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, savedUnpackSkipPixels);
		GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, savedUnpackSkipRows);
		if (savedViewport.get(2) > 0 && savedViewport.get(3) > 0) {
			GlStateManager._viewport(savedViewport.get(0), savedViewport.get(1), savedViewport.get(2), savedViewport.get(3));
		}
		if (savedScissorTest && savedScissorBox.get(2) > 0 && savedScissorBox.get(3) > 0) {
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox(savedScissorBox.get(0), savedScissorBox.get(1), savedScissorBox.get(2), savedScissorBox.get(3));
		} else {
			GlStateManager._disableScissorTest();
		}
		if (savedBlend)
			GlStateManager._enableBlend();
		else GlStateManager._disableBlend();
		GlStateManager._blendFuncSeparate(savedBlendSrcRgb, savedBlendDstRgb, savedBlendSrcAlpha, savedBlendDstAlpha);
		GL20.glBlendEquationSeparate(savedBlendEqRgb, savedBlendEqAlpha);
		if (savedDepthTest)
			GlStateManager._enableDepthTest();
		else GlStateManager._disableDepthTest();
		GlStateManager._depthFunc(savedDepthFunc);
		GlStateManager._depthMask(savedDepthMask);
		if (savedCull)
			GlStateManager._enableCull();
		else GlStateManager._disableCull();
		if (savedStencilTest)
			GL11.glEnable(GL11.GL_STENCIL_TEST);
		else GL11.glDisable(GL11.GL_STENCIL_TEST);
		GL11.glStencilMask(savedStencilMask);
		GlStateManager._colorMask(savedColorMask.get(0) != 0, savedColorMask.get(1) != 0,
				savedColorMask.get(2) != 0, savedColorMask.get(3) != 0);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo);
	}

	/**
	 * Restores the known OpenGL state expected by Minecraft/Blaze3D right before
	 * blitToScreen or HUD rendering with zero glGet* queries.
	 */
	public static void restoreToMinecraft(Framebuffer fb) {
		GL20.glUseProgram(0);
		GlStateManager._glUseProgram(0);
		GL30.glBindVertexArray(0);
		GlStateManager._glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
		GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);

		for (int i = 0; i < TEXTURE_UNITS; i++) {
			GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GlStateManager._bindTexture(0);
			GL33.glBindSampler(i, 0);
		}
		GlStateManager._activeTexture(GL13.GL_TEXTURE0);

		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GlStateManager._disableScissorTest();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GlStateManager._disableDepthTest();
		GL11.glDisable(GL11.GL_STENCIL_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GlStateManager._disableCull();

		GL11.glEnable(GL11.GL_BLEND);
		GlStateManager._enableBlend();
		GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL20.glBlendEquation(GL14.GL_FUNC_ADD);

		GL11.glDepthMask(true);
		GlStateManager._depthMask(true);
		GL11.glStencilMask(0xFF);

		GL11.glColorMask(true, true, true, true);
		GlStateManager._colorMask(true, true, true, true);

		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
		GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
		GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
		GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
		GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
		GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);

		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		if (fb != null && fb.textureWidth > 0 && fb.textureHeight > 0) {
			GlStateManager._viewport(0, 0, fb.textureWidth, fb.textureHeight);
		}
	}
}
