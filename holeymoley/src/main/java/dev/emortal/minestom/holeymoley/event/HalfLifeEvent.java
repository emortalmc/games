package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

public class HalfLifeEvent extends Event {

    public HalfLifeEvent() {
        super("Half Life");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        for (Player player : game.getAlivePlayers()) {
            player.setHealth(player.getHealth() / 2);
            player.addEffect(new Potion(PotionEffect.REGENERATION, 1, 4 * 20));
        }
    }
}
