package me.cutebow.totem_pop_predictor.client.logic;

import me.cutebow.totem_pop_predictor.client.config.TppConfig;
import me.cutebow.totem_pop_predictor.client.hud.TppHud;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@Environment(EnvType.CLIENT)
public class TppLogic {
    private static boolean requireRetotemTip = false;

    public static void tick(MinecraftClient c){
        if(!TppConfig.enabled){ TppHud.hide(); return; }
        if(c.world==null || c.player==null){ TppHud.hide(); return; }
        if(c.player.isCreative() || c.player.isSpectator()){ TppHud.hide(); return; }

        PlayerEntity p = c.player;
        boolean show;

        if(requireRetotemTip && hasTotemEquipped(p)) requireRetotemTip = false;

        if(TppConfig.mode == TppConfig.Mode.PREDICTION){
            show = predictionShouldShow(c, p);
        } else if (TppConfig.mode == TppConfig.Mode.TIP){
            show = requireRetotemTip;
        } else {
            show = predictionShouldShow(c, p) || requireRetotemTip;
        }

        if(show) TppHud.show(); else TppHud.hide();
    }

    public static void onEntityStatus(EntityStatusS2CPacket p, MinecraftClient c){
        if(!TppConfig.enabled) return;
        if(c.world == null || c.player == null) return;
        if (p.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;
        Entity e = p.getEntity(c.world);
        if (e == null || e.getId() != c.player.getId()) return;
        requireRetotemTip = true;
    }

    private static boolean predictionShouldShow(MinecraftClient c, PlayerEntity p){
        if(!hasTotemAnywhere(p)) return false;
        if(TppConfig.predMode == TppConfig.PredMode.SIMPLE){
            double effHp = p.getHealth() + p.getAbsorptionAmount();
            return effHp <= 6.0;
        } else {
            double eff = p.getHealth() + p.getAbsorptionAmount();
            boolean low = eff <= 6.0;
            boolean lethal = lethalAdvanced(c, p);
            return lethal && low;
        }
    }

    private static boolean lethalAdvanced(MinecraftClient c, PlayerEntity p){
        double health = p.getHealth();
        double absorption = p.getAbsorptionAmount();
        float saturation = p.getHungerManager().getSaturationLevel();
        double effHp = health + absorption + Math.min(6.0, saturation * 0.5);

        double nearCrystal = nearestCrystalDamageAdv(c, p, 5.0);
        double nearAnchor  = nearestAnchorDamageAdv(c, p, 5.0);
        double hypoAround  = hypotheticalCrystalAroundFeetAdv(c, p);
        double hypoHole    = estimateAdv(Vec3d.ofCenter(p.getBlockPos()).add(0, -0.5, 0), p);
        double hypoAbove   = estimateAdv(Vec3d.ofCenter(p.getBlockPos()).add(0, 1.0, 0), p);
        double maxThreat = Math.max(Math.max(nearCrystal, nearAnchor), Math.max(hypoAround, Math.max(hypoHole, hypoAbove)));

        if(isHoleish(c, p.getBlockPos())) maxThreat *= 1.15;
        if(p.isTouchingWater()) maxThreat *= 0.6;

        StatusEffectInstance res = p.getStatusEffect(StatusEffects.RESISTANCE);
        if(res != null) maxThreat *= Math.max(0.0, 1.0 - 0.2*(res.getAmplifier()+1));

        double armor = p.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
        double tough = p.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        double reduced = damageReduction(maxThreat, armor, tough);

        return reduced >= effHp - 0.5;
    }

    private static boolean hasTotemEquipped(PlayerEntity p){
        return p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) || p.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    private static boolean hasTotemReady(PlayerEntity p){
        if(p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return true;
        for(int i=0;i<9;i++){
            if(p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }

    private static boolean hasTotemAnywhere(PlayerEntity p){
        if(hasTotemReady(p)) return true;
        for(int i=9;i<p.getInventory().size();i++){
            ItemStack s = p.getInventory().getStack(i);
            if(s.isOf(Items.TOTEM_OF_UNDYING)) return true;
        }
        return false;
    }

    private static double nearestCrystalDamageAdv(MinecraftClient c, PlayerEntity p, double radius){
        double max = 0.0;
        for(Entity e : c.world.getEntities()){
            if(e instanceof EndCrystalEntity){
                if(e.distanceTo(p) <= radius){
                    double est = estimateAdv(e.getPos(), p);
                    if(est > max) max = est;
                }
            }
        }
        return max;
    }

    private static double nearestAnchorDamageAdv(MinecraftClient c, PlayerEntity p, double radius){
        double max = 0.0;
        BlockPos.Mutable mp = new BlockPos.Mutable();
        BlockPos base = p.getBlockPos();
        int r = (int)Math.ceil(radius);
        for(int dx=-r; dx<=r; dx++) for(int dy=-1; dy<=2; dy++) for(int dz=-r; dz<=r; dz++){
            mp.set(base.getX()+dx, base.getY()+dy, base.getZ()+dz);
            if(c.world.getBlockState(mp).isOf(net.minecraft.block.Blocks.RESPAWN_ANCHOR)){
                double est = estimateAdv(Vec3d.ofCenter(mp), p)*0.9;
                if(est > max) max = est;
            }
        }
        return max;
    }

    private static double hypotheticalCrystalAroundFeetAdv(MinecraftClient c, PlayerEntity p){
        double max = 0.0;
        BlockPos center = p.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for(int dx=-2; dx<=2; dx++){
            for(int dz=-2; dz<=2; dz++){
                m.set(center.getX()+dx, center.getY()-1, center.getZ()+dz);
                if(isCrystalBase(c, m)){
                    double est = estimateAdv(Vec3d.ofCenter(m).add(0, 1.0, 0), p);
                    if(est > max) max = est;
                }
                m.set(center.getX()+dx, center.getY(), center.getZ()+dz);
                if(isCrystalBase(c, m)){
                    double est = estimateAdv(Vec3d.ofCenter(m).add(0, 1.0, 0), p);
                    if(est > max) max = est;
                }
            }
        }
        return max;
    }

    private static boolean isCrystalBase(MinecraftClient c, BlockPos pos){
        return c.world.getBlockState(pos).isOf(net.minecraft.block.Blocks.OBSIDIAN)
                || c.world.getBlockState(pos).isOf(net.minecraft.block.Blocks.CRYING_OBSIDIAN)
                || c.world.getBlockState(pos).isOf(net.minecraft.block.Blocks.BEDROCK);
    }

    private static boolean clear(Vec3d from, LivingEntity to){
        HitResult r = to.getWorld().raycast(new RaycastContext(from, to.getEyePos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, to));
        return r.getType()==HitResult.Type.MISS;
    }

    private static double estimateAdv(Vec3d explosion, PlayerEntity target){
        Box box = target.getBoundingBox();
        Vec3d[] pts = new Vec3d[]{
                new Vec3d(box.getCenter().x, box.minY+0.1, box.getCenter().z),
                new Vec3d(box.getCenter().x, box.minY+0.9, box.getCenter().z),
                new Vec3d(box.minX+0.1, box.minY+0.9, box.getCenter().z),
                new Vec3d(box.maxX-0.1, box.minY+0.9, box.getCenter().z),
                new Vec3d(box.getCenter().x, box.minY+1.6, box.getCenter().z)
        };
        int visible = 0;
        for(Vec3d p2 : pts){
            HitResult r = target.getWorld().raycast(new RaycastContext(explosion, p2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, target));
            if(r.getType()==HitResult.Type.MISS) visible++;
        }
        double exposure = visible/(double)pts.length;
        double dx = explosion.x - target.getX();
        double dy = explosion.y - target.getEyeY();
        double dz = explosion.z - target.getZ();
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double radius = 6.0;
        double f = MathHelper.clamp(dist/(radius*2.0),0.0,1.0);
        double impact = (1.0 - f) * exposure;
        double raw = ((impact*impact + impact)/2.0) * 7.0 * (radius*2.0) + 1.0;
        if(target.isTouchingWater()) raw *= 0.6;
        double armor = target.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
        double tough = target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        double reduced = damageReduction(raw, armor, tough);
        StatusEffectInstance res = target.getStatusEffect(StatusEffects.RESISTANCE);
        if(res != null) reduced *= Math.max(0.0, 1.0 - 0.2*(res.getAmplifier()+1));
        return reduced;
    }

    private static double damageReduction(double dmg, double armor, double toughness){
        double f = 2.0 + toughness/4.0;
        double g = Math.max(armor - dmg/f, armor*0.2);
        return dmg * (1.0 - Math.max(0.0, Math.min(20.0, g))/25.0);
    }

    private static boolean isHoleish(MinecraftClient c, BlockPos pos){
        BlockPos.Mutable m = new BlockPos.Mutable();
        int solid = 0;
        m.set(pos.getX()+1,pos.getY(),pos.getZ()); if(c.world.getBlockState(m).isOpaqueFullCube(c.world, m)) solid++;
        m.set(pos.getX()-1,pos.getY(),pos.getZ()); if(c.world.getBlockState(m).isOpaqueFullCube(c.world, m)) solid++;
        m.set(pos.getX(),pos.getY(),pos.getZ()+1); if(c.world.getBlockState(m).isOpaqueFullCube(c.world, m)) solid++;
        m.set(pos.getX(),pos.getY(),pos.getZ()-1); if(c.world.getBlockState(m).isOpaqueFullCube(c.world, m)) solid++;
        return solid >= 3;
    }
}
