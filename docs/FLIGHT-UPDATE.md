# Flight update

LegendarySwords 0.5.5 enables temporary flight for **Infernal Dragon boss ultimates**, alongside Angel and Sky Leviathan. `bosses.flight-enabled` must be true. Activate the actual weapon ultimate; `/bm disguise` alone only changes appearance. Boss expiry/cancellation restores previous flight settings.

LegendaryArmor 1.1.3 grants flight while wearing all four matching pieces of **Angel, Dragon or Leviathan** armor. Double-jump to fly. Removing the qualifying set restores previous flight settings; creative/spectator remain controlled by the server. Set `sets.angel.flight-enabled`, `sets.dragon.flight-enabled`, or `sets.leviathan.flight-enabled` to false to disable a set's flight. `/armor reload` applies config changes.

Install both updated plugins together for coordinated armor/boss flight restoration. Taking armor off during a boss form does not revoke boss flight; ending a boss while armor remains equipped retains armor flight. Existing armor items and resource packs remain compatible. Floating accessories stay disabled in stable mode.

Stop the server, remove old LegendarySwords and LegendaryArmor JARs, add the two new JARs, keep data folders, and restart.

Automated tests cover flight activation/expiry, armor removal, preexisting flight, game-mode handling and overlapping armor/boss flight. Live server testing remains necessary.
