package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

public class BlindnessEvent extends Event {

    public BlindnessEvent() {
        super("Blindness");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        for (Player player : game.getAlivePlayers()) {
            player.addEffect(new Potion(PotionEffect.BLINDNESS, 0, 10 * 20));
        }
    }
}
