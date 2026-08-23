package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

public class CloseCallEvent extends Event {

    public CloseCallEvent() {
        super("Close Call");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        for (Player player : game.getAlivePlayers()) {
            player.setHealth(1);
            player.addEffect(new Potion(PotionEffect.REGENERATION, 2, 4 * 20));
        }
    }
}
