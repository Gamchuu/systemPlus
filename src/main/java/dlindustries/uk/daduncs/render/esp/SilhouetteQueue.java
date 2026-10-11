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

package dlindustries.uk.daduncs.render.esp;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SilhouetteQueue extends OrderedRenderCommandQueueImpl {

	private final Int2ObjectAVLTreeMap<BatchingRenderCommandQueue> orders = new Int2ObjectAVLTreeMap<>();
	private int color;
	private boolean empty = true;

	public void setColor(int color) {
		this.color = color | 0xFF000000;
	}

	public boolean isEmpty() {
		return empty;
	}

	@Override
	public BatchingRenderCommandQueue getBatchingQueue(int order) {
		return orders.computeIfAbsent(order, value -> new Collection());
	}

	@Override
	public Int2ObjectAVLTreeMap<BatchingRenderCommandQueue> getBatchingQueues() {
		return orders;
	}

	@Override
	public void clear() {
		orders.values().forEach(BatchingRenderCommandQueue::clear);
		empty = true;
	}

	@Override
	public void onNextFrame() {
		orders.values().removeIf(collection -> !collection.hasCommands());
		orders.values().forEach(BatchingRenderCommandQueue::onNextFrame);
	}

	private final class Collection extends BatchingRenderCommandQueue {

		private Collection() {
			super(SilhouetteQueue.this);
		}

		@Override
		public <S> void submitModel(Model<? super S> model, S state, MatrixStack matrices, RenderLayer layer, int light, int overlay,
									int tint, @Nullable Sprite sprite, int outlineColor, ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumbling) {
			empty = false;
			super.submitModel(model, state, matrices, layer, light, overlay, tint, sprite, color, null);
		}

		@Override
		public void submitModelPart(ModelPart part, MatrixStack matrices, RenderLayer layer, int light, int overlay, @Nullable Sprite sprite,
									boolean sheeted, boolean glint, int tint, ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumbling, int outlineColor) {
			empty = false;
			super.submitModelPart(part, matrices, layer, light, overlay, sprite, sheeted, false, tint, null, color);
		}

		@Override
		public void submitItem(MatrixStack matrices, ItemDisplayContext context, int light, int overlay, int outlineColor, int[] tints,
							   List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint glint) {
			empty = false;
			super.submitItem(matrices, context, light, overlay, color, tints, quads, layer, ItemRenderState.Glint.NONE);
		}

		@Override
		public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
			empty = false;
			super.submitBlock(matrices, state, light, overlay, color);
		}

		@Override
		public void submitBlockStateModel(MatrixStack matrices, RenderLayer layer, BlockStateModel model, float red, float green, float blue,
										  int light, int overlay, int outlineColor) {
			empty = false;
			super.submitBlockStateModel(matrices, layer, model, red, green, blue, light, overlay, color);
		}

		@Override
		public void submitShadowPieces(MatrixStack matrices, float radius, List<EntityRenderState.ShadowPiece> pieces) {
		}

		@Override
		public void submitLabel(MatrixStack matrices, @Nullable Vec3d position, int offset, Text name, boolean seeThrough, int light,
								double distance, CameraRenderState camera) {
		}

		@Override
		public void submitText(MatrixStack matrices, float x, float y, OrderedText text, boolean shadow, TextRenderer.TextLayerType layerType,
							   int light, int color, int background, int outline) {
		}

		@Override
		public void submitFire(MatrixStack matrices, EntityRenderState state, Quaternionf rotation) {
		}

		@Override
		public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leash) {
		}

		@Override
		public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
		}

		@Override
		public void submitCustom(MatrixStack matrices, RenderLayer layer, OrderedRenderCommandQueue.Custom custom) {
		}

		@Override
		public void submitCustom(OrderedRenderCommandQueue.LayeredCustom custom) {
		}
	}
}
