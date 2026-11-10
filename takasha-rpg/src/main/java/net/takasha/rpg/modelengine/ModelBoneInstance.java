package net.takasha.rpg.modelengine;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import net.takasha.rpg.mixin.DisplayAccessor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModelBoneInstance {

    private final BlockbenchModel.BoneDef definition;
    private Display.ItemDisplay displayEntity;

    private final Vector3f localTranslation = new Vector3f(0, 0, 0);
    private final Quaternionf localRotation = new Quaternionf();
    private final Vector3f localScale = new Vector3f(1, 1, 1);
    private float boneScale = 1.0f;

    private final Matrix4f localMatrix = new Matrix4f();
    private final Matrix4f globalMatrix = new Matrix4f();

    public ModelBoneInstance(BlockbenchModel.BoneDef definition) {
        this.definition = definition;
        resetTransforms();
    }

    public BlockbenchModel.BoneDef getDefinition() {
        return definition;
    }

    public Display.ItemDisplay getDisplayEntity() {
        return displayEntity;
    }

    public void setDisplayEntity(Display.ItemDisplay displayEntity) {
        this.displayEntity = displayEntity;
    }

    public float getBoneScale() {
        return boneScale;
    }

    public void setBoneScale(float scale) {
        this.boneScale = scale > 0 ? scale : 1.0f;
    }

    public void resetTransforms() {
        localTranslation.set(0, 0, 0);
        localScale.set(1, 1, 1);

        // Apply base rotation from Blockbench (degrees -> radians)
        Vector3f baseRot = definition.getBaseRotation();
        localRotation.identity()
                .rotateZ((float) Math.toRadians(baseRot.z()))
                .rotateY((float) Math.toRadians(baseRot.y()))
                .rotateX((float) Math.toRadians(baseRot.x()));
    }

    public void setAnimatedTransform(Vector3f pos, Vector3f rotDeg, Vector3f scale) {
        localTranslation.set(pos.x() / 16f, pos.y() / 16f, pos.z() / 16f); // 16 pixels per block
        localScale.set(scale);

        Vector3f baseRot = definition.getBaseRotation();
        float rx = (float) Math.toRadians(baseRot.x() + rotDeg.x());
        float ry = (float) Math.toRadians(baseRot.y() + rotDeg.y());
        float rz = (float) Math.toRadians(baseRot.z() + rotDeg.z());

        localRotation.identity().rotateZ(rz).rotateY(ry).rotateX(rx);
    }

    public void updateGlobalMatrix(Matrix4f parentMatrix) {
        Vector3f pivot = definition.getOrigin();
        float relPx;
        float relPy;
        float relPz;

        if (definition.getParent() != null) {
            Vector3f parentOrigin = definition.getParent().getOrigin();
            relPx = (pivot.x() - parentOrigin.x()) / 16f;
            relPy = (pivot.y() - parentOrigin.y()) / 16f;
            relPz = (pivot.z() - parentOrigin.z()) / 16f;
        } else {
            relPx = pivot.x() / 16f;
            relPy = pivot.y() / 16f;
            relPz = pivot.z() / 16f;
        }

        localMatrix.identity();
        localMatrix.translate(relPx + localTranslation.x(), relPy + localTranslation.y(), relPz + localTranslation.z());
        localMatrix.rotate(localRotation);
        localMatrix.scale(localScale);

        if (parentMatrix != null) {
            globalMatrix.set(parentMatrix).mul(localMatrix);
        } else {
            globalMatrix.set(localMatrix);
        }

        if (displayEntity != null && displayEntity.isAlive()) {
            Matrix4f renderMatrix = new Matrix4f(globalMatrix);
            if (boneScale != 1.0f) {
                renderMatrix.scale(boneScale);
            }
            Transformation transformation = new Transformation(renderMatrix);
            DisplayAccessor accessor = (DisplayAccessor) displayEntity;
            accessor.callSetTransformation(transformation);
            accessor.callSetTransformationInterpolationDuration(1);
        }
    }

    public Matrix4f getGlobalMatrix() {
        return globalMatrix;
    }

    public void remove() {
        if (displayEntity != null) {
            Display.ItemDisplay entity = displayEntity;
            displayEntity = null;
            TakashaModelEngine.unregisterActiveBoneDisplay(entity);
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
    }
}
